@echo off
chcp 65001 >nul
REM ============================================================
REM 스케줄러 삭제 스크립트
REM ============================================================

echo 스케줄러 삭제 중...

schtasks /Delete /TN "KStockNewsCrawler" /F
schtasks /Delete /TN "KStockSentimentAnalyzer" /F

echo 스케줄러 삭제 완료!
pause