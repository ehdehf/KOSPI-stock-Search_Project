package com.boot.service;

import com.boot.dao.UserDAO;
import com.boot.dto.LoginRequestDTO;
import com.boot.dto.LoginResponseDTO;
import com.boot.dto.LoginUserInfoDTO;
import com.boot.dto.PasswordResetConfirmDTO;
import com.boot.dto.RegisterRequestDTO;
import com.boot.dto.SocialUserInfoDTO;
import com.boot.dto.UserInfoDTO;
import com.boot.security.JwtProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MailService mailService;

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    
    // 최대 실패 횟수 및 잠금 시간(초)
    private final int MAX_FAIL = 5;
    private final int LOCK_TIME = 30;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ResponseEntity<?> login(LoginRequestDTO req) {

    	UserInfoDTO user = userDAO.findByEmail(req.getEmail());

        // 1) 이메일 존재 확인
        if (user == null) {
            return ResponseEntity.status(401).body("❌ 존재하지 않는 이메일입니다.");
        }

        // 2) 이메일 인증 + 계정 상태 체크 (예시)
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return ResponseEntity.status(403)
                    .body("❌ 이메일 인증이 필요하거나 정지된 계정입니다.");
        }

        // 3) 계정 잠금 여부 체크 (이미 있던 코드 그대로)
        if (user.getLockUntil() != null) {
            LocalDateTime lockUntil = LocalDateTime.parse(user.getLockUntil(), DT_FORMAT);
            if (lockUntil.isAfter(LocalDateTime.now())) {
                long remainSec = Duration.between(LocalDateTime.now(), lockUntil).getSeconds();
                return ResponseEntity.status(403)
                        .body("🚫 계정이 잠겨있습니다. " + remainSec + "초 후 다시 시도 가능합니다.");
            }
        }

        // 4) 비밀번호 검증
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // 실패 횟수 증가 + 잠금 로직 (기존 코드 그대로)
            Integer failCount = user.getLoginFailCount();
            int newFailCount = (failCount == null ? 0 : failCount) + 1;
            userDAO.updateFailCount(user.getEmail(), newFailCount);

            if (newFailCount >= MAX_FAIL) {
                LocalDateTime lockTime = LocalDateTime.now().plusSeconds(LOCK_TIME);
                userDAO.lockUser(user.getEmail(), lockTime.format(DT_FORMAT));
                return ResponseEntity.status(403)
                        .body("❌ 비밀번호 5회 이상 오류. 계정이 30초 동안 잠겼습니다.");
            }

            int remain = MAX_FAIL - newFailCount;
            return ResponseEntity.status(401)
                    .body("❌ 비밀번호 오류. 남은 시도: " + remain + "회");
        }

        // 5) 로그인 성공 → 실패횟수 초기화
        userDAO.resetFailCount(user.getEmail());

        // 6) Access + Refresh 발급
        String accessToken = jwtProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

        
        userDAO.updateRefreshToken(user.getEmail(), refreshToken);

        // 7) 프론트에 내려줄 사용자 정보 구성 (민감정보 제외)
        LoginUserInfoDTO userInfo = new LoginUserInfoDTO(
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getProvider(),
                user.getCreatedAt(),
                user.getAccountStatus()
        );

        LoginResponseDTO response = new LoginResponseDTO(accessToken, refreshToken, userInfo);

        return ResponseEntity.ok(response);
    }
    
    //이메일 중복 확인
    public ResponseEntity<?> checkEmail(String email) {

        UserInfoDTO exist = userDAO.findByEmail(email);

        if (exist != null) {
            return ResponseEntity.ok(false); // 사용 불가
        }

        return ResponseEntity.ok(true); // 사용 가능
    }
    
    //회원가입
    public ResponseEntity<?> register(RegisterRequestDTO req) {

        // 1) 중복 체크
        if (userDAO.findByEmail(req.getEmail()) != null) {
            return ResponseEntity.status(400).body("이미 존재하는 이메일입니다.");
        }

        // 2) 비밀번호 암호화
        String encodedPw = passwordEncoder.encode(req.getPassword());

        // 3) fullName 생성
        String fullName = req.getLastName() + req.getFirstName();

        // 4) 이메일 인증 토큰 생성 (UUID 사용)
        String token = UUID.randomUUID().toString();
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(30);
        
        String expireAtStr = expireAt.format(DT_FORMAT);
        // 5) DB 저장
        userDAO.insertUser(
                req.getEmail(),
                req.getFirstName(),
                req.getLastName(),
                fullName,
                encodedPw,
                "LOCAL",
                "USER",
                token,
                expireAtStr
        );
        mailService.sendVerificationMail(req.getEmail(), token);
        // 6) 응답
        return ResponseEntity.ok("회원가입 완료! 이메일 인증을 진행해주세요.");
    }
    public ResponseEntity<?> verifyEmail(String token) {

        // 1) 토큰으로 유저 찾기
        UserInfoDTO user = userDAO.findByToken(token);

        if (user == null) {
            return ResponseEntity.status(400)
                    .body("❌ 유효하지 않은 인증 링크입니다.");
        }

        // 2) 계정이 이미 활성화 상태면
        if ("ACTIVE".equals(user.getAccountStatus())) {
            return ResponseEntity.status(400)
                    .body("이미 인증이 완료된 계정입니다.");
        }

        // 3) 토큰 만료 여부 체크
        LocalDateTime expireAt = LocalDateTime.parse(user.getTokenExpireAt(), DT_FORMAT);

        if (expireAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400)
                    .body("❌ 인증 시간이 만료되었습니다. 다시 요청해주세요.");
        }

        // 4) 인증 성공 → 계정 활성화
        userDAO.activateUser(user.getEmail());

        return ResponseEntity.ok("🎉 이메일 인증이 완료되었습니다! 로그인할 수 있습니다.");
    }
    
 // 5-1) 비밀번호 재설정 요청
    public ResponseEntity<?> requestPasswordReset(String email) {

        UserInfoDTO user = userDAO.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("해당 이메일의 계정을 찾을 수 없습니다.");
        }

        // 토큰 발급 + 만료시간 30분
        String token = UUID.randomUUID().toString();
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(30);

        userDAO.updateResetToken(
                email,
                token,
                expireAt.format(DT_FORMAT)
        );

        // 실제 서비스에서는 이메일 발송
        // 개발 중에는 token을 응답으로 내려서 Postman 테스트 가능하도록 함
        return ResponseEntity.ok("비밀번호 재설정 토큰이 발급되었습니다. (dev token: " + token + ")");
    }



    // 5-2) 토큰 유효성 검증
    public ResponseEntity<?> verifyResetToken(String token) {

        UserInfoDTO user = userDAO.findByToken(token);
        if (user == null) {
            return ResponseEntity.status(400).body("유효하지 않은 토큰입니다.");
        }

        LocalDateTime expireAt = LocalDateTime.parse(user.getTokenExpireAt(), DT_FORMAT);
        if (expireAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("토큰이 만료되었습니다.");
        }

        return ResponseEntity.ok("토큰이 유효합니다. 비밀번호를 재설정하세요.");
    }



    // 5-3) 새 비밀번호 저장
    public ResponseEntity<?> resetPassword(PasswordResetConfirmDTO req) {

        UserInfoDTO user = userDAO.findByToken(req.getToken());
        if (user == null) {
            return ResponseEntity.status(400).body("유효하지 않은 토큰입니다.");
        }

        LocalDateTime expireAt = LocalDateTime.parse(user.getTokenExpireAt(), DT_FORMAT);
        if (expireAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("토큰이 만료되었습니다.");
        }

        // 새 비밀번호 암호화 후 저장
        String encodedPw = passwordEncoder.encode(req.getNewPassword());

        userDAO.updatePasswordAndClearToken(user.getEmail(), encodedPw);

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }
    
    public ResponseEntity<?> refresh(String refreshToken) {

        // 1) refreshToken null 체크
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(400).body("Refresh Token이 없습니다.");
        }

        // 2) DB에서 해당 refreshToken 가진 유저 정보 조회
        UserInfoDTO user = userDAO.findByRefreshToken(refreshToken);
        if (user == null) {
            return ResponseEntity.status(401).body("유효하지 않은 Refresh Token입니다.");
        }

        // 3) 계정 상태 확인
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return ResponseEntity.status(403).body("계정 상태가 비정상적입니다.");
        }

        // 4) Refresh Token 자체 유효성(JWT 검증)
        if (!jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body("Refresh Token이 만료되었습니다. 다시 로그인하세요.");
        }

        // 5) 새 Access Token 생성
        String newAccessToken = jwtProvider.createAccessToken(user.getEmail());

        // 6) userInfo 생성 (LoginUserInfoDTO 형태)
        LoginUserInfoDTO userInfo = new LoginUserInfoDTO(
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getProvider(),
                user.getCreatedAt(),
                user.getAccountStatus()
        );

        // 7) LoginResponseDTO 생성 (access + refresh + user)
        LoginResponseDTO response = new LoginResponseDTO(
                newAccessToken,      // 새로운 accessToken
                refreshToken,        // refreshToken 그대로 반환
                userInfo             // 사용자 정보
        );

        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> logout(String email) {

        UserInfoDTO user = userDAO.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body("해당 이메일의 계정을 찾을 수 없습니다.");
        }

        // DB에서 refresh token 삭제
        userDAO.deleteRefreshToken(email);

        return ResponseEntity.ok("로그아웃되었습니다.");
    }
    
    //소셜로그인
    public ResponseEntity<?> socialLogin(SocialUserInfoDTO social) {

        // 1) 기존 회원 조회
        UserInfoDTO user = userDAO.findByEmail(social.getEmail());

        if (user == null) {
            // 2) 신규 소셜 회원 등록
            String fullName = social.getFullName();
            userDAO.insertSocialUser(
                    social.getEmail(),
                    fullName,
                    social.getProvider()
            );

            // 다시 조회
            user = userDAO.findByEmail(social.getEmail());
        }

        // 3) 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

        // 4) Refresh Token DB 저장
        userDAO.updateRefreshToken(user.getEmail(), refreshToken);

        // 5) 프론트로 내려줄 사용자 정보 구성
        LoginUserInfoDTO userInfo = new LoginUserInfoDTO(
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getProvider(),
                user.getCreatedAt(),
                user.getAccountStatus()
        );

        LoginResponseDTO response = new LoginResponseDTO(accessToken, refreshToken, userInfo);

        return ResponseEntity.ok(response);
    }
}
