# main.py - 전체 뉴스 분석
from db_connector import DBConnector
from sentiment_analyzer import SentimentAnalyzer

def main():
    # ============================================
    # DB 연결 정보 (opendata_user)
    # ============================================
    DB_USER = "opendata_user"
    DB_PASSWORD = "opendata123"
    DB_DSN = "192.168.10.34:1521/XE"
    
    # DB 연결
    db = DBConnector(DB_USER, DB_PASSWORD, DB_DSN)
    if not db.connect():
        print("DB 연결에 실패했습니다. 연결 정보를 확인하세요.")
        return
    
    # 감성 분석기 초기화 (형태소 분석 사용)
    print("\n감성 분석기 초기화 중...")
    analyzer = SentimentAnalyzer(use_morphology=True)
    print()
    
    try:
        # 분석할 뉴스 조회 (전체)
        print("=" * 60)
        print("분석할 뉴스 조회 중... (전체)")
        print("=" * 60)
        news_list = db.get_unanalyzed_news(limit=None)  # 전체 조회
        print(f"총 {len(news_list)}개의 뉴스를 분석합니다.\n")
        
        if len(news_list) == 0:
            print("분석할 뉴스가 없습니다.")
            return
        
        # 각 뉴스 분석
        success_count = 0
        fail_count = 0
        
        for i, news in enumerate(news_list, 1):
            print(f"[{i}/{len(news_list)}] 뉴스 ID: {news['news_id']} 분석 중...")
            print(f"  제목: {news['title'][:50]}...")
            
            try:
                # 감성 분석 수행 (형태소 분석 사용)
                sentiment, score, keywords = analyzer.analyze_sentiment(
                    news['title'],
                    news['content']
                )
                
                # DB 업데이트
                if db.update_sentiment(news['news_id'], sentiment, score, keywords):
                    success_count += 1
                    print(f"  ✓ 완료: {sentiment} (점수: {score}, 키워드: {keywords})")
                else:
                    fail_count += 1
                    print(f"  ✗ 실패: DB 업데이트 오류")
                
            except Exception as e:
                fail_count += 1
                print(f"  ✗ 실패: {e}")
                import traceback
                traceback.print_exc()
            
            print()
        
        # 결과 요약
        print("=" * 60)
        print("분석 완료!")
        print(f"성공: {success_count}개")
        print(f"실패: {fail_count}개")
        print(f"전체: {len(news_list)}개")
        print("=" * 60)
        
    except Exception as e:
        print(f"에러 발생: {e}")
        import traceback
        traceback.print_exc()
    finally:
        db.close()

if __name__ == "__main__":

    main()
