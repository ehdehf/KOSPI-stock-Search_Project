package com.boot.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.boot.dto.StockDetailResponseDTO;
import com.boot.dto.StockInfoDTO;
import com.boot.dto.StockNewsDTO;
import com.boot.service.StockInfoService;
import com.boot.service.StockNewsService;
import com.boot.service.StockService;
import org.springframework.http.ResponseEntity;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
//@RequestMapping("/api")    
@RequiredArgsConstructor
/*
 * 검색 + 상세
 * */
@CrossOrigin(origins = "http://localhost:5173")
public class StockController {

    private final StockInfoService stockInfoService;
    private final StockNewsService stockNewsService;
    private final StockService stockService;

    // 자동완성 + 검색
    @GetMapping("/search")
    public List<StockInfoDTO> search(@RequestParam String keyword) {
        return stockInfoService.searchStocks(keyword);
    }

    // 상세보기
    @GetMapping("/{stockCode}")
    public StockDetailResponseDTO getDetail(@PathVariable String stockCode) {

        StockDetailResponseDTO dto = new StockDetailResponseDTO();

        dto.setStockInfo(stockInfoService.getStockDetail(stockCode));
        dto.setNewsList(stockNewsService.getNewsByStock(stockCode));
        dto.setSentiment(stockNewsService.getSentimentSummary(stockCode));

        return dto;
    }
    @PostMapping
    public String insertStockInfo(@RequestBody StockInfoDTO dto) {

        System.out.println("==== [CHECK] 들어온 STOCK_NAME ====");
        System.out.println(dto.getStockName());
        System.out.println("=================================");

        stockService.insertStockInfo(dto);
        return "OK";
    }


    @PostMapping("/news")
    public String insertStockNews(@RequestBody StockNewsDTO dto) {
    	stockService.insertStockNews(dto);
        return "OK";
    }
    // 시가총액 순위 조회 엔드포인트 추가
    @GetMapping("/marketcap") // 최종 경로: /api/stocks/marketcap
    public ResponseEntity<List<StockInfoDTO>> getMarketCapRanking() {
        
        // StockService의 메서드를 호출
        List<StockInfoDTO> ranking = stockService.selectTop100MarketCap(); 
        
        return ResponseEntity.ok(ranking);
    }
    
    // 🌟 급등/급락 종목 조회 API
    //테스트용 주석
    @GetMapping("/top-movers")
    public Map<String, List<StockInfoDTO>> getTopMovers() {
        
        List<StockInfoDTO> rising = stockService.selectTopRisingStocks();
        List<StockInfoDTO> falling = stockService.selectTopFallingStocks();
        
        Map<String, List<StockInfoDTO>> movers = new HashMap<>();
        movers.put("rising", rising);
        movers.put("falling", falling);
        
        return movers;
    }
}
