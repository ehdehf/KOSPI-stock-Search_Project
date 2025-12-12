// StockNewsServiceImpl.java
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

    @Override
    public List<StockNewsDTO> getNewsByStock(String stockCode) {
        return stockNewsDAO.getNewsByStock(stockCode);
    }

    @Override
    public SentimentSummaryDTO getSentimentSummary(String stockCode) {
        Map<String, Object> result = stockNewsDAO.getSentimentSummary(stockCode);

        if (result == null) {
            return new SentimentSummaryDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }

        int positive = getNumber(result.get("POSITIVECOUNT"));
        int negative = getNumber(result.get("NEGATIVECOUNT"));
        int neutral = getNumber(result.get("NEUTRALCOUNT"));

        SentimentSummaryDTO dto = new SentimentSummaryDTO();
        dto.setPositiveCount(positive);
        dto.setNegativeCount(negative);
        dto.setNeutralCount(neutral);

        int total = positive + negative + neutral;

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

    @Override
    public SentimentSummaryDTO getSentimentSummaryByStock(String stockCode) {
        Map<String, Object> result = stockNewsDAO.getSentimentSummaryByStock(stockCode);

        if (result == null) {
            return new SentimentSummaryDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }

        int total = getNumber(result.get("TOTALNEWS"));
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

    @Override
    public SentimentSummaryDTO getSentimentSummaryByStockWithPeriod(String stockCode, int days) {
        Map<String, Object> result = stockNewsDAO.getSentimentSummaryByStockWithPeriod(stockCode, days);

        if (result == null) {
            return new SentimentSummaryDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }

        int total = getNumber(result.get("TOTALNEWS"));
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

    @Override
    public List<Map<String, Object>> getAllStockSentimentSummary() {
        return stockNewsDAO.getAllStockSentimentSummary();
    }

    @Override
    public List<Map<String, Object>> getAllStockSentimentSummaryWithPeriod(int days) {
        return stockNewsDAO.getAllStockSentimentSummaryWithPeriod(days);
    }

    @Override
    public List<Map<String, Object>> getSentimentTrendByStock(String stockCode, int days) {
        return stockNewsDAO.getSentimentTrendByStock(stockCode, days);
    }

    @Override
    public List<Map<String, Object>> getTopKeywordsByStock(String stockCode) {
        // DB에서 KEYWORDS만 조회
        List<Map<String, Object>> keywordRows = stockNewsDAO.getTopKeywordsByStock(stockCode);
        
        // Java에서 키워드 분리 및 카운트
        Map<String, Integer> keywordCountMap = new HashMap<>();
        
        for (Map<String, Object> row : keywordRows) {
            // 대소문자 구분 없이 KEYWORDS 찾기
            Object keywordsObj = null;
            for (String key : row.keySet()) {
                if (key != null && key.toUpperCase().equals("KEYWORDS")) {
                    keywordsObj = row.get(key);
                    break;
                }
            }
            
            // KEYWORDS를 찾지 못한 경우 첫 번째 값 사용
            if (keywordsObj == null && !row.isEmpty()) {
                keywordsObj = row.values().iterator().next();
            }
            
            String keywords = null;
            
            // CLOB 처리
            if (keywordsObj != null) {
                if (keywordsObj instanceof String) {
                    keywords = (String) keywordsObj;
                } else {
                    try {
                        if (keywordsObj.getClass().getName().contains("CLOB")) {
                            java.sql.Clob clob = (java.sql.Clob) keywordsObj;
                            long clobLength = clob.length();
                            if (clobLength > 0) {
                                keywords = clob.getSubString(1, (int) clobLength);
                            }
                        } else {
                            keywords = keywordsObj.toString();
                        }
                    } catch (Exception e) {
                        keywords = keywordsObj.toString();
                    }
                }
            }
            
            if (keywords != null && !keywords.trim().isEmpty()) {
                // 쉼표로 분리 (쉼표+공백 또는 쉼표만)
                String[] keywordArray = keywords.split(",\\s*|,");
                for (String keyword : keywordArray) {
                    keyword = keyword.trim();
                    // 빈 문자열이 아니고, 너무 긴 단어 제외 (100자 이상)
                    if (!keyword.isEmpty() && keyword.length() <= 100) {
                        keywordCountMap.put(keyword, keywordCountMap.getOrDefault(keyword, 0) + 1);
                    }
                }
            }
        }
        
        // 카운트 순으로 정렬하고 TOP 10 반환
        return keywordCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("keyword", entry.getKey());
                    result.put("keywordCount", entry.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTopKeywordsAll(int days) {
        // DB에서 KEYWORDS만 조회
        List<Map<String, Object>> keywordRows = stockNewsDAO.getTopKeywordsAll(days);
        
        // Java에서 키워드 분리 및 카운트
        Map<String, Integer> keywordCountMap = new HashMap<>();
        
        for (Map<String, Object> row : keywordRows) {
            // 대소문자 구분 없이 KEYWORDS 찾기
            Object keywordsObj = null;
            for (String key : row.keySet()) {
                if (key != null && key.toUpperCase().equals("KEYWORDS")) {
                    keywordsObj = row.get(key);
                    break;
                }
            }
            
            // KEYWORDS를 찾지 못한 경우 첫 번째 값 사용
            if (keywordsObj == null && !row.isEmpty()) {
                keywordsObj = row.values().iterator().next();
            }
            
            String keywords = null;
            
            // CLOB 처리
            if (keywordsObj != null) {
                if (keywordsObj instanceof String) {
                    keywords = (String) keywordsObj;
                } else {
                    try {
                        if (keywordsObj.getClass().getName().contains("CLOB")) {
                            java.sql.Clob clob = (java.sql.Clob) keywordsObj;
                            long clobLength = clob.length();
                            if (clobLength > 0) {
                                keywords = clob.getSubString(1, (int) clobLength);
                            }
                        } else {
                            keywords = keywordsObj.toString();
                        }
                    } catch (Exception e) {
                        keywords = keywordsObj.toString();
                    }
                }
            }
            
            if (keywords != null && !keywords.trim().isEmpty()) {
                // 쉼표로 분리 (쉼표+공백 또는 쉼표만)
                String[] keywordArray = keywords.split(",\\s*|,");
                for (String keyword : keywordArray) {
                    keyword = keyword.trim();
                    // 빈 문자열이 아니고, 너무 긴 단어 제외 (100자 이상)
                    if (!keyword.isEmpty() && keyword.length() <= 100) {
                        keywordCountMap.put(keyword, keywordCountMap.getOrDefault(keyword, 0) + 1);
                    }
                }
            }
        }
        
        // 카운트 순으로 정렬하고 TOP 20 반환
        return keywordCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("keyword", entry.getKey());
                    result.put("keywordCount", entry.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getOverallSentimentSummary() {
        return stockNewsDAO.getOverallSentimentSummary();
    }

    // 안전한 숫자 변환 함수
    private int getNumber(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    private double getDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0.0;
    }
    
    @Override
    public List<String> getIndustries() {
        return stockNewsDAO.getIndustries();
    }

    @Override
    public List<StockNewsDTO> getNewsByIndustry(String industry) {
        return stockNewsDAO.getNewsByIndustry(industry);
    }

    @Override
    public List<StockNewsDTO> getNewsByKeyword(String keyword) {
        return stockNewsDAO.getNewsByKeyword(keyword);
    }

    @Override
    public List<Map<String, Object>> getStocksByKeyword(String keyword) {
        return stockNewsDAO.getStocksByKeyword(keyword);
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
    
    @Override
    public List<Map<String, Object>> getTop10PopularStocks() {
        return stockNewsDAO.getTop10PopularStocks();
    }

}
