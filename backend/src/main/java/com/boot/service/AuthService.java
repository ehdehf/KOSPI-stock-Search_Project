package com.boot.service;

import com.boot.dao.LoginLogDAO;
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

import javax.servlet.http.HttpServletRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MailService mailService;

    private final UserDAO userDAO;
    private final LoginLogDAO loginLogDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    	
    // 최대 실패 횟수 및 잠금 시간(초)
    private final int MAX_FAIL = 5;
    private final int LOCK_TIME = 30;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ResponseEntity<?> login(LoginRequestDTO req, HttpServletRequest request) {

    	String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        UserInfoDTO user = userDAO.findByEmail(req.getEmail());

        // 1) 이메일 존재 확인
        if (user == null) {
        	loginLogDAO.insertLog(req.getEmail(), "FAIL", ip, ua);
            return ResponseEntity.status(401).body("❌ 존재하지 않는 이메일입니다.");
        }

        // 2) 이메일 인증 여부 체크
        if (!"ACTIVE".equals(user.getAccountStatus())) {
        	loginLogDAO.insertLog(user.getEmail(), "FAIL", ip, ua);
            return ResponseEntity.status(403)
                    .body("❌ 이메일 인증이 완료되지 않은 계정입니다.");
        }

        // 3) 관리자 정지 여부 체크
        if ("Y".equals(user.getIsSuspended())) {

            // 정지 해제 시간이 있으면 확인
            if (user.getSuspendUntil() != null) {

                LocalDateTime until = LocalDateTime.parse(user.getSuspendUntil(), DT_FORMAT);

                // 정지 기간이 지났다면 → 자동 해제
                if (until.isBefore(LocalDateTime.now())) {
                    userDAO.clearSuspend(user.getEmail());
                    loginLogDAO.insertLog(user.getEmail(), "AUTO_UNSUSPEND", ip, ua);
                } 
                else {
                	loginLogDAO.insertLog(user.getEmail(), "SUSPENDED", ip, ua);
                    // 정지 기간이 아직 남아 있으면 로그인 차단
                    String message = "🚫 해당 계정은 정지되었습니다.\n";

                    if (user.getSuspendReason() != null)
                        message += "사유: " + user.getSuspendReason() + "\n";

                    message += "정지 해제 예정: " + user.getSuspendUntil();

                    return ResponseEntity.status(403).body(message);
                }
            } else {
                // 정지 해제 시간이 없는데 정지인 경우 → 무기한 정지
            	loginLogDAO.insertLog(user.getEmail(), "SUSPENDED", ip, ua);
                String message = "🚫 해당 계정은 무기한 정지되었습니다.\n";

                if (user.getSuspendReason() != null)
                    message += "사유: " + user.getSuspendReason();

                return ResponseEntity.status(403).body(message);
            }
        }

        // 4) 계정 잠금 여부 체크 (로그인 실패 5회)
        if (user.getLockUntil() != null) {
            LocalDateTime lockUntil = LocalDateTime.parse(user.getLockUntil(), DT_FORMAT);
            if (lockUntil.isAfter(LocalDateTime.now())) {
            	loginLogDAO.insertLog(user.getEmail(), "LOCKED", ip, ua);
                long remainSec =
                        Duration.between(LocalDateTime.now(), lockUntil).getSeconds();

                return ResponseEntity.status(403)
                        .body("🚫 계정이 잠겨있습니다. " + remainSec + "초 후 다시 시도 가능합니다.");
            }
        }

        // 5) 비밀번호 검증
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {

            Integer failCount = user.getLoginFailCount();
            int newFailCount = (failCount == null ? 0 : failCount) + 1;
            userDAO.updateFailCount(user.getEmail(), newFailCount);

            loginLogDAO.insertLog(user.getEmail(), "FAIL", ip, ua);
            
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

        // 6) 로그인 성공 → 실패 횟수 초기화
        userDAO.resetFailCount(user.getEmail());
        
        // 7) 로그인 성공 로그 기록
        loginLogDAO.insertLog(user.getEmail(), "SUCCESS", ip, ua);
        
        // 8) 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getRole());
        userDAO.updateRefreshToken(user.getEmail(), refreshToken);

        // 9) 유저 정보 반환
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
        
        // 소셜-only 계정 → password가 없음(null)
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.status(400).body(
                    "해당 계정은 소셜 로그인(" + user.getProvider() + ")으로 가입되었습니다.\n" +
                    "비밀번호가 존재하지 않으므로 재설정할 수 없습니다.\n" +
                    user.getProvider() + " 로그인으로 이용해주세요."
            );
        }
        
        // 토큰 발급 + 만료시간 30분,일반 계정 + 소셜 연동 계정 중 비밀번호가 있는 경우는 정상 처리
        String token = UUID.randomUUID().toString();
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(30);

        userDAO.updateResetToken(
                email,
                token,
                expireAt.format(DT_FORMAT)
        );
        
        // 이메일 발송
        mailService.sendPasswordResetMail(email, token);
        
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

        // 1) null / 공백 체크
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(400).body("Refresh Token이 없습니다.");
        }

        // 2) JWT 자체 검증 (서명, 만료시간 등)
        if (!jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body("Refresh Token이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.");
        }

        // 3) 토큰에서 email 꺼내기
        String email = jwtProvider.getEmailFromToken(refreshToken);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).body("Refresh Token에서 이메일 정보를 읽을 수 없습니다.");
        }

        // 4) email 기준으로 유저 조회
        UserInfoDTO user = userDAO.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(401).body("해당 사용자를 찾을 수 없습니다.");
        }

        // 5) 계정 상태 확인
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return ResponseEntity.status(403).body("계정이 활성 상태가 아닙니다.");
        }

        // 6) DB에 저장된 Refresh Token과 비교 (로테이션 핵심)
        String storedRefreshToken = user.getRefreshToken();

        // DB에 토큰이 없거나, 전달된 토큰과 다르면 → 탈취 / 재사용 의심
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {

            // 👉 보안상: DB의 Refresh Token 완전히 폐기
            userDAO.deleteRefreshToken(email);

            // 필요하면 로그 남기기 (여기서는 System.out 예시)
            System.out.println("[SECURITY] Refresh Token mismatch! email=" + email);

            return ResponseEntity.status(401)
                    .body("Refresh Token이 유효하지 않습니다. 다시 로그인해주세요.");
        }

        // 7) 여기까지 통과했다면: 정상적인 Refresh 요청
        //    → 새 Access Token + 새 Refresh Token 발급 (로테이션)
        String newAccessToken = jwtProvider.createAccessToken(email, user.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(email, user.getRole());

        // 8) DB에 새 Refresh Token 저장 (이전 토큰은 자동으로 폐기)
        userDAO.updateRefreshToken(email, newRefreshToken);

        // 9) 프론트에 내려줄 사용자 정보
        LoginUserInfoDTO userInfo = new LoginUserInfoDTO(
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getProvider(),
                user.getCreatedAt(),
                user.getAccountStatus()
        );

        // 10) 응답 DTO 구성 (새 Access + 새 Refresh + 유저 정보)
        LoginResponseDTO response = new LoginResponseDTO(
                newAccessToken,
                newRefreshToken,
                userInfo
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

        // 1) 이메일로 기존 회원 조회
        UserInfoDTO user = userDAO.findByEmail(social.getEmail());

        // 2) 없으면 신규 소셜 회원으로 INSERT
        if (user == null) {
            userDAO.insertSocialUser(
                    social.getEmail(),
                    social.getFullName(),
                    social.getProvider()
            );
            user = userDAO.findByEmail(social.getEmail());
        }

        // 3) 계정 상태 체크 (정지 등 필요하면 여기서)
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return ResponseEntity.status(403)
                    .body("❌ 사용이 제한된 계정입니다.");
        }

        // 4) JWT Access / Refresh 발급
        String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getRole());

        // 5) Refresh 토큰 DB 저장
        userDAO.updateRefreshToken(user.getEmail(), refreshToken);

        // 6) 화면으로 내려줄 사용자 정보
        LoginUserInfoDTO userInfo = new LoginUserInfoDTO(
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getProvider(),
                user.getCreatedAt(),
                user.getAccountStatus()
        );

        LoginResponseDTO response = new LoginResponseDTO(
                accessToken,
                refreshToken,
                userInfo
        );

        return ResponseEntity.ok(response);
    }
}
