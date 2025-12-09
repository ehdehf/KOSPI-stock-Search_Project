package com.boot.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.boot.dao.StockNewsDAO;
import com.boot.dto.StockNewsDTO;
import com.boot.dto.SentimentSummaryDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockNewsServiceImpl implements StockNewsService {

    private final StockNewsDAO stockNewsDAO;

    // ===========================================================
    // 1) 종목별 뉴스 조회
    // ===========================================================
    @Override
    public List<StockNewsDTO> getNewsByStock(String stockCode) {
        return stockNewsDAO.getNewsByStock(stockCode);
    }

    // ===========================================================
    // 2) 종목 감성 요약 (기본)
    // ===========================================================
    @Override
    public SentimentSummaryDTO getSentimentSummary(String stockCode) {
        Map<String, Object> result = stockNewsDAO.getSentimentSummary(stockCode);

        if (result == null) {
            return new SentimentSummaryDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }

        int positive = getNumber(result.get("POSITIVECOUNT"));
        int negative = getNumber(result.get("NEGATIVECOUNT"));
        int neutral = getNumber(result.get("NEUTRALCOUNT"));

        int total = positive + negative + neutral;

        SentimentSummaryDTO dto = new SentimentSummaryDTO();
        dto.setPositiveCount(positive);
        dto.setNegativeCount(negative);
        dto.setNeutralCount(neutral);

        if (total > 0) {
            dto.setPositiveRate(positive * 100.0 / total);
            dto.setNegativeRate(negative * 100.0 / total);
            dto.setNeutralRate(neutral * 100.0 / total);
        } else {
            dto.setPositiveRate(0);
            dto.setNegativeRate(0);
            dto.setNeutralRate(0);
        }

        return dto;
    }

    // ===========================================================
    // 3) 종목 감성 상세 통계
    // ===========================================================
    @Override
    public SentimentSummaryDTO getSentimentSummaryByStock(String stockCode) {
        Map<String, Object> result = stockNewsDAO.getSentimentSummaryByStock(stockCode);

        if (result == null) {
            return new SentimentSummaryDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }

        int positive = getNumber(result.get("POSITIVECOUNT"));
        int negative = getNumber(result.get("NEGATIVECOUNT"));
        int neutral = getNumber(result.get("NEUTRALCOUNT"));

        double positiveRatio = getDouble(result.get("POSITIVERATIO"));
        double negativeRatio = getDouble(result.get("NEGATIVERATIO"));

        SentimentSummaryDTO dto = new SentimentSummaryDTO();
        dto.setPositiveCount(positive);
        dto.setNegativeCount(negative);
        dto.setNeutralCount(neutral);
        dto.setPositiveRate(positiveRatio);
        dto.setNegativeRate(negativeRatio);
        dto.setNeutralRate(100.0 - positiveRatio - negativeRatio);

        return dto;
    }

    // ===========================================================
    // 4) 종목 감성 상세 통계 (기간 필터링 지원)
    // ===========================================================
    @Override
    public SentimentSummaryDTO getSentimentSummaryByStockWithPeriod(String stockCode, int days) {
        Map<String, Object> result = stockNewsDAO.getSentimentSummaryByStockWithPeriod(stockCode, days);

        if (result == null) {
            return new SentimentSummaryDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }

        int positive = getNumber(result.get("POSITIVECOUNT"));
        int negative = getNumber(result.get("NEGATIVECOUNT"));
        int neutral = getNumber(result.get("NEUTRALCOUNT"));

        double positiveRatio = getDouble(result.get("POSITIVERATIO"));
        double negativeRatio = getDouble(result.get("NEGATIVERATIO"));

        SentimentSummaryDTO dto = new SentimentSummaryDTO();
        dto.setPositiveCount(positive);
        dto.setNegativeCount(negative);
        dto.setNeutralCount(neutral);
        dto.setPositiveRate(positiveRatio);
        dto.setNegativeRate(negativeRatio);
        dto.setNeutralRate(100.0 - positiveRatio - negativeRatio);

        return dto;
    }

    // ===========================================================
    // 5) 전체 종목 감성 요약 (전체 기간)
    // ===========================================================
    @Override
    public List<Map<String, Object>> getAllStockSentimentSummary() {
        return stockNewsDAO.getAllStockSentimentSummary();
    }

    // ===========================================================
    // ⭐ 6) 전체 종목 감성 요약 (기간 필터링)
    //    → "감성 대시보드"에서 사용하는 핵심 메서드
    // ===========================================================
    @Override
    public List<Map<String, Object>> getAllStockSentimentSummaryWithPeriod(int days) {
        return stockNewsDAO.getAllStockSentimentSummaryWithPeriod(days);
    }

    // ===========================================================
    // 7) 종목별 감성 트렌드 (날짜별)
    // ===========================================================
    @Override
    public List<Map<String, Object>> getSentimentTrendByStock(String stockCode, int days) {
        return stockNewsDAO.getSentimentTrendByStock(stockCode, days);
    }

    // ===========================================================
    // 8) 종목별 키워드 TOP10
    // ===========================================================
    @Override
    public List<Map<String, Object>> getTopKeywordsByStock(String stockCode) {
        List<Map<String, Object>> keywordRows = stockNewsDAO.getTopKeywordsByStock(stockCode);
        return extractTopKeywords(keywordRows, 10);
    }

    // ===========================================================
    // 9) 전체 뉴스 기준 키워드 TOP20
    // ===========================================================
    @Override
    public List<Map<String, Object>> getTopKeywordsAll(int days) {
        List<Map<String, Object>> keywordRows = stockNewsDAO.getTopKeywordsAll(days);
        return extractTopKeywords(keywordRows, 20);
    }

    // ===========================================================
    // 10) 전체 감성 통계 (기사 전체 기준)
    // ===========================================================
    @Override
    public Map<String, Object> getOverallSentimentSummary() {
        return stockNewsDAO.getOverallSentimentSummary();
    }

    // ===========================================================
    // 🔧 공통 유틸리티 - 키워드 집계
    // ===========================================================
    private List<Map<String, Object>> extractTopKeywords(List<Map<String, Object>> keywordRows, int limit) {
        Map<String, Integer> map = new HashMap<>();

        for (Map<String, Object> row : keywordRows) {
            Object clobOrString = row.values().iterator().next();
            String text = safeClobToString(clobOrString);

            if (text == null) continue;

            String[] arr = text.split(",\\s*|,");
            for (String kw : arr) {
                kw = kw.trim();
                if (!kw.isEmpty() && kw.length() <= 100) {
                    map.put(kw, map.getOrDefault(kw, 0) + 1);
                }
            }
        }

        return map.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("keyword", e.getKey());
                    r.put("keywordCount", e.getValue());
                    return r;
                })
                .collect(Collectors.toList());
    }

    private String safeClobToString(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof String) return (String) obj;
            if (obj instanceof java.sql.Clob) {
                java.sql.Clob clob = (java.sql.Clob) obj;
                return clob.getSubString(1, (int) clob.length());
            }
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ===========================================================
    // 🔧 숫자/실수 변환 유틸
    // ===========================================================
    private int getNumber(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    private double getDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
}
