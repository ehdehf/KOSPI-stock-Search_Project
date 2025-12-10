package com.boot.dto;

import lombok.Data;

/*
 * 상단 카드 통계
 * */
@Data
public class DashboardSummaryDTO {

    private int totalUsers;         // 전체 사용자 수
    private int activeUsers;        // ACTIVE
    private int suspendedUsers;     // IS_SUSPENDED = 'Y'
    private int waitingVerifyUsers; // ACCOUNT_STATUS = 'WAITING_VERIFY'
    private int dangerUsers;        // LOGIN_FAIL_COUNT >= 3

    private int totalStocks;        // STOCK_INFO 전체 종목 수
    private int totalNews;          // STOCK_NEWS 전체 뉴스 수
}