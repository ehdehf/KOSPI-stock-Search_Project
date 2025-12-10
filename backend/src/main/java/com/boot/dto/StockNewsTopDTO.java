package com.boot.dto;

import lombok.Data;

/*
 * 뉴스 많이 나온 종목 탑5 (막대차트)
 * */
@Data
public class StockNewsTopDTO {

    private String stockCode;
    private String stockName;
    private int newsCount;
}