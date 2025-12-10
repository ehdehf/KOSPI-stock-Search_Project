package com.boot.dto;

import lombok.Data;


/*
 * 일별 가입자 수(라인차트)
 * */
@Data
public class DailyUserJoinDTO {

    private String joinDate;  // '2025-12-10' 형태
    private int count;        // 해당 날짜 신규 가입자 수
}