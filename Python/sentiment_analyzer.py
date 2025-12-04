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
            '상승', '급등', '호재', '증가', '성장', '개선', '돌파', 
            '신고가', '반등', '회복', '기대', '긍정', '확산', '출시',
            '긍정적', '호조', '안정', '강세', '수익', '실적', '성공',
            '투자', '확대', '성장세', '두드러지', '평가', '개발',
            '상향', '증액', '확장', '향상', '개선', '회복', '반등',
            '수익성', '실적개선', '매출증가', '이익증가', '성장률',
            '투자확대', '시장점유율', '경쟁력', '혁신', '기술개발',
            '폭발', '회복', '강화', '증대', '확충', '상승세'
        ]
        
        # 부정 키워드 (형태소 분석용 - 어간 추출)
        self.negative_keywords = [
            '하락', '급락', '부재', '감소', '위축', '악화', '폭락',
            '신저가', '하향', '침체', '우려', '부정', '공격', '위험',
            '부정적', '부진', '불안', '약세', '손실', '실패', '위기',
            '매도', '위축', '악화', '우려', '제기', '치열', '불안',
            '하락세', '실적부진', '매출감소', '이익감소', '손실확대',
            '경쟁력저하', '시장점유율하락', '부채증가', '유동성위기',
            '리스크', '불확실성', '변동성', '하락압력', '부담',
            '추락', '위축', '감소세', '부담', '악재'
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
        
        if total_keywords == 0:
            score = 0
            sentiment = "보통"
        else:
            # 점수 계산: (긍정 - 부정) / 전체 * 100
            score = int(((positive_count - negative_count) / total_keywords) * 100)
            
            # 점수 범위 제한
            score = max(-100, min(100, score))
            
            # 감성 라벨 결정
            if score >= 50:
                sentiment = "긍정"
            elif score <= -50:
                sentiment = "부정"
            else:
                sentiment = "보통"
        
        # 키워드 추출
        keywords = self.extract_keywords(full_text)
        
        return sentiment, score, keywords