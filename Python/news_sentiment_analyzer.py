# sentiment_analyzer.py - 형태소 분석기 초기화 개선 + 기본 키워드 추출 개선
import re
from collections import Counter

# 형태소 분석 라이브러리 (Okt 또는 Komoran)
try:
    from konlpy.tag import Okt, Komoran
    KONLPY_AVAILABLE = True
except ImportError:
    KONLPY_AVAILABLE = False
    print("⚠ 경고: konlpy가 설치되지 않았습니다. 기본 키워드 매칭을 사용합니다.")
    print("   설치: pip install konlpy")

class SentimentAnalyzer:
    def __init__(self, use_morphology=True):
        """
        use_morphology: True면 형태소 분석 사용, False면 기본 키워드 매칭
        """
        self.use_morphology = use_morphology and KONLPY_AVAILABLE
        self.morph_analyzer = None
        
        # 형태소 분석기 초기화
        if self.use_morphology:
            try:
                # Okt 시도 (더 빠름)
                self.okt = Okt()
                self.morph_analyzer = "Okt"
                print("✓ Okt 형태소 분석기 사용")
            except Exception as e1:
                try:
                    # Komoran 시도
                    self.komoran = Komoran()
                    self.morph_analyzer = "Komoran"
                    print("✓ Komoran 형태소 분석기 사용")
                except Exception as e2:
                    self.use_morphology = False
                    print(f"⚠ 형태소 분석기 초기화 실패")
                    print(f"   Okt 에러: {e1}")
                    print(f"   Komoran 에러: {e2}")
                    print("   기본 키워드 매칭을 사용합니다.")
        
        # 긍정 키워드 (형태소 분석용 - 어간 추출)
        self.positive_keywords = [
            '상승','급등','호재','증가','성장','개선','돌파','신고가','반등','회복','기대','확산','출시',
            '긍정적','호조','안정','강세','수익','실적','성공','투자','확대','성장세','평가','개발',
            '상향','증액','확장','향상','수익성','매출증가','이익증가','성장률','투자확대','경쟁력','혁신',
            '기술개발','강화','증대','확충','상승세',

            # 확장 긍정 단어
            '호황','호조세','개선세','개선국면','상향조정','목표가상향','매수세','수급개선','수주확대',
            '영업이익증가','영업익증가','순이익증가','흑자전환','실적호전','시장점유율확대','수요증가','수요회복',
            '수요확대','기대감','개발성공','신규확보','지속상승','강한수요','탄탄한수요','고성장','성장모멘텀',
            '인기','관심집중','강력','긍정전망','매출호조','실적회복','개선추세','수익확대','신규수주','사업확장',
            '글로벌확장','해외확대','해외진출','신사업성공','전망밝음','촉진','기여','추진','인정받다','기록경신',
            '돌파구','상승기조','상승탄력','활성화','확대추세','개선기대','매력적','긍정평가','전망우호적','수급강세',
            '초강세','강한반등','강력반등','회복세','급반등','기록적성장','사상최대','초과달성','목표초과','후속호재',
            '동반상승','강한매수','주가상승','주가강세','랠리','지속강세','강세장','상승여력','상승압력','수익상승',
            '만회','도약','기지개','재도약','급증','급성장','급개선','안정화','시장확대','환경개선',
            '구조적성장','급성장세','호재성','초호황','초기대','폭발적성장','수출증가','판매증가','고평가','견조한수요'
        ]
        
        # 부정 키워드 (형태소 분석용 - 어간 추출)
        self.negative_keywords = [
            '하락','급락','부재','감소','위축','악화','폭락','신저가','하향','침체','우려','부정','위험',
            '부진','불안','약세','손실','실패','위기','매도','치열','불안','실적부진','매출감소','이익감소',
            '손실확대','경쟁력저하','시장점유율하락','부채증가','유동성위기','리스크','불확실성','변동성','부담','악재',

            # 확장 부정 단어
            '부정적전망','부정평가','수요감소','수요위축','수요둔화','수요부진','가격하락','가치하락','영업익감소',
            '영업이익감소','순이익감소','적자전환','적자','대규모적자','부도위기','채무불이행','감산','정리해고',
            '구조조정','감원','파산위기','급감','급감소','실적악화','수익성악화','고정비부담','원가부담','부담가중',
            '악영향','악순환','시장침체','수출감소','판매부진','주가급락','주가하락','약보합','혼조세','실패확률',
            '불확실전망','전망악화','부정전망','부진지속','악화추세','급하락','폭하락','투자심리악화','심리위축',
            '부정요인','악성재고','재고증가','경기둔화','경기침체','경기악화','경기침체우려','경기둔화우려',
            '급락세','하락세지속','하락압력','약세지속','약세기조','악화국면','실망','악화요인','불확실성확대',
            '고평가부담','주가조정','급조정','조정국면','하향조정','목표가하향','실적쇼크','매도압력','매도세',
            '투매','투심위축','급락장','약세장','침체장','비관론','부정론','악화요소','위험요인','부정기사',
            '충격','타격','하락모멘텀','성장둔화','악재쏟아져','리스크확대','불리','감소세지속','영향축소',
            '축소','위축세','취약성','취약','악화확률','퇴보','침체심화','부진한흐름','기록적하락','저조','저조세',
            '저조한실적','적자지속','실적쇼크','예상하회','하회','실망감','약세압력'
        ]
        
        # 주요 키워드 추출용 (주식 관련 중요 단어)
        self.important_keywords = [
            '주가', '주식', '시장', '투자', '실적', '매출', '이익',
            '반도체', 'AI', '메모리', '스마트폰', '디스플레이',
            '코스피', '코스닥', '상장', '배당', '인수', '합병',
            '삼성전자', '네이버', '카카오', 'LG전자', 'SK하이닉스',
            '게임', '서비스', '기술', '경쟁', '마케팅',
            'HBM', 'GPU', '서버', '데이터센터', '클라우드',
            '전기차', '배터리', '수소', '신재생에너지', '태양광',
            '바이오', '제약', '화학', '철강', '건설', '부동산',
            '증권', '은행', '금융', '펀드', 'ETF', '채권',
            '환율', '금리', '인플레이션', '경제', 'GDP',
            '공시', '상장폐지', '자사주', '소각', '청약',
            '계약', '공급', '발행', '증자', '인수합병'
        ]
    
    def preprocess_text(self, text):
        """텍스트 전처리"""
        if not text:
            return ""
        # 문자열로 변환
        if not isinstance(text, str):
            text = str(text)
        # HTML 태그 제거
        text = re.sub('<[^>]+>', '', text)
        # 공백 정리
        text = ' '.join(text.split())
        return text
    
    def extract_morphs(self, text):
        """형태소 분석을 통한 단어 추출"""
        if not self.use_morphology or not text:
            return []
        
        try:
            if self.morph_analyzer == "Okt":
                # Okt: 명사, 형용사, 동사 추출
                morphs = self.okt.pos(text, norm=True, stem=True)
                # 명사, 형용사, 동사만 추출
                words = [word for word, pos in morphs if pos in ['Noun', 'Adjective', 'Verb']]
            elif self.morph_analyzer == "Komoran":
                # Komoran: 명사, 형용사, 동사 추출
                morphs = self.komoran.pos(text)
                words = [word for word, pos in morphs if pos.startswith('N') or pos.startswith('VA') or pos.startswith('VV')]
            else:
                return []
            
            return words
        except Exception as e:
            print(f"형태소 분석 실패: {e}")
            return []
    
    def extract_simple_keywords(self, text):
        """형태소 분석 없이 간단한 키워드 추출 (2글자 이상 명사 추정)"""
        if not text:
            return []
        
        # 한글 2글자 이상 단어 추출
        korean_words = re.findall(r'[가-힣]{2,}', text)
        # 숫자+한글 조합 (예: 100억, 3분기)
        number_korean = re.findall(r'\d+[가-힣]+', text)
        # 영문 대문자 단어 (예: AI, ETF, GDP)
        english_caps = re.findall(r'[A-Z]{2,}', text)
        
        # 중복 제거 및 길이 제한 (너무 긴 단어 제외)
        all_words = korean_words + number_korean + english_caps
        filtered_words = [w for w in all_words if 2 <= len(w) <= 10]
        
        return list(set(filtered_words))
    
    def extract_keywords(self, text, max_keywords=5):
        """주요 키워드 추출 (형태소 분석 사용)"""
        if not text:
            return ""
        
        text = self.preprocess_text(text)
        found_keywords = []
        
        if self.use_morphology:
            # 형태소 분석으로 추출한 단어 중에서 중요 키워드 찾기
            morphs = self.extract_morphs(text)
            for morph in morphs:
                for keyword in self.important_keywords:
                    if keyword in morph or morph in keyword:
                        if keyword not in found_keywords:
                            found_keywords.append(keyword)
                        break
        else:
            # 기본 방식 1: 중요 키워드 중에서 텍스트에 포함된 것 찾기
            for keyword in self.important_keywords:
                if keyword in text:
                    found_keywords.append(keyword)
            
            # 기본 방식 2: 간단한 키워드 추출로 보완
            if len(found_keywords) < max_keywords:
                simple_keywords = self.extract_simple_keywords(text)
                # 중요 키워드와 겹치지 않는 것만 추가
                for word in simple_keywords:
                    if word not in found_keywords and len(found_keywords) < max_keywords:
                        # 너무 일반적인 단어 제외
                        if word not in ['것', '수', '때', '등', '및', '또한', '그리고']:
                            found_keywords.append(word)
        
        # 최대 개수만큼만 반환
        keywords_str = ", ".join(found_keywords[:max_keywords])
        return keywords_str
    
    def analyze_sentiment(self, title, content):
        """
        감성 분석 수행 (형태소 분석 사용)
        반환: (sentiment, score, keywords)
        - sentiment: '긍정' / '보통' / '부정'
        - score: -100 ~ 100 (50이상-긍정, -49~+49-보통, -50이하-부정)
        - keywords: "AI, 반도체, 메모리" 형식
        """
        # 문자열로 변환
        if title and not isinstance(title, str):
            title = str(title)
        if content and not isinstance(content, str):
            content = str(content)
        
        if not title and not content:
            return "보통", 0, ""
        
        # 제목과 본문 합치기
        full_text = (title or "") + " " + (content or "")
        full_text = self.preprocess_text(full_text)
        
        if not full_text:
            return "보통", 0, ""
        
        # 형태소 분석 사용 여부에 따라 다른 방식으로 키워드 카운트
        if self.use_morphology:
            # 형태소 분석으로 단어 추출
            words = self.extract_morphs(full_text)
            
            # 추출한 단어들 중에서 긍정/부정 키워드 매칭
            positive_count = 0
            negative_count = 0
            
            for word in words:
                # 긍정 키워드 체크
                for pos_keyword in self.positive_keywords:
                    if pos_keyword in word or word in pos_keyword:
                        positive_count += 1
                        break
                
                # 부정 키워드 체크
                for neg_keyword in self.negative_keywords:
                    if neg_keyword in word or word in neg_keyword:
                        negative_count += 1
                        break
        else:
            # 기본 방식: 문자열 포함 여부로 체크
            positive_count = sum(1 for word in self.positive_keywords if word in full_text)
            negative_count = sum(1 for word in self.negative_keywords if word in full_text)
        
        # 점수 계산 (-100 ~ 100)
        total_keywords = positive_count + negative_count
        
                # ------------------------------
        # 1) 점수(score) 계산
        # ------------------------------
        if total_keywords == 0:
            score = 0
        else:
            score = int(((positive_count - negative_count) / total_keywords) * 100)

        score = max(-100, min(100, score))  # 범위 제한

        # ------------------------------
        # 2) A + C 혼합 감성 판단 규칙 (강화 버전)
        # ------------------------------

        # C 기준: 상대 비교 (더 완화)
        if positive_count > negative_count * 1.0:
            sentiment = "긍정"
        elif negative_count > positive_count * 1.0:
            sentiment = "부정"
        else:
            # A 기준: 점수 기준 완화
            if score >= 8:
                sentiment = "긍정"
            elif score <= -8:
                sentiment = "부정"
            else:
                sentiment = "보통"

        # 추가 규칙 — 단어 수 기반 강제 분류
        if positive_count >= 2 and negative_count == 0:
            sentiment = "긍정"
        elif negative_count >= 2 and positive_count == 0:
            sentiment = "부정"
        
        # 키워드 추출
        keywords = self.extract_keywords(full_text)
        
        return sentiment, score, keywords
