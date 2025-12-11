import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

// ==========================================
// 1. 내부 컴포넌트: 심플 워드 클라우드
// ==========================================
const SimpleWordCloud = ({ words, onClick }) => {
  if (!words || words.length === 0) return null;
  const maxCount = Math.max(...words.map(w => w.count));
  const minCount = Math.min(...words.map(w => w.count));

  return (
    <div style={{
      display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: '12px',
      padding: '20px', background: '#f8f9fa', borderRadius: '12px'
    }}>
      {words.map((word, idx) => {
        const fontSize = 14 + ((word.count - minCount) / (maxCount - minCount || 1)) * 16;
        const opacity = 0.6 + Math.random() * 0.4;
        return (
          <span
            key={idx}
            onClick={() => onClick && onClick(word)}
            style={{
              fontSize: `${fontSize}px`, fontWeight: 'bold', color: `rgba(0, 123, 255, ${opacity})`,
              cursor: 'pointer', transition: 'transform 0.2s', userSelect: 'none'
            }}
            onMouseOver={(e) => e.target.style.transform = 'scale(1.1)'}
            onMouseOut={(e) => e.target.style.transform = 'scale(1)'}
            title={`${word.count}회 등장`}
          >
            {word.value}
          </span>
        );
      })}
    </div>
  );
};

// ==========================================
// 2. 스타일 객체 정의
// ==========================================
const styles = {
  container: {
    maxWidth: '1000px',
    margin: '50px auto',
    padding: '20px',
    fontFamily: 'sans-serif',
    position: 'relative',
  },
  header: {
    borderBottom: '2px solid #333',
    paddingBottom: '20px',
    marginBottom: '30px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: { fontSize: '2em', fontWeight: 'bold', color: '#333', margin: 0 },
  tabs: { display: 'flex', gap: '10px', marginBottom: '30px', borderBottom: '1px solid #ddd' },
  tabButton: (isActive) => ({
    padding: '12px 24px',
    cursor: 'pointer',
    fontSize: '16px',
    fontWeight: 'bold',
    border: 'none',
    background: 'none',
    color: isActive ? '#007bff' : '#666',
    borderBottom: isActive ? '3px solid #007bff' : '3px solid transparent',
    transition: 'all 0.2s',
  }),
  card: {
    background: 'white',
    padding: '30px',
    borderRadius: '12px',
    boxShadow: '0 2px 10px rgba(0,0,0,0.05)',
    marginBottom: '20px',
    border: '1px solid #eee',
  },
  chartSection: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginBottom: '30px',
    padding: '20px',
    backgroundColor: '#fff',
    borderRadius: '12px',
    border: '1px solid #eee',
    boxShadow: '0 2px 10px rgba(0,0,0,0.05)',
  },
  chartTitle: {
    fontSize: '1.2rem',
    fontWeight: 'bold',
    marginBottom: '20px',
    color: '#333'
  },
  row: { display: 'flex', marginBottom: '15px', alignItems: 'center' },
  label: { width: '120px', fontWeight: 'bold', color: '#555' },
  value: { flex: 1, color: '#333' },
  input: { padding: '8px', border: '1px solid #ddd', borderRadius: '4px', width: '200px' },
  btnGroup: { marginTop: '20px', display: 'flex', gap: '10px' },
  btnPrimary: { padding: '10px 20px', background: '#007bff', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
  btnSecondary: { padding: '10px 20px', background: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
  btnDanger: { padding: '10px 20px', background: '#dc3545', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
  btnDelete: { padding: '5px 10px', background: '#fff', border: '1px solid #dc3545', color: '#dc3545', borderRadius: '4px', cursor: 'pointer', fontSize: '12px' },
  
  listItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    padding: '15px',
    borderBottom: '1px solid #eee',
  },
  stockNameLink: { fontWeight: 'bold', fontSize: '18px', color: '#333', textDecoration: 'none', cursor: 'pointer' },
  stockCode: { fontSize: '12px', color: '#999', marginLeft: '8px' },
  stockPrice: { color: '#d60000', fontWeight: 'bold' },
  stockIndustry: { fontSize: '12px', color: '#007bff', backgroundColor: '#eef4ff', padding: '2px 6px', borderRadius: '4px', marginLeft: '10px' },
  
  newsTitle: { textDecoration: 'none', fontSize: '16px', fontWeight: '500', display: 'block', marginBottom: '5px', cursor: 'pointer' },
  newsDate: { fontSize: '12px', color: '#888' },

  memoBtn: { background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', marginLeft: '10px', color: '#888', transition: 'color 0.2s' },
  memoDisplay: { marginTop: '8px', fontSize: '13px', color: '#666', background: '#f8f9fa', padding: '8px', borderRadius: '6px', borderLeft: '3px solid #007bff', whiteSpace: 'pre-wrap' },
  
  modalOverlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 },
  modalContent: { background: 'white', padding: '25px', borderRadius: '12px', width: '400px', boxShadow: '0 4px 15px rgba(0,0,0,0.2)' },
  modalTitle: { fontSize: '1.2rem', fontWeight: 'bold', marginBottom: '15px' },
  modalTextarea: { width: '100%', height: '100px', padding: '10px', border: '1px solid #ddd', borderRadius: '4px', resize: 'none', marginBottom: '15px', fontFamily: 'inherit' },
  modalActions: { display: 'flex', justifyContent: 'flex-end', gap: '10px' },
};

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d', '#ffc658'];

// ==========================================
// 3. 컴포넌트 로직
// ==========================================

function MyPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('INFO');
  const [userInfo, setUserInfo] = useState(null);
  const [favorites, setFavorites] = useState({ stocks: [], news: [] });
  
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ fullName: '' });

  const [memoModal, setMemoModal] = useState({
      isOpen: false, type: null, id: null, content: ''
  });

  const [chartData, setChartData] = useState([]);
  const [wordCloudData, setWordCloudData] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
             alert("로그인이 필요합니다.");
             navigate('/');
             return;
        }

        const res = await axios.get('/api/mypage/info', {
            headers: { Authorization: `Bearer ${token}` }
        });
        
        if (res.status === 200) {
            setUserInfo(res.data.user);
            const stocks = res.data.stocks || [];
            const news = res.data.news || [];
            
            setFavorites({ stocks, news });
            setEditForm({ fullName: res.data.user.fullName });

            // 1. 차트 데이터 생성
            const industryCount = stocks.reduce((acc, stock) => {
                const industry = stock.industry || '기타(ETF 등)';
                acc[industry] = (acc[industry] || 0) + 1;
                return acc;
            }, {});
            const dataForChart = Object.keys(industryCount).map(key => ({
                name: key, value: industryCount[key]
            })).sort((a, b) => b.value - a.value);
            setChartData(dataForChart);

            // 2. 워드클라우드 데이터 생성 (개선된 로직 사용)
            generateWordCloud(news);
        }

      } catch (error) {
        console.error("데이터 로드 실패", error);
        alert("정보를 불러올 수 없습니다.");
        navigate('/');
      }
    };
    fetchData();
  }, [navigate]);

  // ⭐ [개선됨] 워드클라우드 생성 함수
  const generateWordCloud = (newsList) => {
      const wordMap = {};
      const stopWords = ['뉴스', '속보', '특징주', '종합', '오늘', '내일', '마감', '상승', '하락', '급등', '급락', '포토', '영상', 'Why', '코스피', '코스닥', '공시', '단독', '주가', '전망'];

      newsList.forEach(news => {
          const title = news.newsTitle || news.title || "";
          
          // 1. 특수문자를 공백으로 치환 (가운데 점(·), 쉼표, 괄호 등)
          // "삼성전자·현대차" -> "삼성전자 현대차" 로 분리됨
          let cleanTitle = title.replace(/[\[\]\(\)\,\.\'\"·…!/?&~@#$%^&*_=+\-|<>]/g, ' ');

          // 2. 공백 기준 분리
          const words = cleanTitle.split(/\s+/);
          
          words.forEach(word => {
              let cleanWord = word;

              // 3. 한국어 조사 제거 (2글자 이상인 경우만)
              // "삼성전자와" -> "삼성전자", "반도체는" -> "반도체"
              if (cleanWord.length > 2) {
                  const lastChar = cleanWord.slice(-1);
                  // 흔한 조사 목록
                  if (['은', '는', '이', '가', '을', '를', '의', '와', '과', '로', '에', '도'].includes(lastChar)) {
                       cleanWord = cleanWord.slice(0, -1);
                  }
              }

              // 4. 유효성 체크 및 카운팅
              if (cleanWord.length > 1 && !stopWords.includes(cleanWord)) {
                  // (선택) 동의어 처리: '삼성' -> '삼성전자' 로 합치기
                  if (cleanWord === '삼성') cleanWord = '삼성전자';
                  
                  wordMap[cleanWord] = (wordMap[cleanWord] || 0) + 1;
              }
          });
      });

      const cloudData = Object.keys(wordMap)
          .map(key => ({ value: key, count: wordMap[key] }))
          .sort((a, b) => b.count - a.count)
          .slice(0, 30);

      setWordCloudData(cloudData);
  };

  const handleUpdate = async () => {
    if (!editForm.fullName.trim()) { alert("이름을 입력해주세요."); return; }
    if (!window.confirm("정보를 수정하시겠습니까?")) return;
    try {
        const token = localStorage.getItem('accessToken');
        await axios.put('/api/mypage/update', editForm, { headers: { Authorization: `Bearer ${token}` } });
        const currentUser = JSON.parse(localStorage.getItem('user'));
        localStorage.setItem('user', JSON.stringify({ ...currentUser, fullName: editForm.fullName }));
        alert("정보가 수정되었습니다.");
        setIsEditing(false);
        window.location.reload(); 
    } catch (e) { console.error(e); alert("수정 실패"); }
  };

  const handleWithdraw = async () => {
    if (window.confirm("정말로 탈퇴하시겠습니까?")) {
        try {
            const token = localStorage.getItem('accessToken');
            await axios.delete('/api/mypage/withdraw', { headers: { Authorization: `Bearer ${token}` } });
            localStorage.clear();
            alert("탈퇴 완료");
            navigate('/');
            window.location.reload();
        } catch (e) { alert("탈퇴 실패"); }
    }
  };

  const handleDeleteStock = async (stockCode) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      const token = localStorage.getItem('accessToken');
      await axios.delete(`/api/mypage/favorites/stock/${stockCode}`, { headers: { Authorization: `Bearer ${token}` } });
      setFavorites(prev => ({ ...prev, stocks: prev.stocks.filter(s => s.stockCode !== stockCode) }));
    } catch (e) { alert("삭제 실패"); }
  };

  const handleDeleteNews = async (newsId) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      const token = localStorage.getItem('accessToken');
      await axios.delete(`/api/mypage/favorites/news/${newsId}`, { headers: { Authorization: `Bearer ${token}` } });
      setFavorites(prev => ({ ...prev, news: prev.news.filter(n => n.newsId !== newsId) }));
      // 삭제 후 클라우드 다시 계산
      generateWordCloud(favorites.news.filter(n => n.newsId !== newsId));
    } catch (e) { alert("삭제 실패"); }
  };

  const handleNewsClick = async (newsId, url) => {
    window.open(url, '_blank', 'noopener,noreferrer');
    const token = localStorage.getItem('accessToken');
    if (token) {
        try {
            await axios.post('/api/mypage/favorites/news/read', { newsId: newsId }, { headers: { Authorization: `Bearer ${token}` } });
            setFavorites(prev => ({
                ...prev,
                news: prev.news.map(n => n.newsId === newsId ? { ...n, isRead: 'Y' } : n)
            }));
        } catch (e) { console.error("읽음 처리 실패", e); }
    }
  };

  const openMemoModal = (type, id, currentMemo) => {
      setMemoModal({ isOpen: true, type: type, id: id, content: currentMemo || '' });
  };

  const handleSaveMemo = async () => {
      const token = localStorage.getItem('accessToken');
      const { type, id, content } = memoModal;
      try {
          const endpoint = type === 'STOCK' ? '/api/mypage/favorites/stock/memo' : '/api/mypage/favorites/news/memo';
          const payload = type === 'STOCK' ? { stockCode: id, memo: content } : { newsId: id, memo: content };
          
          await axios.post(endpoint, payload, { headers: { Authorization: `Bearer ${token}` } });
          
          setFavorites(prev => {
              if (type === 'STOCK') {
                  return { ...prev, stocks: prev.stocks.map(s => s.stockCode === id ? { ...s, memo: content } : s) };
              } else {
                  return { ...prev, news: prev.news.map(n => n.newsId === id ? { ...n, memo: content } : n) };
              }
          });
          setMemoModal({ ...memoModal, isOpen: false });
      } catch (e) { alert("메모 저장 오류"); }
  };

  if (!userInfo) return <div style={{textAlign:'center', marginTop:'50px'}}>로딩중...</div>;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>마이페이지</h1>
      </div>

      <div style={styles.tabs}>
        <button style={styles.tabButton(activeTab === 'INFO')} onClick={() => setActiveTab('INFO')}>내 정보</button>
        <button style={styles.tabButton(activeTab === 'STOCK')} onClick={() => setActiveTab('STOCK')}>관심 종목</button>
        <button style={styles.tabButton(activeTab === 'NEWS')} onClick={() => setActiveTab('NEWS')}>스크랩 뉴스</button>
      </div>

      {activeTab === 'INFO' && (
        <div style={styles.card}>
            <div style={styles.row}>
                <span style={styles.label}>이메일</span>
                <span style={styles.value}>{userInfo.email}</span>
            </div>
            <div style={styles.row}>
                <span style={styles.label}>이름</span>
                {isEditing ? (
                    <input style={styles.input} value={editForm.fullName} 
                        onChange={(e) => setEditForm({...editForm, fullName: e.target.value})} />
                ) : (
                    <span style={styles.value}>{userInfo.fullName}</span>
                )}
            </div>
            <div style={styles.btnGroup}>
                {isEditing ? (
                    <>
                        <button style={styles.btnPrimary} onClick={handleUpdate}>저장</button>
                        <button style={styles.btnSecondary} onClick={() => setIsEditing(false)}>취소</button>
                    </>
                ) : (
                    <>
                        <button style={styles.btnPrimary} onClick={() => setIsEditing(true)}>정보 수정</button>
                        <button style={styles.btnPrimary} onClick={() => navigate('/find-pw')}>비밀번호 변경</button>
                        <button style={styles.btnDanger} onClick={handleWithdraw}>회원 탈퇴</button>
                    </>
                )}
            </div>
        </div>
      )}

      {activeTab === 'STOCK' && (
          <>
            {favorites.stocks.length > 0 && (
                <div style={styles.chartSection}>
                    <div style={styles.chartTitle}>📊 내 포트폴리오 업종 분석</div>
                    <div style={{ width: '100%', height: 300 }}>
                        <ResponsiveContainer>
                            <PieChart>
                                <Pie data={chartData} cx="50%" cy="50%" labelLine={false}
                                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                                    outerRadius={100} fill="#8884d8" dataKey="value"
                                >
                                    {chartData.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip formatter={(value) => `${value}개`} />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            )}

            <div style={styles.card}>
                {favorites.stocks.length === 0 ? <p style={{color:'#888'}}>찜한 종목이 없습니다.</p> : 
                    favorites.stocks.map((stock, idx) => (
                        <div key={idx} style={styles.listItem}>
                            <div style={{flex: 1}}>
                                <div style={{display:'flex', alignItems:'center', flexWrap:'wrap', gap:'5px'}}>
                                    <Link to={`/stock/${stock.stockCode}`} style={styles.stockNameLink}>{stock.stockName}</Link>
                                    <span style={styles.stockCode}>{stock.stockCode}</span>
                                    <span style={styles.stockIndustry}>{stock.industry || 'ETF/기타'}</span>
                                    <button style={styles.memoBtn} onClick={() => openMemoModal('STOCK', stock.stockCode, stock.memo)}>✏️</button>
                                </div>
                                {stock.memo && <div style={styles.memoDisplay}>📝 {stock.memo}</div>}
                            </div>
                            <div style={{display:'flex', alignItems:'center', gap:'15px'}}>
                                <span style={styles.stockPrice}>{stock.price ? stock.price.toLocaleString() : '-'}원</span>
                                <button style={styles.btnDelete} onClick={() => handleDeleteStock(stock.stockCode)}>삭제</button>
                            </div>
                        </div>
                    ))
                }
            </div>
          </>
      )}

      {activeTab === 'NEWS' && (
          <>
            {favorites.news.length > 0 && wordCloudData.length > 0 && (
                <div style={styles.chartSection}>
                    <div style={styles.chartTitle}>☁️ 나의 뉴스 관심 키워드</div>
                    <SimpleWordCloud 
                        words={wordCloudData} 
                        onClick={word => alert(`'${word.value}' 키워드가 ${word.count}번 등장했습니다.`)}
                    />
                </div>
            )}

            <div style={styles.card}>
                {favorites.news.length === 0 ? <p style={{color:'#888'}}>스크랩한 뉴스가 없습니다.</p> : 
                    favorites.news.map((news, idx) => {
                        const isRead = news.isRead === 'Y';
                        return (
                            <div key={idx} style={styles.listItem}>
                                <div style={{flex:1, paddingRight:'20px'}}>
                                    <div style={{display:'flex', alignItems:'center'}}>
                                        <a href={news.newsUrl} 
                                            onClick={(e) => { e.preventDefault(); handleNewsClick(news.newsId, news.newsUrl); }}
                                            style={{ ...styles.newsTitle, color: isRead ? '#bbb' : '#333', textDecoration: isRead ? 'line-through' : 'none', marginBottom: 0 }}
                                        >
                                            {news.newsTitle}
                                        </a>
                                        <button style={styles.memoBtn} onClick={() => openMemoModal('NEWS', news.newsId, news.memo)}>✏️</button>
                                    </div>
                                    <div style={styles.newsDate}>{news.newsDate}</div>
                                    {news.memo && <div style={styles.memoDisplay}>📝 {news.memo}</div>}
                                </div>
                                <button style={styles.btnDelete} onClick={() => handleDeleteNews(news.newsId)}>삭제</button>
                            </div>
                        );
                    })
                }
            </div>
          </>
      )}

      {memoModal.isOpen && (
          <div style={styles.modalOverlay} onClick={() => setMemoModal({...memoModal, isOpen: false})}>
              <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                  <div style={styles.modalTitle}>{memoModal.type === 'STOCK' ? '📈 종목 메모' : '📰 뉴스 메모'}</div>
                  <textarea style={styles.modalTextarea} placeholder="내용을 입력하세요..." value={memoModal.content} onChange={(e) => setMemoModal({...memoModal, content: e.target.value})} />
                  <div style={styles.modalActions}>
                      <button style={styles.btnSecondary} onClick={() => setMemoModal({...memoModal, isOpen: false})}>취소</button>
                      <button style={styles.btnPrimary} onClick={handleSaveMemo}>저장</button>
                  </div>
              </div>
          </div>
      )}
    </div>
  );
}

export default MyPage;