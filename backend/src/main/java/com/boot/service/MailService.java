package com.boot.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendVerificationMail(String to, String token) {

//        String link = "https://frontend.com/verify?token=" + token;
//    	먼저 프론트로 보내고 거기서 백엔드로 인증 요청 보내기
        String link = "http://localhost:5173/verify?token=" + token;
    	

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[검색엔진 프로젝트] 이메일 인증 요청");
        message.setText(
                "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n\n"
                + link + "\n\n"
                + "본 인증 링크는 30분 동안 유효합니다."
        );

        mailSender.send(message);
    }
    // 비밀번호 재설정 메일 (새로 추가하는 기능)
    public void sendPasswordResetMail(String to, String token) {

        // 프론트에서 열릴 비밀번호 재설정 화면
        // → 프론트가 token으로 백엔드 POST /auth/reset/confirm 를 호출하는 구조
        String link = "http://localhost:5173/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[검색엔진 프로젝트] 비밀번호 재설정 안내");
        message.setText(
                "아래 링크를 클릭하여 비밀번호를 재설정하세요.\n\n"
                + link + "\n\n"
                + "본 재설정 링크는 30분 동안 유효합니다."
        );

        mailSender.send(message);
    }
}
