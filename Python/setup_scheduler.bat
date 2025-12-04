@echo off
chcp 65001 >nul
REM ============================================================
REM 뉴스 크롤링 + 감성 분석 자동 실행 스케줄러 등록
REM 1시간마다 자동 실행
REM ============================================================

set PYTHON_PATH=C:\Users\KH\AppData\Local\Programs\Python\Python313\python.exe
set WORK_DIR=C:\temp6
set CRAWLER_SCRIPT=crawler_stock_news.py
set SENTIMENT_SCRIPT=main.py

echo ============================================================
echo 스케줄러 등록 시작 (1시간마다 실행)
echo ============================================================

REM 기존 스케줄러 삭제 (이미 등록되어 있으면)
schtasks /Delete /TN "KStockNewsCrawler" /F 2>nul
schtasks /Delete /TN "KStockSentimentAnalyzer" /F 2>nul

REM 1. 뉴스 크롤링 스케줄러 등록 (1시간마다)
echo.
echo [1] 뉴스 크롤링 스케줄러 등록 중...
schtasks /Create /TN "KStockNewsCrawler" ^
    /TR "\"%PYTHON_PATH%\" \"%WORK_DIR%\%CRAWLER_SCRIPT%\"" ^
    /SC HOURLY ^
    /ST 00:00 ^
    /RU SYSTEM ^
    /RL HIGHEST ^
    /F

if %ERRORLEVEL% EQU 0 (
    echo ✓ 뉴스 크롤링 스케줄러 등록 완료!
) else (
    echo ✗ 뉴스 크롤링 스케줄러 등록 실패
)

REM 2. 감성 분석 스케줄러 등록 (1시간마다, 크롤링 5분 후 실행)
echo.
echo [2] 감성 분석 스케줄러 등록 중...
schtasks /Create /TN "KStockSentimentAnalyzer" ^
    /TR "\"%PYTHON_PATH%\" \"%WORK_DIR%\%SENTIMENT_SCRIPT%\"" ^
    /SC HOURLY ^
    /ST 00:05 ^
    /RU SYSTEM ^
    /RL HIGHEST ^
    /F

if %ERRORLEVEL% EQU 0 (
    echo ✓ 감성 분석 스케줄러 등록 완료!
) else (
    echo ✗ 감성 분석 스케줄러 등록 실패
)

echo.
echo ============================================================
echo 스케줄러 등록 완료!
echo ============================================================
echo.
echo 등록된 작업:
schtasks /Query /TN "KStockNewsCrawler" /FO LIST /V
echo.
schtasks /Query /TN "KStockSentimentAnalyzer" /FO LIST /V
echo.
echo 스케줄러 확인: 작업 스케줄러 앱에서 확인하세요
pause