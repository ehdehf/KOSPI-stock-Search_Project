# db_connector.py - CLOB 처리 수정 + KEYWORDS NULL 조건 추가
import oracledb
from datetime import datetime

class DBConnector:
    def __init__(self, user, password, dsn):
        self.user = user
        self.password = password
        self.dsn = dsn
        self.conn = None
    
    def connect(self):
        """DB 연결"""
        try:
            try:
                oracledb.init_oracle_client()
            except:
                pass
            
            self.conn = oracledb.connect(
                user=self.user,
                password=self.password,
                dsn=self.dsn
            )
            print("Oracle DB 연결 성공!")
            return True
        except Exception as e:
            print(f"DB 연결 실패: {e}")
            return False
    
    def get_unanalyzed_news(self, limit=None):
        """
        분석 안 된 뉴스 조회 (전체)
        (SENTIMENT가 NULL이거나 SCORE가 NULL이거나 KEYWORDS가 NULL인 뉴스)
        """
        if not self.conn:
            print("DB 연결이 안 되어 있습니다.")
            return []
        
        try:
            cursor = self.conn.cursor()
            
            if limit:
                sql = """
                    SELECT NEWS_ID, STOCK_CODE, TITLE, CONTENT
                    FROM (
                        SELECT NEWS_ID, STOCK_CODE, TITLE, CONTENT
                        FROM STOCK_NEWS
                        WHERE SENTIMENT IS NULL OR SCORE IS NULL OR KEYWORDS IS NULL
                        ORDER BY CREATED_AT ASC
                    )
                    WHERE ROWNUM <= :limit
                """
                cursor.execute(sql, limit=limit)
            else:
                # 전체 조회
                sql = """
                    SELECT NEWS_ID, STOCK_CODE, TITLE, CONTENT
                    FROM STOCK_NEWS
                    WHERE SENTIMENT IS NULL OR SCORE IS NULL OR KEYWORDS IS NULL
                    ORDER BY CREATED_AT ASC
                """
                cursor.execute(sql)
            
            results = cursor.fetchall()
            
            # 딕셔너리 형태로 변환 (CLOB 처리)
            news_list = []
            for row in results:
                news_id = row[0]
                stock_code = row[1]
                title = row[2]
                
                # ★ CLOB 처리: content를 문자열로 변환
                content = row[3]
                if content:
                    # CLOB 객체인 경우 read() 메서드로 읽기
                    if hasattr(content, 'read'):
                        content = content.read()
                    # 이미 문자열이면 그대로 사용
                    if not isinstance(content, str):
                        content = str(content)
                else:
                    content = ""
                
                news_list.append({
                    'news_id': news_id,
                    'stock_code': stock_code,
                    'title': title if title else "",
                    'content': content
                })
            
            cursor.close()
            return news_list
            
        except Exception as e:
            print(f"뉴스 조회 실패: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def update_sentiment(self, news_id, sentiment, score, keywords):
        """
        감성 분석 결과를 DB에 업데이트
        """
        if not self.conn:
            print("DB 연결이 안 되어 있습니다.")
            return False
        
        try:
            cursor = self.conn.cursor()
            sql = """
                UPDATE STOCK_NEWS
                SET SENTIMENT = :sentiment,
                    SCORE = :score,
                    KEYWORDS = :keywords,
                    UPDATED_AT = SYSDATE
                WHERE NEWS_ID = :news_id
            """
            cursor.execute(sql, {
                'sentiment': sentiment,
                'score': score,
                'keywords': keywords,
                'news_id': news_id
            })
            self.conn.commit()
            cursor.close()
            return True
            
        except Exception as e:
            print(f"업데이트 실패 (뉴스 ID: {news_id}): {e}")
            self.conn.rollback()
            return False
    
    def close(self):
        """DB 연결 종료"""
        if self.conn:
            self.conn.close()
            print("DB 연결 종료")