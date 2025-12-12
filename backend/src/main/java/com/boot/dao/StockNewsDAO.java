// StockNewsDAO.java
package com.boot.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.boot.dto.StockNewsDTO;

@Mapper
public interface StockNewsDAO {

    List<StockNewsDTO> getNewsByStock(String stockCode);

    Map<String, Object> getSentimentSummary(String stockCode);

    // 종목별 감성 통계 (상세)
    Map<String, Object> getSentimentSummaryByStock(String stockCode);

    // 종목별 감성 통계 (기간 필터링)
    Map<String, Object> getSentimentSummaryByStockWithPeriod(@Param("stockCode") String stockCode, @Param("days") int days);

    // 전체 종목별 감성 통계 (대시보드용)
    List<Map<String, Object>> getAllStockSentimentSummary();

    // 전체 종목별 감성 통계 (기간 필터링)
    List<Map<String, Object>> getAllStockSentimentSummaryWithPeriod(@Param("days") int days);

    // 종목별 날짜별 감성 통계 (트렌드)
    List<Map<String, Object>> getSentimentTrendByStock(@Param("stockCode") String stockCode, @Param("days") int days);

    // 키워드 TOP 10 (특정 종목) - KEYWORDS만 반환
    List<Map<String, Object>> getTopKeywordsByStock(String stockCode);

    // 전체 키워드 TOP 20 (트렌드) - KEYWORDS만 반환
    List<Map<String, Object>> getTopKeywordsAll(@Param("days") int days);

    // 전체 감성 통계
    Map<String, Object> getOverallSentimentSummary();
    
    // ✅ 산업 목록 조회
    List<String> getIndustries();

    // ✅ 산업별 뉴스 조회
    List<StockNewsDTO> getNewsByIndustry(@Param("industry") String industry);

    // ✅ 키워드별 뉴스 조회
    List<StockNewsDTO> getNewsByKeyword(@Param("keyword") String keyword);

    // ✅ 키워드별 종목 조회
    List<Map<String, Object>> getStocksByKeyword(@Param("keyword") String keyword);

    List<Map<String, Object>> getTop10PopularStocks();

}
