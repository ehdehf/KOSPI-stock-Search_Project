package com.boot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.boot.dto.StockDetailResponseDTO;
import com.boot.dto.StockInfoDTO;
import com.boot.dto.StockNewsDTO;
import com.boot.service.StockInfoService;
import com.boot.service.StockNewsService;
import com.boot.service.StockService;

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
}
