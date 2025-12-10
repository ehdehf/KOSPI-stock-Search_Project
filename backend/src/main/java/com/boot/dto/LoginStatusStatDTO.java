package com.boot.dto;

import lombok.Data;

/*
 * 로그인 성공/실패/잠금 (막대차트)
 * */
@Data
public class LoginStatusStatDTO {

    private String status;   // SUCCESS / FAIL / LOCKED / SUSPENDED 등
    private int count;       // 횟수
}