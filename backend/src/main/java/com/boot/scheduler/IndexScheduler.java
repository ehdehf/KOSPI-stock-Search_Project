package com.boot.scheduler;

import com.boot.service.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class IndexScheduler {

    @Autowired
    private IndexService indexService;

    // 매일 17:00 KST 에 전일(또는 오늘 기준 적절한 날짜)의 KOSPI 데이터 저장
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Seoul")
    public void saveDailyKospi() {
        String targetDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        System.out.println("[SCHEDULER] KOSPI 일일 저장 시작: " + targetDate);
        indexService.saveSingleDayData(targetDate);
        System.out.println("[SCHEDULER] KOSPI 일일 저장 완료: " + targetDate);
    }

    // 매일 17:02 KST 에 KOSDAQ 데이터 저장
    @Scheduled(cron = "0 5 3 * * ?", zone = "Asia/Seoul")
    public void saveDailyKosdaq() {
        String targetDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        System.out.println("[SCHEDULER] KOSDAQ 일일 저장 시작: " + targetDate);
        indexService.saveSingleKosdaqDayData(targetDate);
        System.out.println("[SCHEDULER] KOSDAQ 일일 저장 완료: " + targetDate);
    }
}
