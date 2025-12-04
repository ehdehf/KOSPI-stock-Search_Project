package com.boot.controller;

import com.boot.dto.IndexDataDTO;
import com.boot.service.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chart")
@CrossOrigin(origins = "http://localhost:5173")
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("/kospi-history")
    public ResponseEntity<List<IndexDataDTO>> getKospiHistory() {
        return ResponseEntity.ok(indexService.getKospiTimeSeriesData());
    }

    @GetMapping("/kosdaq-history")
    public ResponseEntity<List<IndexDataDTO>> getKosdaqHistory() {
        return ResponseEntity.ok(indexService.getKosdaqTimeSeriesData());
    }
}
