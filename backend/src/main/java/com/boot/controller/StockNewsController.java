package com.boot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.boot.dto.StockNewsDTO;
import com.boot.dto.SentimentSummaryDTO;
import com.boot.service.StockNewsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class StockNewsController {

    private final StockNewsService stockNewsService;

    // 종목별 뉴스 리스트
    @GetMapping("/{stockCode}")
    public List<StockNewsDTO> getNews(@PathVariable String stockCode) {
        return stockNewsService.getNewsByStock(stockCode);
    }

    // 종목별 감성 요약 (기본)
    @GetMapping("/{stockCode}/sentiment")
    public SentimentSummaryDTO getSentimentSummary(@PathVariable String stockCode) {
        return stockNewsService.getSentimentSummary(stockCode);
    }

    // 종목별 감성 통계 (상세)
    @GetMapping("/{stockCode}/sentiment/detail")
    public SentimentSummaryDTO getSentimentSummaryByStock(@PathVariable String stockCode) {
        return stockNewsService.getSentimentSummaryByStock(stockCode);
    }

    // 종목별 감성 통계 (기간 필터링)
    @GetMapping("/{stockCode}/sentiment/period")
    public SentimentSummaryDTO getSentimentSummaryWithPeriod(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "30") int days) {
        return stockNewsService.getSentimentSummaryByStockWithPeriod(stockCode, days);
    }

    // 전체 종목별 감성 통계 (대시보드용)
    @GetMapping("/sentiment/all")
    public List<Map<String, Object>> getAllStockSentimentSummary() {
        return stockNewsService.getAllStockSentimentSummary();
    }

    // 전체 종목별 감성 통계 (기간 필터링)
    @GetMapping("/sentiment/all/period")
    public List<Map<String, Object>> getAllStockSentimentSummaryWithPeriod(
            @RequestParam(defaultValue = "30") int days) {
        return stockNewsService.getAllStockSentimentSummaryWithPeriod(days);
    }

    // 종목별 날짜별 감성 통계 (트렌드)
    @GetMapping("/{stockCode}/sentiment/trend")
    public List<Map<String, Object>> getSentimentTrend(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "30") int days) {
        return stockNewsService.getSentimentTrendByStock(stockCode, days);
    }

    // 키워드 TOP 10 (특정 종목)
    @GetMapping("/{stockCode}/keywords")
    public List<Map<String, Object>> getTopKeywordsByStock(@PathVariable String stockCode) {
        return stockNewsService.getTopKeywordsByStock(stockCode);
    }

    // 전체 키워드 TOP 20 (트렌드)
    @GetMapping("/keywords/top")
    public List<Map<String, Object>> getTopKeywordsAll(
            @RequestParam(defaultValue = "30") int days) {
        return stockNewsService.getTopKeywordsAll(days);
    }

    // 전체 감성 통계
    @GetMapping("/sentiment/overall")
    public Map<String, Object> getOverallSentimentSummary() {
        return stockNewsService.getOverallSentimentSummary();
    }
}
