import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';

// 스타일 객체 정의 (styled-components 대체)
const styles = {
  container: {
    maxWidth: '1000px',
    margin: '50px auto',
    padding: '20px',
    fontFamily: 'sans-serif',
  },
  header: {
    borderBottom: '2px solid #333',
    paddingBottom: '20px',
    marginBottom: '30px',
  },
  stockTitle: {
    margin: '0',
    color: '#333',
    display: 'flex',
    alignItems: 'baseline',
    fontSize: '2em',
    fontWeight: 'bold',
  },
  stockCode: {
    fontSize: '18px',
    color: '#666',
    marginLeft: '10px',
    fontWeight: 'normal',
  },
  priceContainer: {
    marginTop: '10px',
    display: 'flex',
    alignItems: 'flex-end',
    gap: '15px',
  },
  price: {
    fontSize: '36px',
    fontWeight: 'bold',
  },
  changeInfo: {
    fontSize: '18px',
    fontWeight: '500',
    marginBottom: '8px',
  },
  metaData: {
    marginTop: '10px',
    fontSize: '14px',
    color: '#666',
  },
  metaSpan: {
    marginRight: '15px',
  },
  section: {
    marginBottom: '40px',
    backgroundColor: 'white',
    padding: '25px',
    borderRadius: '12px',
    boxShadow: '0 2px 10px rgba(0,0,0,0.05)',
    border: '1px solid #eee',
  },
  sectionTitle: {
    marginBottom: '15px',
    borderLeft: '4px solid #007bff',
    paddingLeft: '10px',
    fontSize: '1.5em',
    fontWeight: 'bold',
    color: '#333',
  },
  sentimentBarContainer: {
    display: 'flex',
    gap: '30px',
    alignItems: 'center',
  },
  barWrapper: {
    flex: 1,
    height: '20px',
    backgroundColor: '#eee',
    borderRadius: '10px',
    overflow: 'hidden',
    display: 'flex',
  },
  sentimentStats: {
    display: 'flex',
    gap: '15px',
    fontSize: '16px',
    fontWeight: 'bold',
  },
  newsItem: {
    borderBottom: '1px solid #eee',
    padding: '15px 0',
  },
  newsLink: {
    textDecoration: 'none',
    color: '#333',
    fontWeight: 'bold',
    fontSize: '17px',
    display: 'block',
    marginBottom: '8px',
  },
  newsSummary: {
    fontSize: '14px',
    color: '#555',
    marginBottom: '8px',
    lineHeight: '1.4',
  },
  newsInfo: {
    fontSize: '12px',
    color: '#888',
    display: 'flex',
    gap: '10px',
  },
  sentimentBadge: {
    fontWeight: 'bold',
    marginRight: '5px',
  },
  noNews: {
    textAlign: 'center',
    color: '#888',
  }
};

function StockDetailPage() {
  const { stockCode } = useParams();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        setLoading(true);
        const response = await axios.get(`/api/stocks/${stockCode}`);
        console.log("상세 정보 수신:", response.data);
        setData(response.data);
      } catch (error) {
        console.error("상세 정보 조회 실패", error);
        alert("정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };
    fetchDetail();
  }, [stockCode]);

  if (loading) return <div style={styles.container}>로딩중...</div>;
  if (!data) return <div style={styles.container}>데이터가 없습니다.</div>;

  const { stockInfo, newsList, sentiment } = data;

  // 등락에 따른 색상 및 기호 결정 로직
  const isRising = stockInfo.priceChange > 0;
  const isFalling = stockInfo.priceChange < 0;
  const priceColor = isRising ? '#d60000' : isFalling ? '#0051c7' : '#333';
  const priceSign = isRising ? '▲' : isFalling ? '▼' : '-';

  return (
    <div style={styles.container}>
      {/* 1. 헤더 정보 (이름, 가격, 등락폭) */}
      <div style={styles.header}>
        <h1 style={styles.stockTitle}>
          {stockInfo.stockName} <span style={styles.stockCode}>{stockInfo.stockCode}</span>
        </h1>
        
        <div style={styles.priceContainer}>
          <div style={{ ...styles.price, color: priceColor }}>
            {stockInfo.price.toLocaleString()}원
          </div>
          <div style={{ ...styles.changeInfo, color: priceColor }}>
            {priceSign} {Math.abs(stockInfo.priceChange).toLocaleString()} 
            ({stockInfo.changeRate}%)
          </div>
        </div>

        <div style={styles.metaData}>
            <span style={styles.metaSpan}>시장: {stockInfo.marketType}</span>
            <span style={styles.metaSpan}>업종: {stockInfo.industry}</span>
            <span style={styles.metaSpan}>시가총액: {stockInfo.marketCap}</span>
            <span style={styles.metaSpan}>기준일: {stockInfo.updatedAt}</span>
        </div>
      </div>

      {/* 2. 감성 분석 요약 */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>🤖 AI 뉴스 감성 분석</h3>
        <div style={styles.sentimentBarContainer}>
            {/* 간단한 바 차트 시각화 */}
            <div style={styles.barWrapper}>
                <div style={{ width: `${sentiment?.positiveRate}%`, backgroundColor: '#d60000' }} />
                <div style={{ width: `${sentiment?.neutralRate}%`, backgroundColor: '#999' }} />
                <div style={{ width: `${sentiment?.negativeRate}%`, backgroundColor: '#0051c7' }} />
            </div>
            
            <div style={styles.sentimentStats}>
                <div style={{ color: '#d60000' }}>긍정 {sentiment?.positiveCount}건 ({sentiment?.positiveRate}%)</div>
                <div style={{ color: '#0051c7' }}>부정 {sentiment?.negativeCount}건 ({sentiment?.negativeRate}%)</div>
                <div style={{ color: '#666' }}>중립 {sentiment?.neutralCount}건</div>
            </div>
        </div>
      </div>

      {/* 3. 뉴스 리스트 */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>📰 관련 주요 뉴스</h3>
        {newsList && newsList.length > 0 ? (
            newsList.map((news) => (
                <div key={news.newsId} style={styles.newsItem}>
                    <a href={news.url} target="_blank" rel="noopener noreferrer" style={styles.newsLink}>
                        {news.title}
                    </a>
                    <div style={styles.newsSummary}>{news.content}</div>
                    <div style={styles.newsInfo}>
                        {/* 감성 뱃지 표시 */}
                        <span style={{ 
                            ...styles.sentimentBadge, 
                            color: news.sentiment === '긍정' ? '#d60000' : news.sentiment === '부정' ? '#0051c7' : '#666' 
                        }}>
                            [{news.sentiment}]
                        </span>
                        <span>{news.newsDate}</span>
                        <span>키워드: {news.keywords}</span>
                    </div>
                </div>
            ))
        ) : (
            <p style={styles.noNews}>관련 뉴스가 없습니다.</p>
        )}
      </div>

    </div>
  );
}

export default StockDetailPage;