package com.boot.service;

import java.util.List;
import com.boot.dto.StockInfoDTO;
import com.boot.dto.StockNewsDTO;

public interface StockService {
    void insertStockInfo(StockInfoDTO dto);
    void insertStockNews(StockNewsDTO dto);

    List<StockInfoDTO> selectTop100MarketCap();
}
