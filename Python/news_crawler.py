# news_crawler.py - 종목명 매칭 개선 + 날짜 추출 개선 + 감성 분석 통합 (NULL 방지 강화, 스케줄러 지원, 1시간 간격 체크)
import requests
from bs4 import BeautifulSoup
from datetime import datetime, timedelta, date
import time
import re
import oracledb
import sys
import json
from pathlib import Path

# 감성 분석기 import (실패해도 계속 진행)
try:
    from news_sentiment_analyzer import SentimentAnalyzer
    sentiment_analyzer = SentimentAnalyzer(use_morphology=True)
    SENTIMENT_AVAILABLE = True
    print("✓ 감성 분석기 로드 성공")
except Exception as e:
    print(f"⚠ 감성 분석기 로드 실패: {e}")
    print("   기본값으로 진행합니다.")
    SENTIMENT_AVAILABLE = False
    sentiment_analyzer = None

# ===============================
# DB 연결 정보
# ===============================
DB_USER = "opendata_user"
DB_PASSWORD = "opendata123"
DB_DSN = "192.168.10.34:1521/XE"

# ===============================
# 네이버 뉴스 URL
# ===============================
BASE_URL = "https://news.naver.com/main/list.naver"
HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"
}

# ===============================
# 공통: 문자열 정리
# ===============================
def clean(text):
    if not text:
        return None
    text = re.sub(r'[\x00-\x1F\u200b-\u200f]', '', text)
    text = re.sub('<[^>]+>', '', text)
    text = ' '.join(text.split())
    return text.strip()

# ===============================
# 감성 분석 강제 수행 함수 (NULL 방지 강화)
# ===============================
def ensure_sentiment_analysis(title, content):
    """
    감성 분석을 반드시 수행하고 결과를 반환 (NULL 방지)
    실패해도 기본값을 반환하여 NULL이 절대 나오지 않도록 보장
    """
    sentiment = "보통"
    score = 0
    keywords = " "
    
    # 최대 3번 재시도
    max_retries = 3
    for attempt in range(max_retries):
        try:
            if SENTIMENT_AVAILABLE and sentiment_analyzer:
                result = sentiment_analyzer.analyze_sentiment(title, content)
                if result and len(result) >= 3:
                    sentiment_val, score_val, keywords_val = result
                    
                    # 감성 값 검증
                    if sentiment_val and sentiment_val in ['긍정', '부정', '보통']:
                        sentiment = sentiment_val
                    else:
                        sentiment = "보통"
                    
                    # 점수 검증
                    try:
                        score = int(score_val) if score_val is not None else 0
                    except (ValueError, TypeError):
                        score = 0
                    
                    # 키워드 검증
                    if keywords_val and str(keywords_val).strip():
                        keywords = str(keywords_val).strip()[:990]
                    else:
                        keywords = " "
                    
                    # 성공적으로 분석 완료
                    return sentiment, score, keywords
        except Exception as e:
            if attempt < max_retries - 1:
                time.sleep(0.5)  # 재시도 전 대기
                continue
            else:
                print(f"    ⚠ 감성 분석 실패 (재시도 {attempt + 1}/{max_retries}): {e}")
    
    # 모든 시도 실패 시 기본값 반환
    print(f"    ⚠ 감성 분석 실패, 기본값 사용: sentiment=보통, score=0, keywords=' '")
    return sentiment, score, keywords

# ===============================
# 종목명 매칭 함수 (STOCK_INFO에 있는 종목만)
# ===============================
def find_stock_code(conn, title, content):
    """
    제목과 본문에서 종목명을 찾아 STOCK_CODE 반환
    - STOCK_INFO에 있는 종목만 매칭
    - 제목 우선 검색
    - 본문에서도 검색
    - 부분 매칭 지원
    - ETF/ETN은 낮은 우선순위
    """
    if not title and not content:
        return None
    
    # 제목과 본문을 합친 텍스트
    search_text = ""
    if title:
        search_text += title + " "
    if content:
        search_text += content
    
    if not search_text:
        return None
    
    # 은행/금융기관 약어 매핑
    bank_aliases = {
        "농협": ["NH농협", "농협은행", "농협銀", "농협금융", "농협금융지주"],
        "KB금융": ["KB", "KB금융지주", "KB금융"],
        "신한지주": ["신한", "신한금융", "신한금융지주"],
        "하나금융": ["하나", "하나금융지주", "하나금융"],
        "우리금융": ["우리", "우리금융지주", "우리금융"],
        "새마을금고": ["새마을금고중앙회"],
    }
    
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT STOCK_NAME, STOCK_CODE FROM STOCK_INFO WHERE STOCK_NAME IS NOT NULL ORDER BY LENGTH(STOCK_NAME) DESC")
        stocks = cursor.fetchall()
        cursor.close()
        
        if not stocks:
            return None
        
        # 매칭된 종목들 (우선순위 점수, 종목명 길이, 종목명, 코드)
        matched_stocks = []
        
        for stock_name, stock_code in stocks:
            if not stock_name or not stock_code:
                continue
            
            stock_name_clean = stock_name.strip()
            if not stock_name_clean:
                continue
            
            # ETF/ETN 등은 낮은 우선순위
            is_etf_etn = "ETF" in stock_name_clean or "ETN" in stock_name_clean or "HANARO" in stock_name_clean
            
            # 1. 정확한 종목명 매칭 (최고 우선순위: 100점)
            if stock_name_clean in search_text:
                if title and stock_name_clean in title:
                    score = 100 if not is_etf_etn else 50
                    matched_stocks.append((score, len(stock_name_clean), stock_name_clean, stock_code))
                else:
                    score = 90 if not is_etf_etn else 40
                    matched_stocks.append((score, len(stock_name_clean), stock_name_clean, stock_code))
                continue
            
            # 2. 은행/금융기관 약어 매칭
            for main_name, aliases in bank_aliases.items():
                if stock_name_clean == main_name or stock_name_clean in aliases:
                    for alias in [main_name] + aliases:
                        if alias in search_text:
                            if title and alias in title:
                                matched_stocks.append((95, len(stock_name_clean), stock_name_clean, stock_code))
                            else:
                                matched_stocks.append((85, len(stock_name_clean), stock_name_clean, stock_code))
                            break
            
            # 3. 종목명의 주요 부분 매칭
            if len(stock_name_clean) >= 2:
                # 앞부분 2글자
                prefix_2 = stock_name_clean[:2]
                if prefix_2 in search_text and len(stock_name_clean) >= 3:
                    if not is_etf_etn:
                        if title and prefix_2 in title:
                            matched_stocks.append((70, len(stock_name_clean), stock_name_clean, stock_code))
                        else:
                            matched_stocks.append((60, len(stock_name_clean), stock_name_clean, stock_code))
                
                # 앞부분 3글자
                if len(stock_name_clean) >= 4:
                    prefix_3 = stock_name_clean[:3]
                    if prefix_3 in search_text:
                        if not is_etf_etn:
                            if title and prefix_3 in title:
                                matched_stocks.append((80, len(stock_name_clean), stock_name_clean, stock_code))
                            else:
                                matched_stocks.append((75, len(stock_name_clean), stock_name_clean, stock_code))
                
                # 뒷부분 매칭
                if len(stock_name_clean) >= 4:
                    suffix_2 = stock_name_clean[-2:]
                    if suffix_2 in search_text:
                        if not is_etf_etn:
                            if title and suffix_2 in title:
                                matched_stocks.append((65, len(stock_name_clean), stock_name_clean, stock_code))
                            else:
                                matched_stocks.append((55, len(stock_name_clean), stock_name_clean, stock_code))
                    
                    if len(stock_name_clean) >= 5:
                        suffix_3 = stock_name_clean[-3:]
                        if suffix_3 in search_text:
                            if not is_etf_etn:
                                if title and suffix_3 in title:
                                    matched_stocks.append((75, len(stock_name_clean), stock_name_clean, stock_code))
                                else:
                                    matched_stocks.append((70, len(stock_name_clean), stock_name_clean, stock_code))
                    
                    if len(stock_name_clean) >= 6:
                        suffix_4 = stock_name_clean[-4:]
                        if suffix_4 in search_text:
                            if not is_etf_etn:
                                if title and suffix_4 in title:
                                    matched_stocks.append((85, len(stock_name_clean), stock_name_clean, stock_code))
                                else:
                                    matched_stocks.append((80, len(stock_name_clean), stock_name_clean, stock_code))
                
                # 중간 부분 매칭 (단어 단위)
                if len(stock_name_clean) >= 5:
                    parts = re.findall(r'[가-힣]+|[A-Z]+|\d+', stock_name_clean)
                    for part in parts:
                        if len(part) >= 2 and part in search_text:
                            if not is_etf_etn:
                                if title and part in title:
                                    matched_stocks.append((60, len(stock_name_clean), stock_name_clean, stock_code))
                                else:
                                    matched_stocks.append((50, len(stock_name_clean), stock_name_clean, stock_code))
                            break
        
        if not matched_stocks:
            return None
        
        # 우선순위 점수와 종목명 길이로 정렬
        matched_stocks.sort(key=lambda x: (x[0], x[1]), reverse=True)
        
        # 중복 제거
        seen_codes = set()
        for score, length, name, code in matched_stocks:
            if code not in seen_codes:
                seen_codes.add(code)
                # 제목에서 매칭된 것이 우선
                if title and name in title:
                    return code
        
        # 제목에서 매칭되지 않았으면 본문에서 매칭된 것 중 최고 점수 (50점 이상만)
        for score, length, name, code in matched_stocks:
            if code not in seen_codes:
                if score >= 50:
                    return code
        
        return None
        
    except Exception as e:
        print(f"종목명 매칭 오류: {e}")
        return None

# ===============================
# 날짜 텍스트 파싱 (개선된 함수 - "입력 YYYY.MM.DD. 오후 HH:MM" 형식 지원)
# ===============================
def parse_news_date_from_text(date_text):
    """다양한 형식의 날짜 텍스트를 DATE 형식으로 변환 (개선)"""
    if not date_text:
        return None
    
    try:
        # 공백 제거
        date_text = date_text.strip()
        
        # "입력 2025.12.04. 오후 5:53" 형식 처리
        # "입력", "기사입력", "입력일시" 등의 키워드 제거
        date_text = re.sub(r'^(입력|기사입력|입력일시|작성일|발행일|게시일)\s*:?\s*', '', date_text, flags=re.IGNORECASE)
        date_text = date_text.strip()
        
        # "오후 5:53" 같은 시간 부분 제거
        date_text = re.sub(r'\s*오[전후]\s*\d{1,2}:\d{2}.*$', '', date_text)
        date_text = re.sub(r'\s*\d{1,2}:\d{2}.*$', '', date_text)  # 시간 형식 제거
        date_text = date_text.strip()
        
        # "2025.12.05" 또는 "2025. 12. 05" 또는 "2025.12.05." 형식
        match = re.search(r'(\d{4})\.\s*(\d{1,2})\.\s*(\d{1,2})\.?', date_text)
        if match:
            year, month, day = match.groups()
            return datetime(int(year), int(month), int(day)).date()
        
        # "2025-12-05" 형식
        match = re.search(r'(\d{4})-(\d{1,2})-(\d{1,2})', date_text)
        if match:
            year, month, day = match.groups()
            return datetime(int(year), int(month), int(day)).date()
        
        # "2025년 12월 05일" 형식
        match = re.search(r'(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일', date_text)
        if match:
            year, month, day = match.groups()
            return datetime(int(year), int(month), int(day)).date()
        
        # "12.05" 형식 (올해로 가정)
        match = re.search(r'^(\d{1,2})\.(\d{1,2})', date_text)
        if match:
            month, day = match.groups()
            year = datetime.now().year
            return datetime(year, int(month), int(day)).date()
        
        # ISO 형식 "2025-12-05T10:30:00" 또는 "2025-12-05 10:30:00"
        iso_match = re.search(r'(\d{4})-(\d{1,2})-(\d{1,2})[T\s]', date_text)
        if iso_match:
            year, month, day = iso_match.groups()
            return datetime(int(year), int(month), int(day)).date()
        
        # 숫자만 있는 경우 "20251205"
        if re.match(r'^\d{8}$', date_text):
            return datetime.strptime(date_text, "%Y%m%d").date()
        
    except Exception as e:
        print(f"    날짜 파싱 오류: {date_text} -> {e}")
        pass
    
    return None

# ===============================
# 뉴스 본문 + 날짜 크롤링 (개선된 버전 - 더 많은 방법 시도)
# ===============================
def fetch_news_content_and_date(news_url):
    """뉴스 상세 페이지에서 본문과 날짜 추출 (개선)"""
    try:
        if "/video/" in news_url:
            return None, None
        
        resp = requests.get(news_url, headers=HEADERS, timeout=15)
        resp.encoding = "EUC-KR"
        soup = BeautifulSoup(resp.text, "html.parser")
        
        if soup.select_one(".video_area, .video_player, #videoPlayer"):
            return None, None
        
        # 날짜 추출 (더 많은 방법 시도)
        news_date = None
        
        # 방법 1: article_info의 date 클래스 (가장 일반적)
        date_selectors = [
            ".article_info .date",
            ".article_info .date_time", 
            ".article_info .t11",
            ".article_info span.date",
            ".article_info ._article_date",
            "#articleInfo .date",
            "#articleInfo .date_time",
            ".media_end_head_info_datetime"
        ]
        
        for selector in date_selectors:
            date_elem = soup.select_one(selector)
            if date_elem:
                date_text = date_elem.get_text().strip()
                news_date = parse_news_date_from_text(date_text)
                if news_date:
                    print(f"    ✓ 방법1({selector})에서 날짜 추출: {news_date}")
                    break
        
        # 방법 2: 기사 본문 상단의 날짜
        if not news_date:
            date_selectors2 = [
                "#articleBodyContents .t11",
                ".article_body .t11",
                ".date",
                "span.t11",
                ".article_info_date",
                "._article_date",
                "#articleBodyContents .date"
            ]
            for selector in date_selectors2:
                date_elem = soup.select_one(selector)
                if date_elem:
                    date_text = date_elem.get_text().strip()
                    news_date = parse_news_date_from_text(date_text)
                    if news_date:
                        print(f"    ✓ 방법2({selector})에서 날짜 추출: {news_date}")
                        break
        
        # 방법 3: 메타 태그에서 날짜 추출
        if not news_date:
            meta_selectors = [
                'meta[property="article:published_time"]',
                'meta[name="publishdate"]',
                'meta[property="og:regDate"]',
                'meta[name="date"]',
                'meta[property="og:article:published_time"]',
                'meta[name="og:regDate"]'
            ]
            for meta_selector in meta_selectors:
                meta_date = soup.select_one(meta_selector)
                if meta_date:
                    date_attr = meta_date.get("content", "") or meta_date.get("value", "")
                    if date_attr:
                        news_date = parse_news_date_from_text(date_attr)
                        if news_date:
                            print(f"    ✓ 방법3(메타태그)에서 날짜 추출: {news_date}")
                            break
        
        # 방법 4: URL에서 날짜 추출 (네이버 뉴스 URL 형식: .../article/.../20251209123456)
        if not news_date:
            url_match = re.search(r'/(\d{8})/', news_url)
            if url_match:
                date_str = url_match.group(1)
                news_date = parse_news_date_from_text(date_str)
                if news_date:
                    print(f"    ✓ 방법4(URL)에서 날짜 추출: {news_date}")
        
        # 방법 5: 기사 헤더 영역의 날짜
        if not news_date:
            header_selectors = [
                ".article_header .date",
                ".article_header .date_time",
                ".article_header .t11",
                ".media_end_head_info_datetime",
                ".press_edit_info",
                "._article_date",
                ".article_header_datetime"
            ]
            for selector in header_selectors:
                header_date = soup.select_one(selector)
                if header_date:
                    date_text = header_date.get_text().strip()
                    news_date = parse_news_date_from_text(date_text)
                    if news_date:
                        print(f"    ✓ 방법5(헤더)에서 날짜 추출: {news_date}")
                        break
        
        # 방법 6: 기사 정보 영역 전체에서 날짜 패턴 찾기
        if not news_date:
            info_area = soup.select_one(".article_info, #articleInfo, .article_header, .media_end_head_info")
            if info_area:
                info_text = info_area.get_text()
                # 날짜 패턴 찾기 (입력 2025.12.04., 2025.12.04, 2025-12-04 등)
                date_patterns = [
                    r'입력\s*(\d{4})\.\s*(\d{1,2})\.\s*(\d{1,2})\.?',
                    r'(\d{4})\.\s*(\d{1,2})\.\s*(\d{1,2})\.?',
                    r'(\d{4})-(\d{1,2})-(\d{1,2})',
                    r'(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일'
                ]
                for pattern in date_patterns:
                    match = re.search(pattern, info_text)
                    if match:
                        if len(match.groups()) == 3:
                            year, month, day = match.groups()
                            try:
                                news_date = datetime(int(year), int(month), int(day)).date()
                                print(f"    ✓ 방법6(패턴매칭)에서 날짜 추출: {news_date}")
                                break
                            except:
                                pass
        
        # 방법 7: 페이지 전체에서 날짜 패턴 찾기 (마지막 시도)
        if not news_date:
            page_text = soup.get_text()
            # "입력 2025.12.04." 같은 패턴 찾기
            date_patterns = [
                r'입력\s*(\d{4})\.\s*(\d{1,2})\.\s*(\d{1,2})\.?',
                r'(\d{4})\.\s*(\d{1,2})\.\s*(\d{1,2})\.?',
            ]
            for pattern in date_patterns:
                match = re.search(pattern, page_text)
                if match:
                    if len(match.groups()) == 3:
                        year, month, day = match.groups()
                        try:
                            # 날짜가 합리적인 범위인지 확인 (2000년~2030년)
                            if 2000 <= int(year) <= 2030:
                                news_date = datetime(int(year), int(month), int(day)).date()
                                print(f"    ✓ 방법7(전체페이지 패턴매칭)에서 날짜 추출: {news_date}")
                                break
                        except:
                            pass
        
        if not news_date:
            print(f"    ⚠ 모든 방법으로 날짜 추출 실패: {news_url}")
        
        # 본문 추출
        content = ""
        selectors = [
            "#articleBodyContents",
            "#articleBody",
            ".article_body",
            "#newsEndContents",
            ".news_end_body",
            "#articeBody",
            ".article-body",
            "#article-view-content-div",
            "#articleBodyContents._article_body_contents"
        ]
        
        for selector in selectors:
            article_body = soup.select_one(selector)
            if article_body:
                for tag in article_body(["script", "style", "iframe", "a", "span"]):
                    if tag.name == "span" and "end_photo_org" in tag.get("class", []):
                        continue
                    tag.decompose()
                
                text = article_body.get_text(separator="\n", strip=True)
                text = clean(text)
                text = re.sub(r'\[.*?기자.*?\]', '', text)
                text = re.sub(r'\(.*?=\s*.*?\)', '', text)
                text = re.sub(r'Copyright.*?All rights reserved', '', text, flags=re.IGNORECASE)
                text = re.sub(r'무단.*?전재.*?금지', '', text, flags=re.IGNORECASE)
                
                if text and len(text) > 100:
                    content = text
                    break
        
        if not content:
            paragraphs = soup.select("#articleBodyContents p, .article_body p, #articleBody p")
            if paragraphs:
                texts = []
                for p in paragraphs:
                    for script in p(["script", "style"]):
                        script.decompose()
                    text = clean(p.get_text())
                    if text and len(text) > 20:
                        if "동영상" not in text and "영상" not in text[:30]:
                            texts.append(text)
                if texts and len(" ".join(texts)) > 100:
                    content = " ".join(texts)
        
        if not content:
            divs = soup.select("div#articleBodyContents, div.article_body")
            for div in divs:
                for tag in div(["script", "style", "iframe", "a"]):
                    tag.decompose()
                text = clean(div.get_text())
                if text and len(text) > 100:
                    content = text
                    break
        
        return content if content and len(content) > 50 else None, news_date
        
    except Exception as e:
        print(f"본문/날짜 크롤링 오류: {e}")
        return None, None

# ===============================
# 뉴스 날짜 파싱 (기존 함수 - 리스트 페이지용)
# ===============================
def parse_news_date(date_str):
    """뉴스 날짜 문자열을 DATE 형식으로 변환 (실패 시 None 반환)"""
    if not date_str:
        return None
    
    # parse_news_date_from_text 사용
    parsed = parse_news_date_from_text(date_str)
    return parsed  # 실패하면 None 반환

# ===============================
# 뉴스 리스트 페이지 파싱
# ===============================
def parse_news_list(conn, sid1=101, page=1, date_str=None):
    """네이버 뉴스에서 뉴스 리스트 파싱"""
    try:
        if not date_str:
            date_str = datetime.now().strftime("%Y%m%d")
        
        params = {
            "mode": "LSD",
            "mid": "sec",
            "sid1": str(sid1),
            "date": date_str,
            "page": page
        }
        
        resp = requests.get(BASE_URL, params=params, headers=HEADERS, timeout=15)
        resp.encoding = "EUC-KR"
        soup = BeautifulSoup(resp.text, "html.parser")
        news_list = []
        
        articles = soup.select("ul.type06_headline li, ul.type06 li, .list_body li, .type06_headline li, .type06 li")
        
        if not articles:
            return []
        
        skipped_video = 0
        
        for article in articles:
            try:
                title_elem = article.select_one("dt a, dt.photo a, .lede a, a.tit, dt a.tlt, a.tlt")
                if not title_elem:
                    continue
                
                title = clean(title_elem.get_text())
                news_url = title_elem.get("href", "")
                
                if not title or not news_url:
                    continue
                
                if title == "동영상기사" or "동영상" in title[:10]:
                    skipped_video += 1
                    continue
                
                if news_url.startswith("//"):
                    news_url = "https:" + news_url
                elif news_url.startswith("/"):
                    news_url = "https://news.naver.com" + news_url
                
                if "/video/" in news_url:
                    skipped_video += 1
                    continue
                
                # 리스트 페이지에서 날짜 추출 (백업용)
                date_elem = article.select_one(".date, .writing, .info, .date_time")
                date_str_article = date_elem.get_text().strip() if date_elem else ""
                news_date_backup = parse_news_date(date_str_article)
                
                print(f"  본문 크롤링 중: {title[:50]}...")
                content, news_date_from_content = fetch_news_content_and_date(news_url)
                
                # 본문에서 날짜를 가져왔으면 그것을 사용, 없으면 리스트 페이지 날짜 사용, 둘 다 없으면 오늘 날짜 사용 (NULL 방지)
                if news_date_from_content:
                    news_date = news_date_from_content
                    print(f"    ✓ 본문에서 날짜 추출 성공: {news_date}")
                elif news_date_backup:
                    news_date = news_date_backup
                    print(f"    ⚠ 본문에서 날짜 추출 실패, 리스트 페이지 날짜 사용: {news_date}")
                else:
                    # ★ NULL 방지: 날짜가 없으면 오늘 날짜 사용
                    news_date = datetime.now().date()
                    print(f"    ⚠ 날짜 추출 실패, 오늘 날짜 사용: {news_date}")
                
                # ★ CONTENT가 없거나 짧으면 제목 기반으로 생성 (NULL 방지)
                if not content or len(content.strip()) < 50:
                    content = title + " " + (title_elem.get("title", "") or "")
                    if not content or len(content.strip()) < 10:
                        content = title  # 최소한 제목이라도
                    print(f"    ⚠ 본문 부족, 제목 기반 본문 사용")
                else:
                    print(f"    ✓ 본문 {len(content)}자 추출 완료")
                
                # ★ TITLE이 없으면 스킵 (NULL 방지)
                if not title or not title.strip():
                    print(f"    ⊙ 제목 없음, 스킵")
                    continue
                
                # ★ STOCK_INFO에 있는 종목만 매칭
                stock_code = find_stock_code(conn, title, content)
                
                # ★★ 중요: STOCK_CODE가 없으면 이 뉴스는 스킵 (저장 안 함)
                if not stock_code:
                    print(f"    ⊙ 종목명 없음, 스킵: {title[:50]}...")
                    continue
                
                # ★★★ 감성 분석 강제 수행 (NULL 방지 강화 - 반드시 값 보장)
                sentiment, score, keywords = ensure_sentiment_analysis(title, content)
                print(f"    ✓ 감성 분석 완료: {sentiment} (점수: {score}), 키워드: {keywords[:50] if keywords and keywords != ' ' else '없음'}")
                
                # ★ 최종 NULL 체크 (모든 필수 필드 확인)
                if not stock_code or not title or not content or not news_url or not news_date:
                    print(f"    ⊙ 필수 필드 누락, 스킵: stock_code={stock_code}, title={bool(title)}, content={bool(content)}, url={bool(news_url)}, date={bool(news_date)}")
                    continue
                
                # 감성 분석 필드도 NULL 체크
                if not sentiment or sentiment not in ['긍정', '부정', '보통']:
                    sentiment = "보통"
                if score is None:
                    score = 0
                if not keywords or keywords.strip() == '':
                    keywords = " "
                
                news_item = {
                    "stock_code": stock_code,
                    "title": title,
                    "content": content,
                    "url": news_url,
                    "news_date": news_date,
                    "sentiment": sentiment,  # 항상 값 있음 (기본값 포함)
                    "score": score,  # 항상 값 있음 (기본값 포함)
                    "keywords": keywords  # 항상 값 있음 (기본값 포함)
                }
                
                news_list.append(news_item)
                time.sleep(0.05)  # 0.1초 → 0.05초로 단축 (속도 개선)
                
            except Exception as e:
                print(f"    ✗ 뉴스 처리 오류: {e}")
                continue
        
        if skipped_video > 0:
            print(f"  [제외] 동영상 기사: {skipped_video}개")
        
        return news_list
        
    except Exception as e:
        print(f"뉴스 리스트 파싱 오류: {e}")
        return []

# ===============================
# Oracle DB에 뉴스 INSERT (STOCK_CODE가 있는 뉴스만 + 감성 분석 포함, NULL 방지 강화)
# ===============================
def insert_news(conn, news_item):
    """신규 뉴스를 Oracle DB에 INSERT (STOCK_CODE가 있는 뉴스만 + 감성 분석 포함, NULL 절대 방지)"""
    cursor = None
    try:
        # ★ 필수 필드 NULL 체크 및 기본값 설정 (강화)
        
        # 1. STOCK_CODE 체크 (없으면 저장하지 않음)
        stock_code_val = news_item.get('stock_code')
        if not stock_code_val or not str(stock_code_val).strip():
            return False, False
        stock_code_val = str(stock_code_val).strip()[:10]
        
        # 2. TITLE 체크 (없으면 저장하지 않음)
        title_val = news_item.get('title')
        if not title_val or not str(title_val).strip():
            return False, False
        title_val = str(title_val).strip()[:490]
        if not title_val:
            return False, False
        
        # 3. URL 체크 (없으면 저장하지 않음)
        url_val = news_item.get('url')
        if not url_val or not str(url_val).strip():
            return False, False
        url_val = str(url_val).strip()[:990]
        if not url_val:
            return False, False
        
        # 4. CONTENT 체크 (없거나 짧으면 제목 사용)
        content = news_item.get('content')
        if not content:
            content = title_val
        else:
            content = str(content).strip()
            if len(content) < 10:
                content = title_val
        
        content_val = content[:3900] if content else title_val
        if not content_val:
            content_val = title_val
        
        # 5. NEWS_DATE 체크 (없으면 오늘 날짜 사용)
        news_date = news_item.get('news_date')
        try:
            if not news_date:
                news_date = datetime.now().date()
            elif isinstance(news_date, str):
                # 문자열인 경우 파싱 시도
                parsed_date = parse_news_date_from_text(news_date)
                news_date = parsed_date if parsed_date else datetime.now().date()
            else:
                # date 객체인지 확인 (datetime.date 타입과 직접 비교)
                try:
                    # datetime.now().date()의 타입과 비교
                    if type(news_date) == type(datetime.now().date()):
                        # 이미 date 객체이므로 그대로 사용
                        pass
                    else:
                        # date 객체가 아니면 오늘 날짜 사용
                        news_date = datetime.now().date()
                except:
                    # 타입 체크 실패 시 오늘 날짜 사용
                    news_date = datetime.now().date()
        except Exception as e:
            # 날짜 처리 중 에러 발생 시 오늘 날짜 사용
            print(f"    ⚠ 날짜 처리 오류: {e}, 오늘 날짜 사용")
            news_date = datetime.now().date()
        
        # ★★★ 감성 분석 결과 가져오기 (NULL 방지 강화 - 반드시 값 보장)
        sentiment_val = news_item.get('sentiment')
        # 빈 문자열, None, 'None' 문자열 모두 체크
        if not sentiment_val or sentiment_val == 'None' or sentiment_val is None or str(sentiment_val).strip() == '':
            sentiment_val = '보통'
        sentiment_val = str(sentiment_val).strip()  # 문자열로 확실히 변환
        # 유효한 값인지 확인
        if sentiment_val not in ['긍정', '부정', '보통']:
            sentiment_val = '보통'
        
        score_val = news_item.get('score')
        # None이거나 숫자가 아니면 0으로 설정
        if score_val is None:
            score_val = 0
        try:
            score_val = int(score_val)  # 정수로 변환
        except (ValueError, TypeError):
            score_val = 0
        
        keywords_val = news_item.get('keywords')
        # None이거나 빈 문자열이면 최소한 공백이라도 넣기 (NULL 방지)
        # Oracle은 빈 문자열('')을 NULL로 처리하므로 공백 문자(' ') 사용
        if not keywords_val or keywords_val == 'None' or keywords_val is None or str(keywords_val).strip() == '':
            keywords_val = ' '  # 공백 문자 (Oracle NULL 방지)
        else:
            keywords_val = str(keywords_val).strip()[:990]  # 길이 제한
            if not keywords_val:  # strip 후 빈 문자열이면 공백으로
                keywords_val = ' '
        
        # STOCK_CODE가 STOCK_INFO에 존재하는지 확인
        check_cursor = conn.cursor()
        check_cursor.execute("SELECT COUNT(*) FROM STOCK_INFO WHERE STOCK_CODE = :code", {'code': stock_code_val})
        if check_cursor.fetchone()[0] == 0:
            check_cursor.close()
            return False, False  # STOCK_INFO에 없으면 저장 안 함
        check_cursor.close()
        
        # 중복 체크 (URL 기준)
        check_cursor = conn.cursor()
        check_cursor.execute("SELECT COUNT(*) FROM STOCK_NEWS WHERE URL = :url", {'url': url_val})
        is_duplicate = check_cursor.fetchone()[0] > 0
        check_cursor.close()
        
        if is_duplicate:
            return True, False  # 중복이므로 스킵
        
        # ★ 최종 NULL 체크 (모든 필드가 확실히 값이 있는지 확인)
        if not stock_code_val or not title_val or not content_val or not url_val or not news_date:
            print(f"  ⚠ NULL 값 감지 - 저장 스킵: stock_code={stock_code_val}, title={title_val[:30]}, url={url_val[:50]}")
            return False, False
        
        # 감성 분석 필드 최종 검증
        if not sentiment_val or sentiment_val not in ['긍정', '부정', '보통']:
            sentiment_val = '보통'
        if score_val is None:
            score_val = 0
        if not keywords_val or keywords_val.strip() == '':
            keywords_val = ' '
        
        cursor = conn.cursor()
        conn.autocommit = False
        
        # ★★★ INSERT SQL에 SENTIMENT, SCORE, KEYWORDS 컬럼 추가!
        sql = """
            INSERT INTO STOCK_NEWS (
                STOCK_CODE, TITLE, CONTENT, URL, NEWS_DATE, CREATED_AT,
                SENTIMENT, SCORE, KEYWORDS
            ) VALUES (
                :stock_code, :title, :content, :url, :news_date, SYSDATE,
                :sentiment, :score, :keywords
            )
        """
        
        # ★★★ 최종 값 확인 및 디버깅 출력
        print(f"    [DEBUG] INSERT 값 확인: sentiment={sentiment_val}, score={score_val}, keywords={keywords_val[:30] if keywords_val and keywords_val != ' ' else '(empty)'}")
        
        cursor.execute(sql, {
            'stock_code': stock_code_val,
            'title': title_val,
            'content': content_val,
            'url': url_val,
            'news_date': news_date,
            'sentiment': sentiment_val,
            'score': score_val,
            'keywords': keywords_val if keywords_val else ' '  # 공백 문자 (Oracle NULL 방지)
        })
        
        conn.commit()
        
        verify_sql = "SELECT NEWS_ID FROM STOCK_NEWS WHERE URL = :url AND ROWNUM = 1"
        cursor.execute(verify_sql, {'url': url_val})
        verify_result = cursor.fetchone()
        
        cursor.close()
        
        is_inserted = verify_result is not None
        return True, is_inserted
            
    except Exception as e:
        if cursor:
            try:
                conn.rollback()
                cursor.close()
            except:
                pass
        print(f"    ✗ INSERT 오류: {e}")
        import traceback
        traceback.print_exc()
        return False, False

# ===============================
# DB 저장 확인 함수
# ===============================
def verify_db_count(conn):
    """DB에 저장된 뉴스 개수 확인"""
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM STOCK_NEWS")
        count = cursor.fetchone()[0]
        cursor.close()
        return count
    except:
        return 0

# ===============================
# 메인 함수 (STOCK_CODE가 있는 뉴스만 저장)
# ===============================
def main():
    # ★★★ 스케줄러 모드 확인
    is_scheduler_mode = "--scheduler" in sys.argv
    force_run = "--force" in sys.argv or "-f" in sys.argv  # 강제 실행 옵션 (1시간 체크 무시)
    
    # ★★★ 스케줄러 모드일 때 실행 간격 체크 (1시간마다만 실행)
    if is_scheduler_mode and not force_run:
        # 마지막 실행 시간 저장 파일
        last_run_file = Path(__file__).parent / "last_crawler_run.json"
        
        current_time = datetime.now()
        
        # 마지막 실행 시간 확인
        if last_run_file.exists():
            try:
                with open(last_run_file, 'r', encoding='utf-8') as f:
                    last_run_data = json.load(f)
                    last_run_str = last_run_data.get('last_run_time')
                    if last_run_str:
                        last_run_time = datetime.fromisoformat(last_run_str)
                        time_diff = (current_time - last_run_time).total_seconds()
                        
                        # 1시간(3600초)이 지나지 않았으면 종료
                        if time_diff < 3600:
                            remaining_minutes = int((3600 - time_diff) / 60)
                            remaining_seconds = int(3600 - time_diff) % 60
                            print("=" * 60)
                            print(f"⚠ 실행 간격 체크: 마지막 실행 후 {int(time_diff/60)}분 {int(time_diff%60)}초 경과")
                            print(f"   다음 실행까지 약 {remaining_minutes}분 {remaining_seconds}초 남았습니다.")
                            print(f"   정확히 1시간마다만 실행되도록 설정되어 있습니다.")
                            print("=" * 60)
                            return
            except Exception as e:
                print(f"⚠ 마지막 실행 시간 확인 오류: {e}")
                print("   계속 진행합니다...")
        
        # 현재 실행 시간 저장
        try:
            with open(last_run_file, 'w', encoding='utf-8') as f:
                json.dump({
                    'last_run_time': current_time.isoformat(),
                    'last_run_timestamp': current_time.timestamp()
                }, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"⚠ 실행 시간 저장 오류: {e}")
    
    conn = None
    try:
        try:
            oracledb.init_oracle_client()
        except:
            pass
        
        conn = oracledb.connect(
            user=DB_USER,
            password=DB_PASSWORD,
            dsn=DB_DSN
        )
        
        conn.autocommit = False
        
        print("Oracle DB 연결 성공!\n")
        
        before_count = verify_db_count(conn)
        print(f"시작 전 DB 뉴스 개수: {before_count}개\n")
        
        # 크롤링 시작 시간 기록 (새로 추가된 뉴스 식별용)
        crawl_start_time = datetime.now()
        
        if is_scheduler_mode:
            print("=" * 60)
            print("스케줄러 모드: 새로운 뉴스만 추가 (중복 체크)")
            print("=" * 60)
            print("※ STOCK_INFO에 있는 종목만 매칭")
            print("※ STOCK_CODE가 없는 뉴스는 스킵")
            print("※ 날짜가 없으면 오늘 날짜 사용 (NULL 방지)")
            print("※ 감성 분석 및 키워드 추출 포함 (NULL 방지 강화)")
            print("※ 중복이 연속으로 많이 나오면 종료")
            print("※ 실행 주기: 정확히 1시간마다\n")
        else:
            print("=" * 60)
            print("뉴스 크롤링 시작 (목표: STOCK_CODE가 있는 뉴스 100개 추가)")
            print("=" * 60)
            print("※ STOCK_INFO에 있는 종목만 매칭")
            print("※ STOCK_CODE가 없는 뉴스는 스킵하고 다음 뉴스로")
            print("※ 무조건 100개 추가될 때까지 크롤링")
            print("※ 날짜가 없으면 오늘 날짜 사용 (NULL 방지)")
            print("※ 감성 분석 및 키워드 추출 포함 (NULL 방지 강화)")
            print("※ 본문 페이지에서 정확한 날짜 추출\n")
        
        total_attempted = 0  # 시도한 개수
        total_inserted = 0   # 실제 INSERT된 개수
        total_duplicated = 0  # 중복 개수
        total_skipped = 0    # 스킵된 개수 (종목명 없음)
        target_count = 100 if not is_scheduler_mode else None  # 스케줄러 모드에서는 목표 없음
        
        # 카테고리 설정
        categories = [
            (101, "경제 일반"),
            (258, "증권"),
        ]
        
        # 최근 날짜 크롤링 (스케줄러 모드: 14일, 일반 모드: 7일)
        dates_to_crawl = []
        date_range = 7  # 스케줄러 모드와 일반 모드 모두 7일로 통일 (속도 개선)
        for i in range(date_range):
            date = datetime.now() - timedelta(days=i)
            dates_to_crawl.append(date.strftime("%Y%m%d"))
        
        if is_scheduler_mode:
            print(f"크롤링 대상: {len(categories)}개 카테고리 × {len(dates_to_crawl)}일")
            print(f"모드: 스케줄러 모드 (새로운 뉴스만 추가)\n")
        else:
            print(f"크롤링 대상: {len(categories)}개 카테고리 × {len(dates_to_crawl)}일")
            print(f"목표: STOCK_CODE가 있는 뉴스 {target_count}개 추가\n")
        
        start_time = datetime.now()
        max_pages_per_date = 30 if is_scheduler_mode else 50  # 스케줄러 모드: 30페이지로 제한 (속도 개선)
        
        # 스케줄러 모드: 연속 중복 체크 (매우 완화된 조건)
        max_attempts = 500 if is_scheduler_mode else None  # 200 → 500으로 증가 (더 많은 시도 허용)
        max_consecutive_duplicates = 200 if is_scheduler_mode else None  # 100 → 200으로 증가 (더 많은 연속 중복 허용)
        consecutive_duplicates = 0
        attempts = 0
        
        # 무한 루프로 100개가 추가될 때까지 계속 크롤링 (일반 모드)
        # 또는 새로운 뉴스를 찾을 때까지 크롤링 (스케줄러 모드)
        while True:
            # 스케줄러 모드: 최대 시도 횟수 체크
            if is_scheduler_mode:
                attempts += 1
                if max_attempts and attempts > max_attempts:
                    print(f"\n⚠ 최대 시도 횟수({max_attempts}) 도달, 종료")
                    break
                
                if max_consecutive_duplicates and consecutive_duplicates >= max_consecutive_duplicates:
                    print(f"\n⚠ 연속 중복 {consecutive_duplicates}개 발생, 종료")
                    break
            
            # 일반 모드: 실제 DB 증가 개수 확인
            if not is_scheduler_mode:
                current_db_count = verify_db_count(conn)
                actual_increase = current_db_count - before_count
                
                if actual_increase >= target_count:
                    print(f"\n✓ 목표 달성! 실제 DB 증가: {actual_increase}개 (목표: {target_count}개)")
                    break
                
                print(f"\n{'='*60}")
                print(f"현재 실제 DB 증가: {actual_increase}개 / 목표: {target_count}개")
                print(f"{'='*60}")
            
            found_new_news = False
            
            for sid1, category_name in categories:
                # 일반 모드: 실제 DB 증가 개수 확인
                if not is_scheduler_mode:
                    current_db_count = verify_db_count(conn)
                    actual_increase = current_db_count - before_count
                    
                    if actual_increase >= target_count:
                        break
                
                print(f"\n카테고리: {category_name} (sid1={sid1})")
                
                for date_idx, date_str in enumerate(dates_to_crawl, 1):
                    # 일반 모드: 실제 DB 증가 개수 확인
                    if not is_scheduler_mode:
                        current_db_count = verify_db_count(conn)
                        actual_increase = current_db_count - before_count
                        
                        if actual_increase >= target_count:
                            break
                        
                        print(f"\n[{date_idx}/{len(dates_to_crawl)}] 날짜: {date_str} (실제 DB 증가: {actual_increase}/{target_count}개)")
                    else:
                        print(f"\n[{date_idx}/{len(dates_to_crawl)}] 날짜: {date_str}")
                    
                    for page in range(1, max_pages_per_date + 1):
                        # 일반 모드: 실제 DB 증가 개수 확인
                        if not is_scheduler_mode:
                            current_db_count = verify_db_count(conn)
                            actual_increase = current_db_count - before_count
                            
                            if actual_increase >= target_count:
                                break
                            
                            print(f"\n▶ {page}페이지 크롤링 중... (실제 DB 증가: {actual_increase}/{target_count}개)")
                        else:
                            print(f"\n▶ {page}페이지 크롤링 중...")
                        
                        news_list = parse_news_list(conn, sid1=sid1, page=page, date_str=date_str)
                        
                        if not news_list:
                            # 빈 페이지여도 계속 진행
                            print(f"  → 빈 페이지, 계속 진행...")
                            continue
                        
                        print(f"  → {len(news_list)}개 뉴스 발견 (모두 STOCK_CODE 있음)")
                        
                        for news in news_list:
                            # 일반 모드: 실제 DB 증가 개수 확인
                            if not is_scheduler_mode:
                                current_db_count = verify_db_count(conn)
                                actual_increase = current_db_count - before_count
                                
                                if actual_increase >= target_count:
                                    break
                            
                            total_attempted += 1
                            success, is_inserted = insert_news(conn, news)
                            
                            if success:
                                if is_inserted:
                                    total_inserted += 1
                                    found_new_news = True
                                    consecutive_duplicates = 0  # 중복 카운터 리셋
                                    stock_info = f" [{news['stock_code']}]"
                                    sentiment_info = f" [{news.get('sentiment', '보통')}]"
                                    if is_scheduler_mode:
                                        print(f"  ✓ 저장 ({total_inserted}개): {news['title'][:40]}...{stock_info}{sentiment_info}")
                                    else:
                                        print(f"  ✓ 저장 ({total_inserted}개, 실제 DB 증가: {actual_increase + 1}/{target_count}): {news['title'][:40]}...{stock_info}{sentiment_info}")
                                else:
                                    total_duplicated += 1
                                    consecutive_duplicates += 1  # 연속 중복 카운터 증가
                                    if total_duplicated % 10 == 0:
                                        print(f"  ⊙ 중복 (시도: {total_attempted}개, 중복: {total_duplicated}개, 실제 추가: {total_inserted}개)")
                            else:
                                total_skipped += 1
                                print(f"  ✗ 실패: {news['title'][:45]}...")
                        
                        # 스케줄러 모드: 연속 중복 체크
                        if is_scheduler_mode and consecutive_duplicates >= max_consecutive_duplicates:
                            print(f"\n⚠ 연속 중복 {consecutive_duplicates}개 발생, 종료")
                            break
                        
                        time.sleep(0.05)  # 0.1초 → 0.05초로 단축 (속도 개선)
                    
                    if not is_scheduler_mode:
                        if actual_increase >= target_count:
                            break
                    elif consecutive_duplicates >= max_consecutive_duplicates:
                        break
                
                if not is_scheduler_mode:
                    if actual_increase >= target_count:
                        break
                elif consecutive_duplicates >= max_consecutive_duplicates:
                    break
            
            # 일반 모드: 실제 DB 증가 개수 확인
            if not is_scheduler_mode:
                current_db_count = verify_db_count(conn)
                actual_increase = current_db_count - before_count
                
                if actual_increase >= target_count:
                    break
            
            # 스케줄러 모드: 연속 중복 체크
            if is_scheduler_mode:
                if consecutive_duplicates >= max_consecutive_duplicates:
                    break
            
            # 새 뉴스를 찾지 못했으면 날짜 범위 확장 (일반 모드만)
            if not is_scheduler_mode and not found_new_news:
                print(f"\n⚠ 새 뉴스를 찾지 못했습니다. 날짜 범위를 확장합니다...")
                dates_to_crawl = []
                for i in range(14):
                    date = datetime.now() - timedelta(days=i)
                    dates_to_crawl.append(date.strftime("%Y%m%d"))
                print(f"날짜 범위 확장: 최근 14일")
                time.sleep(1)
        
        end_time = datetime.now()
        elapsed_time = (end_time - start_time).total_seconds()
        
        after_count = verify_db_count(conn)
        print("\n" + "=" * 60)
        print("크롤링 완료!")
        print("=" * 60)
        print(f"시도한 개수: {total_attempted}개")
        print(f"실제 INSERT된 개수: {total_inserted}개")
        print(f"중복 개수: {total_duplicated}개")
        print(f"스킵된 개수: {total_skipped}개")
        print(f"시작 전 DB 개수: {before_count}개")
        print(f"종료 후 DB 개수: {after_count}개")
        print(f"실제 DB 증가: {after_count - before_count}개")
        print(f"소요 시간: {elapsed_time/60:.1f}분 ({elapsed_time:.0f}초)")
        print("=" * 60)
        
        if not is_scheduler_mode:
            if after_count - before_count >= target_count:
                print(f"\n✓ 목표 달성! 실제 DB 증가 {after_count - before_count}개 (목표: {target_count}개)")
            else:
                print(f"\n⚠ 목표 미달성: 실제 DB 증가 {after_count - before_count}개 (목표: {target_count}개)")
        else:
            print(f"\n✓ 스케줄러 모드 완료: {after_count - before_count}개 추가됨")
        
        # ★★★ 크롤링 완료 후 새로 추가된 뉴스 중 NULL 값만 감성 분석
        if after_count > before_count:
            print("\n" + "=" * 60)
            print("새로 추가된 뉴스 감성 분석 확인 중...")
            print("=" * 60)
            try:
                # 방금 추가된 뉴스 중 NULL 값이 있는 것만 조회
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT NEWS_ID, TITLE, CONTENT 
                    FROM STOCK_NEWS 
                    WHERE CREATED_AT >= :start_time
                      AND (SENTIMENT IS NULL OR SCORE IS NULL OR KEYWORDS IS NULL)
                    ORDER BY CREATED_AT ASC
                """, {'start_time': crawl_start_time})
                
                null_news_list = cursor.fetchall()
                cursor.close()
                
                if null_news_list and len(null_news_list) > 0:
                    print(f"새로 추가된 뉴스 중 NULL 값 발견: {len(null_news_list)}개")
                    print("감성 분석을 수행합니다...\n")
                    
                    # 감성 분석기 import
                    try:
                        from news_sentiment_analyzer import SentimentAnalyzer
                        sentiment_analyzer = SentimentAnalyzer(use_morphology=True)
                        
                        success_count = 0
                        fail_count = 0
                        
                        for news_id, title, content in null_news_list:
                            try:
                                # CLOB 처리
                                if content and hasattr(content, 'read'):
                                    content = content.read()
                                if not isinstance(content, str):
                                    content = str(content) if content else ""
                                
                                # 감성 분석 수행
                                sentiment, score, keywords = sentiment_analyzer.analyze_sentiment(
                                    title if title else "",
                                    content
                                )
                                
                                # NULL 방지: 기본값 설정
                                if not sentiment or sentiment not in ['긍정', '부정', '보통']:
                                    sentiment = '보통'
                                if score is None:
                                    score = 0
                                if not keywords or keywords.strip() == '':
                                    keywords = ' '
                                
                                # DB 업데이트
                                update_cursor = conn.cursor()
                                update_cursor.execute("""
                                    UPDATE STOCK_NEWS
                                    SET SENTIMENT = :sentiment,
                                        SCORE = :score,
                                        KEYWORDS = :keywords,
                                        UPDATED_AT = SYSDATE
                                    WHERE NEWS_ID = :news_id
                                """, {
                                    'sentiment': sentiment,
                                    'score': score,
                                    'keywords': keywords,
                                    'news_id': news_id
                                })
                                conn.commit()
                                update_cursor.close()
                                
                                success_count += 1
                                print(f"  ✓ 뉴스 ID {news_id}: {sentiment} (점수: {score})")
                                
                            except Exception as e:
                                fail_count += 1
                                print(f"  ✗ 뉴스 ID {news_id} 실패: {e}")
                                # 실패해도 기본값으로 업데이트
                                try:
                                    update_cursor = conn.cursor()
                                    update_cursor.execute("""
                                        UPDATE STOCK_NEWS
                                        SET SENTIMENT = '보통',
                                            SCORE = 0,
                                            KEYWORDS = ' ',
                                            UPDATED_AT = SYSDATE
                                        WHERE NEWS_ID = :news_id
                                    """, {'news_id': news_id})
                                    conn.commit()
                                    update_cursor.close()
                                    print(f"  ⚠ 기본값으로 업데이트 완료")
                                    success_count += 1
                                    fail_count -= 1
                                except:
                                    pass
                        
                        print(f"\n✓ 새로 추가된 뉴스 감성 분석 완료: 성공 {success_count}개, 실패 {fail_count}개")
                        
                    except Exception as e:
                        print(f"⚠ 감성 분석기 로드 실패: {e}")
                        print("   기본값으로 업데이트합니다...")
                        # 기본값으로 업데이트
                        update_cursor = conn.cursor()
                        update_cursor.execute("""
                            UPDATE STOCK_NEWS
                            SET SENTIMENT = '보통',
                                SCORE = 0,
                                KEYWORDS = ' ',
                                UPDATED_AT = SYSDATE
                            WHERE CREATED_AT >= :start_time
                              AND (SENTIMENT IS NULL OR SCORE IS NULL OR KEYWORDS IS NULL)
                        """, {'start_time': crawl_start_time})
                        conn.commit()
                        update_cursor.close()
                        print(f"✓ 기본값으로 업데이트 완료: {update_cursor.rowcount}개")
                else:
                    print("새로 추가된 뉴스에 NULL 값이 없습니다. (이미 감성 분석 완료)")
                    
            except Exception as e:
                print(f"⚠ 새로 추가된 뉴스 감성 분석 실패: {e}")
                import traceback
                traceback.print_exc()
        
    except Exception as e:
        print(f"에러 발생: {e}")
        import traceback
        traceback.print_exc()
    finally:
        if conn:
            conn.close()
            print("\nDB 연결 종료")

if __name__ == "__main__":
    main()
