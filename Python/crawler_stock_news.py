# crawler_stock_news.py - 실제 DB 증가 200개 보장 (빈 페이지 체크 없음)
import requests
from bs4 import BeautifulSoup
from datetime import datetime, timedelta
import time
import re
import oracledb

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
# 뉴스 본문 크롤링
# ===============================
def fetch_news_content(news_url):
    """뉴스 상세 페이지에서 본문 추출"""
    try:
        if "/video/" in news_url:
            return None
        
        resp = requests.get(news_url, headers=HEADERS, timeout=15)
        resp.encoding = "EUC-KR"
        soup = BeautifulSoup(resp.text, "html.parser")
        
        if soup.select_one(".video_area, .video_player, #videoPlayer"):
            return None
        
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
        
        return content if content and len(content) > 50 else None
        
    except Exception as e:
        return None

# ===============================
# 뉴스 날짜 파싱
# ===============================
def parse_news_date(date_str):
    """뉴스 날짜 문자열을 DATE 형식으로 변환"""
    try:
        if not date_str:
            return datetime.now().date()
        if "." in date_str:
            date_str = date_str.replace(".", "-")
        if len(date_str) >= 10:
            date_str = date_str[:10]
            return datetime.strptime(date_str, "%Y-%m-%d").date()
    except:
        pass
    return datetime.now().date()

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
                
                date_elem = article.select_one(".date, .writing, .info, .date_time")
                date_str_article = date_elem.get_text().strip() if date_elem else ""
                news_date = parse_news_date(date_str_article)
                
                print(f"  본문 크롤링 중: {title[:50]}...")
                content = fetch_news_content(news_url)
                
                if not content or len(content.strip()) < 50:
                    content = title + " " + (title_elem.get("title", "") or "")
                    print(f"    ⚠ 본문 부족, 제목 기반 본문 사용")
                else:
                    print(f"    ✓ 본문 {len(content)}자 추출 완료")
                
                stock_code = None
                cursor = conn.cursor()
                cursor.execute("SELECT STOCK_NAME, STOCK_CODE FROM STOCK_INFO WHERE STOCK_NAME IS NOT NULL")
                stocks = cursor.fetchall()
                cursor.close()
                
                for name, code in stocks:
                    if name and name in title:
                        stock_code = code
                        break
                
                news_item = {
                    "stock_code": stock_code,
                    "title": title,
                    "content": content,
                    "url": news_url,
                    "news_date": news_date
                }
                
                news_list.append(news_item)
                time.sleep(0.1)
                
            except Exception as e:
                continue
        
        if skipped_video > 0:
            print(f"  [제외] 동영상 기사: {skipped_video}개")
        
        return news_list
        
    except Exception as e:
        return []

# ===============================
# Oracle DB에 뉴스 INSERT (실제 INSERT 여부 반환)
# ===============================
def insert_news(conn, news_item):
    """신규 뉴스를 Oracle DB에 INSERT (실제 INSERT 여부 반환)"""
    cursor = None
    try:
        content = news_item['content']
        if not content or len(content.strip()) < 10:
            content = news_item['title']
        
        if not news_item['title']:
            news_item['title'] = "제목 없음"
        if not content:
            content = news_item['title']
        if not news_item['url']:
            return False, False  # (성공 여부, 실제 INSERT 여부)
        
        title_val = (news_item['title'][:490] if news_item['title'] else "제목 없음")
        url_val = news_item['url'][:990] if news_item['url'] else ""
        content_val = content[:3900] if content else title_val
        
        stock_code_val = news_item['stock_code']
        if stock_code_val:
            check_cursor = conn.cursor()
            check_cursor.execute("SELECT COUNT(*) FROM STOCK_INFO WHERE STOCK_CODE = :code", {'code': stock_code_val})
            if check_cursor.fetchone()[0] == 0:
                stock_code_val = None
            check_cursor.close()
        
        # 먼저 중복 체크
        check_cursor = conn.cursor()
        check_cursor.execute("SELECT COUNT(*) FROM STOCK_NEWS WHERE URL = :url", {'url': url_val})
        is_duplicate = check_cursor.fetchone()[0] > 0
        check_cursor.close()
        
        if is_duplicate:
            return True, False  # 중복이므로 스킵 (성공이지만 INSERT 안 됨)
        
        cursor = conn.cursor()
        conn.autocommit = False
        
        sql = """
            INSERT INTO STOCK_NEWS (
                STOCK_CODE, TITLE, CONTENT, URL, NEWS_DATE, CREATED_AT
            ) VALUES (
                :stock_code, :title, :content, :url, :news_date, SYSDATE
            )
        """
        
        cursor.execute(sql, {
            'stock_code': stock_code_val,
            'title': title_val,
            'content': content_val,
            'url': url_val,
            'news_date': news_item['news_date']
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
# 메인 함수 (실제 DB 증가 200개 보장)
# ===============================
def main():
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
        
        print("=" * 60)
        print("뉴스 크롤링 시작 (목표: 실제 DB 증가 200개)")
        print("=" * 60)
        print("※ 빈 페이지 체크 없이 무조건 200개 추가될 때까지 크롤링\n")
        
        total_attempted = 0  # 시도한 개수
        total_inserted = 0   # 실제 INSERT된 개수
        total_duplicated = 0  # 중복 개수
        total_skipped = 0    # 스킵된 개수
        target_count = 200   # 목표 개수 (실제 DB 증가)
        
        # 카테고리 설정
        categories = [
            (101, "경제 일반"),
            (258, "증권"),
        ]
        
        # 최근 7일치 크롤링
        dates_to_crawl = []
        for i in range(7):
            date = datetime.now() - timedelta(days=i)
            dates_to_crawl.append(date.strftime("%Y%m%d"))
        
        print(f"크롤링 대상: {len(categories)}개 카테고리 × {len(dates_to_crawl)}일")
        print(f"목표: 실제 DB 증가 {target_count}개\n")
        
        start_time = datetime.now()
        max_pages_per_date = 50  # 날짜당 최대 페이지 수
        
        # 무한 루프로 200개가 추가될 때까지 계속 크롤링
        while True:
            # 실제 DB 증가 개수 확인
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
                # 실제 DB 증가 개수 확인
                current_db_count = verify_db_count(conn)
                actual_increase = current_db_count - before_count
                
                if actual_increase >= target_count:
                    break
                
                print(f"\n카테고리: {category_name} (sid1={sid1})")
                
                for date_idx, date_str in enumerate(dates_to_crawl, 1):
                    # 실제 DB 증가 개수 확인
                    current_db_count = verify_db_count(conn)
                    actual_increase = current_db_count - before_count
                    
                    if actual_increase >= target_count:
                        break
                    
                    print(f"\n[{date_idx}/{len(dates_to_crawl)}] 날짜: {date_str} (실제 DB 증가: {actual_increase}/{target_count}개)")
                    
                    for page in range(1, max_pages_per_date + 1):
                        # 실제 DB 증가 개수 확인
                        current_db_count = verify_db_count(conn)
                        actual_increase = current_db_count - before_count
                        
                        if actual_increase >= target_count:
                            break
                        
                        print(f"\n▶ {page}페이지 크롤링 중... (실제 DB 증가: {actual_increase}/{target_count}개)")
                        
                        news_list = parse_news_list(conn, sid1=sid1, page=page, date_str=date_str)
                        
                        if not news_list:
                            # 빈 페이지여도 계속 진행 (빈 페이지 체크 없음)
                            print(f"  → 빈 페이지, 계속 진행...")
                            continue
                        
                        print(f"  → {len(news_list)}개 뉴스 발견")
                        
                        for news in news_list:
                            # 실제 DB 증가 개수 확인
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
                                    stock_info = f" [{news['stock_code']}]" if news['stock_code'] else ""
                                    print(f"  ✓ 저장 ({total_inserted}개, 실제 DB 증가: {actual_increase + 1}/{target_count}): {news['title'][:45]}...{stock_info}")
                                else:
                                    total_duplicated += 1
                                    if total_duplicated % 10 == 0:  # 10개마다 출력
                                        print(f"  ⊙ 중복 (시도: {total_attempted}개, 중복: {total_duplicated}개, 실제 추가: {total_inserted}개)")
                            else:
                                total_skipped += 1
                                print(f"  ✗ 실패: {news['title'][:45]}...")
                        
                        time.sleep(0.1)
                    
                    if actual_increase >= target_count:
                        break
                
                if actual_increase >= target_count:
                    break
            
            # 실제 DB 증가 개수 확인
            current_db_count = verify_db_count(conn)
            actual_increase = current_db_count - before_count
            
            if actual_increase >= target_count:
                break
            
            # 새 뉴스를 찾지 못했으면 날짜 범위 확장
            if not found_new_news:
                print(f"\n⚠ 새 뉴스를 찾지 못했습니다. 날짜 범위를 확장합니다...")
                # 최근 7일 → 최근 14일로 확장
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
        
        if after_count - before_count >= target_count:
            print(f"\n✓ 목표 달성! 실제 DB 증가 {after_count - before_count}개 (목표: {target_count}개)")
        else:
            print(f"\n⚠ 목표 미달성: 실제 DB 증가 {after_count - before_count}개 (목표: {target_count}개)")
        
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
