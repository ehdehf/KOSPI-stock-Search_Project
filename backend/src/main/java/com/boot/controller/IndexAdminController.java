package com.boot.controller;

import com.boot.service.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/index")
public class IndexAdminController {

    @Autowired
    private IndexService indexService;

    @GetMapping("/init/kospi")
    public ResponseEntity<String> initKospi() {
        indexService.initiateHistoricalDataCollection();
        return ResponseEntity.ok("KOSPI init started");
    }

    @GetMapping("/init/kosdaq")
    public ResponseEntity<String> initKosdaq() {
        indexService.initiateKosdaqHistoricalDataCollection();
        return ResponseEntity.ok("KOSDAQ init started");
    }
}
