package com.boot.controller;

import com.boot.dto.LoginRequestDTO;
import com.boot.dto.PasswordResetConfirmDTO;
import com.boot.dto.PasswordResetRequestDTO;
import com.boot.dto.RefreshRequestDTO;
import com.boot.dto.RegisterRequestDTO;
import com.boot.dto.SocialUserInfoDTO;
import com.boot.service.AuthService;
import com.boot.service.GoogleOAuthService;
import com.boot.service.KakaoOAuthService;
import com.boot.service.NaverOAuthService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final KakaoOAuthService kakaoOAuthService;
	private final NaverOAuthService naverOAuthService;
	private final GoogleOAuthService googleOAuthService;

    // 로그인 요청 처리
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO req, HttpServletRequest request) {

        // 모든 로그인 로직은 서비스로 위임
        return authService.login(req, request);
    }
    // 이메일 인증 확인
    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        return authService.checkEmail(email);
    }
    
    //회원 가입
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO req) {
        return authService.register(req);
    }
    
    // 이메일 인증
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }
    
    // 비밀번호 재설정
    @PostMapping("/reset/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDTO req) {
        return authService.requestPasswordReset(req.getEmail());
    }

    @GetMapping("/reset/verify")
    public ResponseEntity<?> verifyResetToken(@RequestParam String token) {
        return authService.verifyResetToken(token);
    }

    @PostMapping("/reset/confirm")
    public ResponseEntity<?> confirmReset(@Valid @RequestBody PasswordResetConfirmDTO req) {
        return authService.resetPassword(req);
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequestDTO req) {
        return authService.refresh(req.getRefreshToken());
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        return authService.logout(email);
    }
    
    //카카오 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<?> kakaoCallback(@RequestParam String code) {

        SocialUserInfoDTO social = kakaoOAuthService.getUserInfo(code);

        return authService.socialLogin(social);
    }
    
    //네이버 로그인
    @GetMapping("/naver/callback")
    public ResponseEntity<?> naverCallback(
            @RequestParam String code,
            @RequestParam String state) {

        SocialUserInfoDTO socialUser = naverOAuthService.getUserInfo(code, state);
        return authService.socialLogin(socialUser);
    }
    
    //구글 로그인
    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code) {

        SocialUserInfoDTO social = googleOAuthService.getUserInfo(code);

        return authService.socialLogin(social);
    }
}
