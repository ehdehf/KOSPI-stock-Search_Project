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
}
