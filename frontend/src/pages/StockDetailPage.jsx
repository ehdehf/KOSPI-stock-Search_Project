// ==========================================
// StockDetailPage.jsx (라인 차트, 4가지 봉 단위, 고정 Y축 범위 적용)
// ==========================================

import React, { useEffect, useState, useRef, useCallback, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import Chart from 'react-apexcharts';

// ==========================================
// 1. 스타일 객체 (원본 유지)
// ==========================================
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
    headerTop: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
    },
    stockTitleGroup: {
        display: 'flex',
        flexDirection: 'column',
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
        marginTop: '15px',
        fontSize: '14px',
        color: '#666',
        display: 'flex',
        gap: '20px',
    },
    metaSpan: {
        display: 'inline-block',
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
    newsItemWrapper: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        borderBottom: '1px solid #eee',
        padding: '15px 0',
    },
    newsContent: {
        flex: 1,
        paddingRight: '15px',
    },
    newsLink: {
        textDecoration: 'none',
        fontWeight: 'bold',
        fontSize: '17px',
        display: 'block',
        marginBottom: '8px',
        cursor: 'pointer',
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
    },
    starButton: {
        background: 'none',
        border: 'none',
        fontSize: '40px',
        cursor: 'pointer',
        color: '#FFD700',
        transition: 'transform 0.2s',
        padding: '0 10px',
    },
    starButtonEmpty: {
        color: '#ccc',
    },
    newsStarButton: {
        background: 'none',
        border: 'none',
        fontSize: '24px',
        cursor: 'pointer',
        color: '#ccc',
        padding: '5px',
        transition: 'color 0.2s',
        marginTop: '5px',
    },
    newsStarActive: {
        color: '#FFD700',
    },
    chartToggle: {
        display: 'flex',
        gap: '10px',
        marginBottom: '15px',
        flexWrap: 'wrap', // 버튼이 많아질 경우 줄바꿈 처리
    },
    toggleButton: (isActive) => ({
        padding: '8px 15px',
        border: `1px solid ${isActive ? '#007bff' : '#ccc'}`,
        backgroundColor: isActive ? '#007bff' : 'white',
        color: isActive ? 'white' : '#333',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: 'bold',
        transition: 'all 0.2s',
    }),
};

// ------------------------------------------
// 데이터 포맷팅 유틸리티
// ------------------------------------------
const formatRate = (rate) => {
    if (rate === undefined || rate === null || rate === "") return '-';
    const numericRate = Number(rate);
    if (isNaN(numericRate)) return '-';
    const sign = numericRate > 0 ? '+' : '';
    return `${sign}${numericRate.toFixed(2)}%`;
};

// Flask 구독/해제 유틸리티
const subscribeFlask = async (code) => {
    try {
        await axios.post("http://localhost:5000/subscribe", { code });
    } catch (error) {
        console.error(`[Flask Subscribe Error] ${code}:`, error.response ? error.response.data : error.message);
    }
};

const unsubscribeFlask = async (code) => {
    try {
        await axios.post("http://localhost:5000/unsubscribe", { code }); 
    } catch (error) {
        console.error(`[Flask Unsubscribe Error] ${code}:`, error.response ? error.response.data : error.message);
    }
};

// ==========================================
// 2. 차트 컴포넌트
// ==========================================

// 가상의 봉 데이터 배열 (라인 차트는 [Timestamp, Price] 사용)
const priceData = {
    '1s': [],
    '15s': [],
    '30s': [],
    '60s': []
};

/**
 * 실시간 가격 데이터를 이용하여 라인 차트를 그리는 컴포넌트
 */
function StockChart({ stockCode, rtPrice, basePrice }) {
    const [chartType, setChartType] = useState('1s'); 
    
    const [series, setSeries] = useState([
        {
            name: "현재가",
            data: [], 
        }
    ]);
    
    // ⭐ 봉 단위별 Y축 고정 범위 설정
    const RANGE_MAP = useMemo(() => ({
        '1s': 300,  // ±300원
        '15s': 500, // ±500원
        '30s': 700, // ±700원
        '60s': 1000 // ±1000원
    }), []);
    
    // ⭐ 봉 단위별 X축 표시 범위 설정 (X축 범위는 봉 단위와 비슷하게 설정)
    const X_RANGE_MAP = useMemo(() => ({
        '1s': 15000, // 15초
        '15s': 60000, // 60초
        '30s': 120000, // 2분
        '60s': 300000 // 5분
    }), []);

    /**
     * 실시간 가격을 받아서 라인 차트 데이터에 반영하는 함수
     * @param {string} type '1s', '15s', '30s' 또는 '60s'
     * @param {number} price 현재 가격
     * @param {number} intervalMs 데이터 샘플링 주기 (밀리초 단위)
     */
    const updateChartData = useCallback((type, price, intervalMs) => {
        if (!price) return;
        
        const now = new Date().getTime();
        const dataArray = priceData[type];
        
        const lastTime = dataArray.length > 0 ? dataArray[dataArray.length - 1][0] : 0;
        
        // 마지막 데이터 시점과 현재 시점을 비교하여 intervalMs가 지났는지 확인
        if (now - lastTime >= intervalMs) {
            dataArray.push([now, price]);
            
            // 데이터 수를 50개로 제한하여 성능 최적화
            if (dataArray.length > 50) {
                dataArray.shift();
            }

            // 현재 선택된 차트 타입일 경우만 상태 업데이트
            if (chartType === type) {
                setSeries([{ name: "현재가", data: [...dataArray] }]);
            }
        }
    }, [chartType]);

    // 차트 옵션 (ApexCharts 설정)
    const options = useMemo(() => {
        
        const centerPrice = rtPrice || basePrice || 100000; 
        
        // ⭐ 선택된 봉 단위에 따른 고정 범위 계산
        const rangeDiff = RANGE_MAP[chartType] || 1000; // 기본값 1000원
        const dynamicMin = Math.max(0, centerPrice - rangeDiff); 
        const dynamicMax = centerPrice + rangeDiff;

        // 깔끔한 표시를 위해 100단위로 절사/올림
        const floorMin = Math.floor(dynamicMin / 100) * 100;
        const ceilMax = Math.ceil(dynamicMax / 100) * 100;
        
        const xRange = X_RANGE_MAP[chartType] || 60000; // X축 표시 범위

        return {
            chart: {
                type: 'line', 
                height: 350,
                toolbar: { show: false },
                animations: { enabled: true, easing: 'linear', speed: 500 },
            },
            title: {
                // text: `실시간 ${chartType}봉 (라인) 차트`,
                align: 'left'
            },
            xaxis: {
                type: 'datetime',
                range: xRange, 
                labels: {
                    datetimeFormatter: {
                        year: 'yyyy',
                        month: 'MMM \'yy',
                        day: 'dd MMM',
                        hour: 'HH:mm',
                        minute: 'HH:mm',
                        second: 'HH:mm:ss'
                    }
                },
                tickAmount: 5
            },
            yaxis: {
                tooltip: { enabled: true },
                // ⭐ 고정 범위 적용
                min: floorMin, 
                max: ceilMax, 
                tickAmount: 5,
                labels: {
                    formatter: (value) => value.toLocaleString()
                }
            },
            stroke: {
                curve: 'smooth',
                width: 2,
                colors: ['#007bff'] 
            },
            dataLabels: { enabled: false },
            markers: { size: 0 },
            noData: {
                text: "실시간 데이터를 기다리는 중...",
                align: 'center',
                verticalAlign: 'middle',
                style: {
                    color: '#888',
                    fontSize: '14px'
                }
            }
        };
    }, [chartType, rtPrice, basePrice, RANGE_MAP, X_RANGE_MAP]); 

    // ------------------------------------------
    // 차트 갱신 useEffect (실시간 가격 rtPrice에 반응)
    // ------------------------------------------
    useEffect(() => {
        if (!rtPrice || isNaN(rtPrice)) return;
        
        // ⭐ 4가지 봉 단위 모두 데이터 갱신
        updateChartData('1s', rtPrice, 1000);   // 1초봉
        updateChartData('15s', rtPrice, 15000); // 15초봉
        updateChartData('30s', rtPrice, 30000); // 30초봉
        updateChartData('60s', rtPrice, 60000); // 60초봉 (1분봉)

    }, [rtPrice, updateChartData]);


    // 차트 타입 변경 시, 해당 타입의 데이터로 갱신
    useEffect(() => {
        const targetData = priceData[chartType];
        setSeries([{ name: "현재가", data: [...targetData] }]);
        
    }, [chartType]);
    
    // 종목코드 변경 시 데이터 초기화
    useEffect(() => {
        for (const key in priceData) {
            priceData[key].length = 0;
        }
        setSeries([{ name: "현재가", data: [] }]);
    }, [stockCode]);

    return (
        <div style={styles.section}>
            <h3 style={styles.sectionTitle}>📉 실시간 주가 라인 차트</h3>
            
            {/* ⭐ 차트 토글 버튼 (4가지 옵션) */}
            <div style={styles.chartToggle}>
                <button 
                    style={styles.toggleButton(chartType === '1s')} 
                    onClick={() => setChartType('1s')}
                >
                    1초봉 
                </button>
                <button 
                    style={styles.toggleButton(chartType === '15s')} 
                    onClick={() => setChartType('15s')}
                >
                    15초봉 
                </button>
                <button 
                    style={styles.toggleButton(chartType === '30s')} 
                    onClick={() => setChartType('30s')}
                >
                    30초봉
                </button>
                <button 
                    style={styles.toggleButton(chartType === '60s')} 
                    onClick={() => setChartType('60s')}
                >
                    60초봉 
                </button>
            </div>
            
            {/* 차트 영역 */}
            <Chart options={options} series={series} type="line" height={350} /> 
            <p style={{marginTop: '15px', color: '#666', fontSize: '14px'}}>
                ⚠️ 이 차트는 실시간 가격을 **샘플링**하여 표시하며, Y축 범위는 **현재 가격을 중앙**으로 각 봉 단위별로 **고정된 범위**로 조정됩니다.
            </p>
        </div>
    );
}


// ==========================================
// 3. 메인 컴포넌트 (원본 유지)
// ==========================================
function StockDetailPage() {
    const { stockCode } = useParams();

    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);

    // ⭐ 실시간 데이터 상태
    const [rtPrice, setRtPrice] = useState(null);
    const [rtPriceChange, setRtPriceChange] = useState(null);
    const [rtChangeRate, setRtChangeRate] = useState(null);
    const [isFavorite, setIsFavorite] = useState(false);
    const [savedBookmarks, setSavedBookmarks] = useState([]);

    // STOMP 객체
    const stompClientRef = useRef(null);
    const subscriptionRef = useRef(null);
    // Flask 구독 상태 추적 (종료 시 해제용)
    const subscribedFlaskRef = useRef(false); 


    // ==========================================
    // ① 기본 상세 정보 로드
    // ==========================================
    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);

                // 주식 기본 정보
                const stockRes = await axios.get(`/api/stocks/${stockCode}`);
                setData(stockRes.data);

                // ⭐ 초기 가격 설정 (실시간 데이터 없을 때 대비)
                const initialPrice = Number(stockRes.data.stockInfo.price); 
                setRtPrice(initialPrice);
                setRtPriceChange(Number(stockRes.data.stockInfo.priceChange));
                setRtChangeRate(Number(stockRes.data.stockInfo.changeRate));

                // 로그인 상태면 즐겨찾기 정보 로드 
                const token = localStorage.getItem('accessToken');
                if (token) {
                    const authHeader = { headers: { Authorization: `Bearer ${token}` } };
                    const myRes = await axios.get('/api/mypage/info', authHeader);
                    const myStocks = myRes.data.stocks || [];
                    setIsFavorite(myStocks.some(s => s.stockCode === stockCode));

                    const newsRes = await axios.get('/api/mypage/favorites/news', authHeader);
                    let rawList = newsRes.data;
                    if (!Array.isArray(rawList) && rawList.data) rawList = rawList.data;
                    if (!Array.isArray(rawList) && rawList.list) rawList = rawList.list;

                    if (Array.isArray(rawList)) {
                        const bookmarks = rawList.map(item => ({
                            newsId: String(item.newsId || item.id),
                            isRead: item.isRead
                        })).filter(b => b.newsId !== 'undefined');
                        setSavedBookmarks(bookmarks);
                    }
                }
            } catch (err) {
                console.error(err);
                alert("정보 불러오기 실패");
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [stockCode]);

    // ==========================================
    // ② 실시간 주식 WebSocket 구독 (원본 유지)
    // ==========================================
    useEffect(() => {
        if (!stockCode) return;

        // 1. Flask 구독 요청
        const startSubscription = async () => {
            await subscribeFlask(stockCode);
            subscribedFlaskRef.current = true;
        };
        startSubscription();


        // 2. STOMP 연결 설정
        const client = new Client({
            webSocketFactory: () => new SockJS("http://localhost:8484/ws-stock"),
            reconnectDelay: 5000,
        });

        client.onConnect = () => {
             // 개별 토픽 구독: /topic/stock/{stockCode}
            subscriptionRef.current = client.subscribe(
                `/topic/stock/${stockCode}`,
                (msg) => {
                    const d = JSON.parse(msg.body);
                    
                    // 가격 업데이트
                    setRtPrice(Number(d.currentPrice)); // 숫자로 변환
                    setRtPriceChange(Number(d.priceChange));
                    setRtChangeRate(Number(d.changeRate));
                }
            );
        };

        client.activate();
        stompClientRef.current = client;

        // 3. 정리 함수 (페이지 이동 또는 언마운트 시)
        return () => {
            if (subscriptionRef.current) subscriptionRef.current.unsubscribe();
            if (stompClientRef.current) stompClientRef.current.deactivate();

            // Flask에 해제 요청
            if (subscribedFlaskRef.current) {
                unsubscribeFlask(stockCode);
            }
        };
    }, [stockCode]); 

    // ==========================================
    // ③ 찜하기 / 뉴스 스크랩 / 읽음 처리 함수 (원본 유지)
    // ==========================================
    const handleToggleFavorite = async () => {
        const token = localStorage.getItem('accessToken');
        if (!token) return alert("로그인이 필요합니다.");

        try {
            if (isFavorite) {
                await axios.delete(`/api/mypage/favorites/stock/${stockCode}`, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                setIsFavorite(false);
            } else {
                await axios.post('/api/mypage/favorites/stock', { stockCode }, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                setIsFavorite(true);
            }
        } catch (e) {
            console.error(e);
        }
    };

    const handleToggleNewsBookmark = async (news) => {
        const token = localStorage.getItem("accessToken");
        if (!token) return alert("로그인이 필요합니다.");

        const newsId = String(news.newsId || news.id);
        const isBookmarked = savedBookmarks.some(b => b.newsId === newsId);

        try {
            if (isBookmarked) {
                await axios.delete(`/api/mypage/favorites/news/${newsId}`, {
                    headers: { Authorization: `Bearer ${token}` },
                });

                setSavedBookmarks(prev => prev.filter(b => b.newsId !== newsId));
            } else {
                await axios.post(
                    "/api/mypage/favorites/news",
                    { newsId },
                    { headers: { Authorization: `Bearer ${token}` } }
                );

                setSavedBookmarks(prev => [...prev, { newsId, isRead: "N" }]);
            }
        } catch (e) {
            console.error(e);
        }
    };

    const handleNewsClick = async (newsId, url, isBookmarked) => {
        window.open(url, "_blank", "noopener,noreferrer");
        const token = localStorage.getItem("accessToken");
        if (!token || !isBookmarked) return;

        try {
            await axios.post(
                "/api/mypage/favorites/news/read",
                { newsId },
                { headers: { Authorization: `Bearer ${token}` } }
            );

            setSavedBookmarks(prev =>
                prev.map(b =>
                    b.newsId === String(newsId) ? { ...b, isRead: "Y" } : b
                )
            );
        } catch (e) {
            console.error(e);
        }
    };

    // ==========================================
    // 화면 렌더링
    // ==========================================
    if (loading) return <div style={styles.container}>로딩중...</div>;
    if (!data || !data.stockInfo) return <div style={styles.container}>데이터가 없습니다.</div>; 

    const { stockInfo, newsList, sentiment } = data;

    // ------------------------------
    // 실시간 가격 적용 
    // ------------------------------
    const displayPrice = rtPrice ?? stockInfo.price;
    const displayChange = rtPriceChange ?? stockInfo.priceChange;
    const displayRate = rtChangeRate ?? stockInfo.changeRate;

    const priceColor =
        displayRate > 0 ? "#d60000"
        : displayRate < 0 ? "#0051c7"
        : "#333";

    const priceSign =
        displayRate > 0 ? "▲"
        : displayRate < 0 ? "▼"
        : "-";
    
    // 가격 문자열 포맷팅
    const formattedPrice = displayPrice ? Number(displayPrice).toLocaleString() : '—';
    const formattedChange = displayChange ? Math.abs(Number(displayChange)).toLocaleString() : '—';
    const formattedRate = formatRate(displayRate); 

    return (
        <div style={styles.container}>
            
            {/* -------------------------- */}
            {/*   헤더 / 가격 / 메타 정보    */}
            {/* -------------------------- */}
            <div style={styles.header}>
                <div style={styles.headerTop}>
                    <div style={styles.stockTitleGroup}>
                        <h1 style={styles.stockTitle}>
                            {stockInfo.stockName}
                            <span style={styles.stockCode}>{stockInfo.stockCode}</span>
                        </h1>

                        <div style={styles.priceContainer}>
                            <div style={{ ...styles.price, color: priceColor }}>
                                {formattedPrice}원
                            </div>
                            <div style={{ ...styles.changeInfo, color: priceColor }}>
                                {priceSign} {formattedChange}  
                                <span style={{ marginLeft: '5px' }}>({formattedRate})</span>
                            </div>
                        </div>
                    </div>

                    {/* 종목 찜 버튼 */}
                    <button
                        style={{
                            ...styles.starButton,
                            ...(isFavorite ? {} : styles.starButtonEmpty),
                        }}
                        onClick={handleToggleFavorite}
                    >
                        {isFavorite ? "★" : "☆"}
                    </button>
                </div>

                <div style={styles.metaData}>
                    <span style={styles.metaSpan}>
                        <strong>시장:</strong> {stockInfo.marketType}
                    </span>
                    <span style={styles.metaSpan}>
                        <strong>업종:</strong>{" "}
                        <Link to={`/industry/${stockInfo.industry || "ETF"}`}>
                            {stockInfo.industry || "ETF"}
                        </Link>
                    </span>
                    <span style={styles.metaSpan}>
                        <strong>시가총액:</strong> {stockInfo.marketCap}
                    </span>
                    <span style={styles.metaSpan}>
                        <strong>기준일:</strong> {stockInfo.updatedAt}
                    </span>
                </div>
            </div>

            {/* -------------------------- */}
            {/*   차트 섹션        */}
            {/* -------------------------- */}
            <StockChart 
                stockCode={stockCode} 
                rtPrice={rtPrice} 
                basePrice={Number(stockInfo.price)} 
            />

            {/* -------------------------- */}
            {/*   감성 분석 섹션            */}
            {/* -------------------------- */}
            <div style={styles.section}>
                <h3 style={styles.sectionTitle}>🤖 AI 뉴스 감성 분석</h3>
                <div style={styles.sentimentBarContainer}>
                    <div style={styles.barWrapper}>
                        <div style={{ width: `${sentiment?.positiveRate || 0}%`, backgroundColor: "#d60000" }} />
                        <div style={{ width: `${sentiment?.neutralRate || 0}%`, backgroundColor: "#999" }} />
                        <div style={{ width: `${sentiment?.negativeRate || 0}%`, backgroundColor: "#0051c7" }} />
                    </div>

                    <div style={styles.sentimentStats}>
                        <div style={{ color: "#d60000" }}>긍정 {sentiment?.positiveCount || 0}건</div>
                        <div style={{ color: "#0051c7" }}>부정 {sentiment?.negativeCount || 0}건</div>
                    </div>
                </div>
            </div>

            {/* -------------------------- */}
            {/*   뉴스 리스트               */}
            {/* -------------------------- */}
            <div style={styles.section}>
                <h3 style={styles.sectionTitle}>📰 관련 주요 뉴스</h3>

                {newsList?.length > 0 ? (
                    newsList.map((news) => {
                        const newsId = String(news.newsId || news.id);
                        const bookmark = savedBookmarks.find(b => b.newsId === newsId);
                        const isBookmarked = !!bookmark;
                        const isRead = bookmark?.isRead === "Y";

                        return (
                            <div key={newsId} style={styles.newsItemWrapper}>
                                <div style={styles.newsContent}>
                                    
                                    <a
                                        href={news.url}
                                        onClick={(e) => {
                                            e.preventDefault();
                                            handleNewsClick(newsId, news.url, isBookmarked);
                                        }}
                                        style={{
                                            ...styles.newsLink,
                                            color: isRead ? "#bbb" : "#333",
                                            textDecoration: isRead ? "line-through" : "none",
                                        }}
                                    >
                                        {news.title}
                                    </a>

                                    <div style={styles.newsSummary}>{news.content}</div>

                                    <div style={styles.newsInfo}>
                                        <span
                                            style={{
                                                ...styles.sentimentBadge,
                                                color:
                                                    news.sentiment === "긍정"
                                                        ? "#d60000"
                                                        : news.sentiment === "부정"
                                                        ? "#0051c7"
                                                        : "#666",
                                            }}
                                        >
                                            [{news.sentiment}]
                                        </span>
                                        <span>{news.newsDate}</span>
                                        <span>키워드: {news.keywords}</span>
                                    </div>
                                </div>

                                {/* 뉴스 찜 버튼 */}
                                <button
                                    onClick={() => handleToggleNewsBookmark(news)}
                                    style={{
                                        ...styles.newsStarButton,
                                        ...(isBookmarked ? styles.newsStarActive : {}),
                                    }}
                                >
                                    {isBookmarked ? "★" : "☆"}
                                </button>
                            </div>
                        );
                    })
                ) : (
                    <p style={styles.noNews}>관련 뉴스가 없습니다.</p>
                )}
            </div>
        </div>
    );
}

export default StockDetailPage;