// StockNewsService.java
package com.boot.service;

import java.util.List;
import java.util.Map;

import com.boot.dto.StockNewsDTO;
import com.boot.dto.SentimentSummaryDTO;

public interface StockNewsService {

    List<StockNewsDTO> getNewsByStock(String stockCode);

    SentimentSummaryDTO getSentimentSummary(String stockCode);

    // 종목별 감성 통계 (상세)
    SentimentSummaryDTO getSentimentSummaryByStock(String stockCode);

    // 종목별 감성 통계 (기간 필터링)
    SentimentSummaryDTO getSentimentSummaryByStockWithPeriod(String stockCode, int days);

    // 전체 종목별 감성 통계 (대시보드용)
    List<Map<String, Object>> getAllStockSentimentSummary();

    // 전체 종목별 감성 통계 (기간 필터링)
    List<Map<String, Object>> getAllStockSentimentSummaryWithPeriod(int days);

    // 종목별 날짜별 감성 통계 (트렌드)
    List<Map<String, Object>> getSentimentTrendByStock(String stockCode, int days);

    // 키워드 TOP 10 (특정 종목)
    List<Map<String, Object>> getTopKeywordsByStock(String stockCode);

    // 전체 키워드 TOP 20 (트렌드)
    List<Map<String, Object>> getTopKeywordsAll(int days);

    // 전체 감성 통계
    Map<String, Object> getOverallSentimentSummary();
}
