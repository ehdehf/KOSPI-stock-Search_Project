package com.boot.service;

import com.boot.dao.NewsMonitorDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsMonitorService {

    private final NewsMonitorDAO newsMonitorDAO;

    private static final long NORMAL_MIN = 10; // 10분
    private static final long DELAY_MIN  = 30; // 30분

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> getRefreshStatus() {

        Map<String, Object> row = newsMonitorDAO.getLastNewsCreatedAt();
        Object raw = (row == null) ? null : row.get("LAST_CREATED_AT");

        Map<String, Object> res = new HashMap<>();

        // 1) 데이터가 아예 없으면
        if (raw == null) {
            res.put("status", "FAIL");
            res.put("lastCreatedAt", null);
            res.put("delayMinutes", null);
            res.put("message", "뉴스 데이터가 없습니다 (크롤링 미동작/DB 미삽입)");
            return res;
        }

        // 2) Oracle DATE -> Timestamp로 들어오는 케이스가 많음
        LocalDateTime last;
        if (raw instanceof Timestamp) {
            last = ((Timestamp) raw).toLocalDateTime();
        } else {
            // 혹시 Date/String으로 들어와도 최대한 안전하게 처리
            last = LocalDateTime.parse(raw.toString().replace("T", " ").substring(0, 19), fmt);
        }

        LocalDateTime now = LocalDateTime.now();
        long diffMin = Duration.between(last, now).toMinutes();

        String status;
        String message;

        if (diffMin <= NORMAL_MIN) {
            status = "NORMAL";
            message = "정상 갱신 중";
        } else if (diffMin <= DELAY_MIN) {
            status = "DELAY";
            message = "갱신 지연 (모니터링 필요)";
        } else {
            status = "FAIL";
            message = "갱신 실패 의심 (스케줄러/크롤러 확인 필요)";
        }

        res.put("status", status);
        res.put("lastCreatedAt", last.format(fmt));
        res.put("delayMinutes", diffMin);
        res.put("message", message);

        return res;
    }
}
