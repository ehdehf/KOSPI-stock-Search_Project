package com.boot.controller;

import com.boot.service.NewsMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsMonitorService newsMonitorService;

    @GetMapping("/refresh-status")
    public ResponseEntity<?> refreshStatus() {
        return ResponseEntity.ok(newsMonitorService.getRefreshStatus());
    }
}
