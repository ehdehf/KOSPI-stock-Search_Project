import requests
from bs4 import BeautifulSoup
from datetime import datetime
import time
import re

# ===============================
# 공통: 문자열 정리 (깨짐·특수문자 제거)
# ===============================
def clean(text):
    if not text:
        return None

    # 제어문자 제거
    text = re.sub(r'[\x00-\x1F\u200b-\u200f]', '', text)

    # EUC-KR 기반 깨짐 방지 처리
    try:
        text = text.encode('euc-kr', 'ignore').decode('euc-kr', 'ignore')
    except:
        pass

    return text.strip()



# ===============================
# 네이버 코스피 시가총액 URL
# ===============================
BASE_URL = "https://finance.naver.com/sise/sise_market_sum.naver"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    )
}

# ===============================
# Spring Boot API URL
# ===============================
API_URL = "http://localhost:8484/api/stocks"


# ===============================
# HTML 가져오기 (안정형)
# ===============================
def fetch_html(page):
    params = {"sosok": 0, "page": page}
    resp = requests.get(BASE_URL, params=params, headers=HEADERS, timeout=10)

    # ★ 핵심: 네이버 금융은 EUC-KR 로 되어있음
    resp.encoding = "EUC-KR"

    return resp.text


# ===============================
# 페이지 파싱
# ===============================
def parse_page(page):
    html = fetch_html(page)
    soup = BeautifulSoup(html, "html.parser")

    rows = soup.select("table.type_2 tr")
    result = []

    for row in rows:
        link = row.select_one("a.tltle")
        if not link:
            continue

        name = clean(link.text)
        code = link.get("href", "").split("code=")[-1]

        nums = [td.text.strip() for td in row.select("td.number")]

        def to_int(v):
            try:
                return int(v.replace(",", "").replace("▲", "").replace("▼", ""))
            except:
                return None

        def to_float(v):
            try:
                return float(v.replace("%", "").replace(",", ""))
            except:
                return None

        price = to_int(nums[0]) if len(nums) > 0 else None
        price_change = to_int(nums[1]) if len(nums) > 1 else None
        change_rate = to_float(nums[2]) if len(nums) > 2 else None
        market_cap = clean(nums[5]) if len(nums) > 5 else None

        dto = {
            "stockCode": code,
            "stockName": name,
            "marketType": "KOSPI",
            "industry": None,
            "price": price,
            "priceChange": price_change,
            "changeRate": change_rate,
            "marketCap": market_cap,
            "updatedAt": datetime.now().isoformat(timespec="seconds")
        }

        result.append(dto)

    return result


# ===============================
# Spring API로 POST (안전 버전)
# ===============================
def send_to_api(item):
    try:
        resp = requests.post(API_URL, json=item, timeout=5)
        if resp.status_code == 200:
            print(f"[OK] {item['stockCode']} 저장")
        else:
            print(f"[FAIL] {item['stockCode']} → 서버 응답 {resp.status_code}")
    except Exception as e:
        print(f"[ERROR] {item['stockCode']} → {e}")


# ===============================
# 메인
# ===============================
def main():
    all_data = []

    print("\n===== KOSPI 종목 전체 수집 시작 =====\n")

    for p in range(1, 40):   # 실제 33~35페이지
        print(f"▶ {p}페이지 수집 중...")
        data = parse_page(p)

        if not data:
            print(f"▶ {p}페이지는 데이터 없음 → 종료")
            break

        all_data.extend(data)
        time.sleep(0.5)

    print(f"\n총 {len(all_data)}개 종목 수집 완료\n")

    # API 전송
    for item in all_data:
        send_to_api(item)
        time.sleep(0.05)


if __name__ == "__main__":
    main()
