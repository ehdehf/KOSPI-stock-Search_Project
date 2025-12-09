import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';

// ==========================================
// 1. 스타일 객체 정의 (Inline Styles)
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
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: '2em',
    fontWeight: 'bold',
    color: '#333',
    margin: 0,
  },
  tabs: {
    display: 'flex',
    gap: '10px',
    marginBottom: '30px',
    borderBottom: '1px solid #ddd',
  },
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
  row: {
    display: 'flex',
    marginBottom: '15px',
    alignItems: 'center',
  },
  label: {
    width: '120px',
    fontWeight: 'bold',
    color: '#555',
  },
  value: {
    flex: 1,
    color: '#333',
  },
  input: {
    padding: '8px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    width: '200px',
  },
  btnGroup: {
    marginTop: '20px',
    display: 'flex',
    gap: '10px',
  },
  btnPrimary: {
    padding: '10px 20px',
    background: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
  btnSecondary: {
    padding: '10px 20px',
    background: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
  btnDanger: {
    padding: '10px 20px',
    background: '#dc3545',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
  btnDelete: {
    padding: '5px 10px',
    background: '#fff',
    border: '1px solid #dc3545',
    color: '#dc3545',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '12px',
  },
  listItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '15px',
    borderBottom: '1px solid #eee',
  },
  stockNameLink: {
    fontWeight: 'bold',
    fontSize: '18px',
    color: '#333',
    textDecoration: 'none',
    transition: 'color 0.2s',
    cursor: 'pointer',
  },
  stockCode: {
    fontSize: '12px',
    color: '#999',
    marginLeft: '8px',
  },
  stockPrice: {
    color: '#d60000',
    fontWeight: 'bold',
  },
  newsTitle: {
    textDecoration: 'none',
    color: '#333',
    fontSize: '16px',
    fontWeight: '500',
    display: 'block',
    marginBottom: '5px',
  },
  newsDate: {
    fontSize: '12px',
    color: '#888',
  },
};

// ==========================================
// 2. 컴포넌트 로직
// ==========================================

function MyPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('INFO');
  const [userInfo, setUserInfo] = useState(null);
  const [favorites, setFavorites] = useState({ stocks: [], news: [] });
  
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ fullName: '' }); // 이름만 수정

  // 1. 데이터 불러오기 (실제 API 호출)
  useEffect(() => {
    const fetchData = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
             alert("로그인이 필요합니다.");
             navigate('/');
             return;
        }

        const res = await axios.get('/api/mypage/info');
        
        if (res.status === 200) {
            setUserInfo(res.data.user);
            setFavorites({ 
                stocks: res.data.stocks || [], 
                news: res.data.news || [] 
            });
            // 수정 폼 초기값 설정
            setEditForm({ fullName: res.data.user.fullName });
        }

      } catch (error) {
        console.error("데이터 로드 실패", error);
        alert("정보를 불러올 수 없습니다. 다시 로그인해주세요.");
        navigate('/');
      }
    };
    fetchData();
  }, [navigate]);

  // 2. 정보 수정 핸들러 (이름 수정 + 로컬 스토리지 갱신)
  const handleUpdate = async () => {
    if (!editForm.fullName.trim()) {
        alert("이름을 입력해주세요.");
        return;
    }
    
    if (!window.confirm("정보를 수정하시겠습니까?")) return;
    
    try {
        // 백엔드 DB 업데이트
        await axios.put('/api/mypage/update', editForm);

        // ⭐ [핵심 추가] 내 브라우저의 명찰(localStorage)도 새 이름으로 바꿔주기
        const currentUser = JSON.parse(localStorage.getItem('user'));
        const newUserInfo = { ...currentUser, fullName: editForm.fullName };
        localStorage.setItem('user', JSON.stringify(newUserInfo));

        alert("정보가 수정되었습니다.");
        setIsEditing(false);
        
        // 화면 새로고침 (이제 Header가 바뀐 localStorage를 읽어올 겁니다)
        window.location.reload(); 
    } catch (e) {
        console.error(e);
        alert("수정 실패");
    }
  };

  // 3. 회원 탈퇴 핸들러
  const handleWithdraw = async () => {
    if (window.confirm("정말로 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
        try {
            await axios.delete('/api/mypage/withdraw');
            
            // 로컬 스토리지 비우기
            localStorage.clear();
            
            alert("탈퇴가 완료되었습니다.");
            navigate('/');
            window.location.reload();
        } catch (e) {
            alert("탈퇴 실패");
        }
    }
  };

  // 4. 종목 찜 해제
  const handleDeleteStock = async (stockCode) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await axios.delete(`/api/mypage/favorites/stock/${stockCode}`);
      alert("삭제되었습니다.");
      window.location.reload(); 
    } catch (e) {
      alert("삭제 실패");
    }
  };

  // 5. 뉴스 스크랩 해제
  const handleDeleteNews = async (newsId) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await axios.delete(`/api/mypage/favorites/news/${newsId}`);
      alert("삭제되었습니다.");
      window.location.reload();
    } catch (e) {
      alert("삭제 실패");
    }
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

      {/* 1. 내 정보 탭 */}
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
                        <button style={styles.btnDanger} onClick={handleWithdraw}>회원 탈퇴</button>
                    </>
                )}
            </div>
        </div>
      )}

      {/* 2. 관심 종목 탭 */}
      {activeTab === 'STOCK' && (
          <div style={styles.card}>
              {favorites.stocks.length === 0 ? <p style={{color:'#888'}}>찜한 종목이 없습니다.</p> : 
                favorites.stocks.map((stock, idx) => (
                    <div key={idx} style={styles.listItem}>
                        <div>
                            <Link 
                                to={`/stock/${stock.stockCode}`}
                                style={styles.stockNameLink}
                                onMouseOver={(e) => {
                                    e.currentTarget.style.textDecoration = 'underline';
                                    e.currentTarget.style.color = '#007bff';
                                }}
                                onMouseOut={(e) => {
                                    e.currentTarget.style.textDecoration = 'none';
                                    e.currentTarget.style.color = '#333';
                                }}
                            >
                                {stock.stockName}
                            </Link>
                            <span style={styles.stockCode}>
                                {stock.stockCode}
                            </span>
                        </div>
                        <div style={{display:'flex', alignItems:'center', gap:'15px'}}>
                            <span style={styles.stockPrice}>
                                {stock.price ? stock.price.toLocaleString() : '-'}원
                            </span>
                            <button style={styles.btnDelete} onClick={() => handleDeleteStock(stock.stockCode)}>삭제</button>
                        </div>
                    </div>
                ))
              }
          </div>
      )}

      {/* 3. 스크랩 뉴스 탭 */}
      {activeTab === 'NEWS' && (
          <div style={styles.card}>
              {favorites.news.length === 0 ? <p style={{color:'#888'}}>스크랩한 뉴스가 없습니다.</p> : 
                favorites.news.map((news, idx) => (
                    <div key={idx} style={styles.listItem}>
                        <div style={{flex:1}}>
                            <a href={news.newsUrl} target="_blank" rel="noopener noreferrer" style={styles.newsTitle}>{news.newsTitle}</a>
                            <div style={styles.newsDate}>{news.newsDate}</div>
                        </div>
                        <button style={styles.btnDelete} onClick={() => handleDeleteNews(news.newsId)}>삭제</button>
                    </div>
                ))
              }
          </div>
      )}
    </div>
  );
}

export default MyPage;