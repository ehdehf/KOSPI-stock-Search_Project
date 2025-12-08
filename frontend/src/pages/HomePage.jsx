// src/pages/HomePage.jsx
import React, { useState, useEffect } from 'react';
import styled, { keyframes, css } from 'styled-components';
import { Link } from 'react-router-dom';
import axios from 'axios';

// 🌟 차트 컴포넌트 import (KospiLineChart를 사용하기 위해 필요)
import KosdaqLineChart from '../components/shared/KosdaqLineChart';
import KospiLineChart from '../components/shared/KospiLineChart';


// 🔴 경로: 상위 폴더(src)로 가서 components/shared로 접근
// 실제 컴포넌트는 나중에 구현한다고 가정하고 빈 박스로 대체합니다.
// import KospiIndexCard from '../components/shared/KospiIndexCard'; 
// import NewsCard from '../components/shared/NewsCard'; 

// --- 임시 컴포넌트 ---
const KospiIndexCard = styled.div`
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  min-height: 250px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  & > h3 {
    color: #3f51b5;
    margin-bottom: 15px;
  }
`;

const NewsCard = styled.div`
  background-color: #f7f7f7;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  border-left: 5px solid #3f51b5;
  & > p {
    font-size: 0.9rem;
    color: #555;
  }
`;
// -----------------

// --- Styled Components for Layout ---

const HomePageContainer = styled.div`
  padding: 30px;
  background-color: #f0f2f5; /* 전체 배경색 */
  min-height: 100vh;
`;

const HeaderSection = styled.header`
  margin-bottom: 40px;
  & > h1 {
    color: #1e3a8a;
    font-weight: 800;
    font-size: 2.5rem;
  }
  & > p {
    color: #6b7280;
    margin-top: 5px;
  }
`;

const IndexAndMarketSection = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 지수 2개(Kospi/Kosdaq)와 급등/급락 종목 1개 */
  gap: 20px;
  margin-bottom: 40px;
`;

const MarketStatusCard = styled.div`
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
`;

const StockList = styled.ul`
  list-style: none;
  padding: 0;
  margin-top: 15px;
  & > li {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px dashed #eee;
    font-size: 0.95rem;
  }
`;

const NewsSection = styled.section`
  background-color: #ffffff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
`;

const NewsHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  & > h2 {
    color: #1e3a8a;
    font-size: 1.8rem;
  }
`;

const KeywordTabs = styled.div`
  display: flex;
  margin-bottom: 20px;
  border-bottom: 2px solid #e5e7eb;
`;

const KeywordTab = styled.button`
  background: none;
  border: none;
  padding: 10px 15px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: ${props => (props.active ? 'bold' : 'normal')};
  color: ${props => (props.active ? '#3f51b5' : '#6b7280')};
  border-bottom: ${props => (props.active ? '3px solid #3f51b5' : '3px solid transparent')};
  transition: all 0.2s;
  /* 🌟 비표준 prop 경고를 무시하고 DOM에 전달하지 않음 */
  &[active="true"] { 
    font-weight: bold;
    color: #3f51b5;
    border-bottom: 3px solid #3f51b5;
  }
`;

const NewsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 뉴스는 2열로 표시 */
  gap: 20px;
`;

// ----------------------------------------------------
// 🌟 Marquee (애니메이션) 관련 Styled Components
// ----------------------------------------------------

const marquee = keyframes`
  0% { transform: translateX(0%); }
  100% { transform: translateX(-50%); } 
`;

const StockMarqueeSection = styled.div`
  margin-bottom: 40px;
  overflow: hidden; 
  white-space: nowrap; 
  background-color: #ffffff;
  padding: 10px 0;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
`;

const StockMarqueeContainer = styled.div`
  /* 애니메이션 속도를 60초로 설정 */
  animation: ${marquee} 60s linear infinite; 
  &:hover {
    animation-play-state: paused; 
  }
  width: 200%; 
  display: flex; 
`;

const MarqueeContent = styled.div`
  /* flex: 0 0 50%로 너비 고정하여 끊김 없는 순환 구현 */
  flex: 0 0 50%; 
  display: inline-flex; 
  gap: 25px; 
  padding: 0 25px; 
`;

const StockPill = styled.span`
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s;
  
  ${props => {
    // 🌟 boolean prop 경고를 피하기 위해 string "true" 또는 "false"로 사용
    const rateString = props.rate ? props.rate.toString().replace(/%|\+/g, '') : '0';
    const isPositive = parseFloat(rateString) > 0;
    const color = isPositive ? '#10b981' : '#ef4444'; 
    const bgColor = isPositive ? '#ecfdf5' : '#fef2f2'; 
    const borderColor = isPositive ? '#34d399' : '#f87171'; 

    return css`
      color: ${color};
      background-color: ${bgColor};
      border: 1px solid ${borderColor};

      &:hover {
        transform: translateY(-2px); 
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
      }
    `;
  }}
`;

const StockName = styled.span`
  margin-right: 5px;
`;

// ----------------------------------------------------
// 🌟 유틸리티 함수
// ----------------------------------------------------

/** 등락률을 포맷합니다. (예: 1.49 -> +1.49%) */
const formatRate = (rate) => {
    if (rate === undefined || rate === null) return '-';
    const numericRate = Number(rate); 
    if (isNaN(numericRate)) return '-';
    
    const sign = numericRate > 0 ? '+' : (numericRate < 0 ? '' : '');
    return `${sign}${numericRate.toFixed(2)}%`; 
};


// --- HomePage Function ---
function HomePage() {
    
    const [activeKeyword, setActiveKeyword] = useState('Today_Hot');

    // 🌟 1. API 데이터를 저장할 상태
    const [stockData, setStockData] = useState({
        rising: [],
        falling: [],
    });
    const [loading, setLoading] = useState(true);

    // 🌟 2. 백엔드에서 급등/급락 종목 데이터를 불러오는 useEffect
    useEffect(() => {
        const fetchTopMovers = async () => {
            try {
                setLoading(true);
                // 🚨 스프링 부트 API 호출 경로 (급등/급락 종목)
                const response = await axios.get('http://localhost:8484/api/stocks/top-movers');
                
                // 받아온 데이터 (Map 형태)를 상태에 저장
                setStockData({
                    rising: response.data.rising,
                    falling: response.data.falling,
                });

            } catch (error) {
                console.error("Top Movers 데이터 로드 실패:", error);
                setStockData({ rising: [], falling: [] });
            } finally {
                setLoading(false);
            }
        };

        fetchTopMovers();
    }, []);


    // --- 임시 데이터 (뉴스 및 마퀴) ---
    const newsData = {
        Today_Hot: [
            { title: '핵심 뉴스 1', summary: '주요 이슈에 대한 간략한 요약입니다.' },
            { title: '핵심 뉴스 2', summary: '시장에 큰 영향을 미치는 소식입니다.' },
            { title: '핵심 뉴스 3', summary: '업계 동향 관련 새로운 정보입니다.' },
            { title: '핵심 뉴스 4', summary: '경제 전문가들의 심층 분석 내용입니다.' },
        ],
        Technology: [
            { title: '기술 뉴스 1', summary: 'AI, 반도체 관련 산업 소식입니다.' },
            { title: '기술 뉴스 2', summary: '미래 산업 동향 관련 정보입니다.' },
        ],
        Economy: [
            { title: '경제 뉴스 1', summary: '금리, 환율 관련 주요 발표입니다.' },
            { title: '경제 뉴스 2', summary: '세계 경제 지표 관련 분석입니다.' },
        ],
    };
    
    const [marqueeStocks, setMarqueeStocks] = useState([]);

    useEffect(() => {
      const fetchMarqueeStocks = async () => {
          try {
              const response = await axios.get('http://localhost:8484/api/stocks/marketcap');
              // ✅ 기존 스타일 유지용 데이터 구조 맞추기
              const converted = response.data.map(stock => ({
                  name: stock.stockName,
                  rate: formatRate(stock.changeRate),
                  code: stock.stockCode   // ✅ 종목코드 추가
              }));

              setMarqueeStocks(converted);
          } catch (error) {
              console.error("마퀴 데이터 로드 실패:", error);
              setMarqueeStocks([]);
          }
      };

      fetchMarqueeStocks();
  }, []);



    // Marquee 콘텐츠 렌더링 함수
    const renderMarqueeContent = () => (
        <>
            {marqueeStocks.map((stock, index) => (
                <Link
                    key={index}
                    to={`/stock/${stock.code}`}   // ✅ 클릭 시 이동
                    style={{ textDecoration: 'none' }}
                >
                    <StockPill rate={stock.rate}>
                        <StockName>{stock.name}</StockName>
                        {stock.rate}
                    </StockPill>
                </Link>
            ))}
        </>
    );



    return (
        <HomePageContainer>
            {/* 1. 헤더 */}
            <HeaderSection>
                <h1>메인 경제 대시보드</h1>
                <p>{new Date().toLocaleString('ko-KR', { dateStyle: 'full' })} 현재 시장 상황</p>
            </HeaderSection>

            {/* 2. 지수 및 급등/급락 종목 영역 */}
            <IndexAndMarketSection>
                {/* Kospi 지수 (그래프 포함 영역) */}
                <KospiIndexCard>
                    <h3>🇰🇷 KOSPI 지수</h3>
                    <p>3,000.50 <span style={{ color: 'red' }}>(+0.50%)</span></p>
                    
                    {/* ⭐ Kospi Line Chart 컴포넌트 삽입 */}
                    <div style={{ 
                        width: '100%', 
                        marginTop: '15px', 
                        // 🌟 그림자 스타일 추가: 차트 영역을 구분
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)', 
                        borderRadius: '6px',
                        padding: '10px',
                        backgroundColor: '#f9f9f9' // 차트 배경을 약간 다르게 설정
                    }}>
                        <KospiLineChart />
                    </div>
                    
                    <p style={{ fontSize: '0.8rem', marginTop: '10px', color: '#888' }}>
                        **그래프 영역** (KospiIndexCard 컴포넌트 내부)
                    </p>
                </KospiIndexCard>

                {/* Kosdaq 지수 (그래프 포함 영역) - Kospi와 동일 스타일 적용 */}
                <KospiIndexCard>
                    <h3>🌐 KOSDAQ 지수</h3>
                    <p>950.75 <span style={{ color: 'blue' }}>(-0.25%)</span></p>
                    
                    {/* ⭐ Kosdaq Line Chart 컴포넌트 삽입 */}
                    <div style={{ 
                        width: '100%', 
                        marginTop: '15px', 
                        // 🌟 그림자 스타일 추가: Kospi와 동일하게 적용
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)', 
                        borderRadius: '6px',
                        padding: '10px',
                        backgroundColor: '#f9f9f9'
                    }}>
                        <KosdaqLineChart />
                    </div>

                    <p style={{ fontSize: '0.8rem', marginTop: '10px', color: '#888' }}>**그래프 영역** (KosdaqIndexCard 컴포넌트 내부)</p>
                </KospiIndexCard>

                {/* 급등/급락 종목 3개씩 - API 데이터 바인딩 */}
                <MarketStatusCard>
                    <h3 style={{ color: '#1e3a8a' }}>🔥 오늘 시장 주도주</h3>
                    
                    {loading ? (
                        <p style={{ textAlign: 'center', marginTop: '30px' }}>종목 데이터 로드 중...</p>
                    ) : (
                        <>
                            {/* 급등 종목 */}
                            <h4 style={{ color: '#ef4444', marginTop: '20px', borderBottom: '1px solid #fee2e2', paddingBottom: '5px' }}>급등 종목 Top 3</h4>
                            <StockList>
                                {stockData.rising.map((stock, index) => (
                                    <li key={stock.stockCode || index}>
                                        <strong>{stock.stockName || '정보 없음'}</strong>
                                        <span style={{ color: '#ef4444', fontWeight: 'bold' }}>{formatRate(stock.changeRate)}</span>
                                    </li>
                                ))}
                            </StockList>

                            {/* 급락 종목 */}
                            <h4 style={{ color: '#3b82f6', marginTop: '20px', borderBottom: '1px solid #eff6ff', paddingBottom: '5px' }}>급락 종목 Top 3</h4>
                            <StockList>
                                {stockData.falling.map((stock, index) => (
                                    <li key={stock.stockCode || index}>
                                        <strong>{stock.stockName || '정보 없음'}</strong>
                                        <span style={{ color: '#3b82f6', fontWeight: 'bold' }}>{formatRate(stock.changeRate)}</span>
                                    </li>
                                ))}
                            </StockList>
                        </>
                    )}
                </MarketStatusCard>
            </IndexAndMarketSection>

            {/* 🌟 2.5. 움직이는 종목 마퀴 (끊김 없는 순환 구조) */}
            <StockMarqueeSection>
                <StockMarqueeContainer>
                    {/* 콘텐츠를 두 번 렌더링하고 flex: 0 0 50%로 너비를 고정하여 끊김을 방지합니다. */}
                    <MarqueeContent>{renderMarqueeContent()}</MarqueeContent>
                    <MarqueeContent>{renderMarqueeContent()}</MarqueeContent> 
                </StockMarqueeContainer>
            </StockMarqueeSection>

            {/* 3. 뉴스 및 이슈 키워드 영역 */}
            <NewsSection>
                <NewsHeader>
                    <h2>📰 오늘의 주요 이슈 및 뉴스</h2>
                    <Link to="/trend" style={{ color: '#3f51b5', textDecoration: 'none', fontWeight: '600' }}>
                        더보기 &gt;
                    </Link>
                </NewsHeader>

                {/* 키워드 탭 */}
                <KeywordTabs>
                    {Object.keys(newsData).map((keyword) => (
                        <KeywordTab
                            key={keyword}
                            // 🌟 boolean prop 경고를 피하기 위해 문자열로 변환
                            active={(activeKeyword === keyword).toString()} 
                            onClick={() => setActiveKeyword(keyword)}
                        >
                            {keyword.replace('_', ' ')}
                        </KeywordTab>
                    ))}
                </KeywordTabs>

                {/* 뉴스 리스트 (선택된 키워드에 따라) */}
                <NewsGrid>
                    {newsData[activeKeyword].map((news, index) => (
                        <NewsCard key={index}>
                            <h4 style={{ color: '#1e3a8a', marginBottom: '5px' }}>{news.title}</h4>
                            <p>{news.summary}</p>
                            <Link to={`/news/${index}`} style={{ fontSize: '0.8rem', color: '#6366f1', marginTop: '10px', display: 'block' }}>
                                뉴스 상세 보기
                            </Link>
                        </NewsCard>
                    ))}
                </NewsGrid>
            </NewsSection>
        </HomePageContainer>
    );
}

export default HomePage;