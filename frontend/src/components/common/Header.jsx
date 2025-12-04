import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios'; // ⭐ axios 임포트 필수!

// 경로 확인 (auth 폴더인지 common 폴더인지)
import LoginModal from '../auth/LoginModal'; 

const StyledHeader = styled.header`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background-color: #ffffff;
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
`;

const Logo = styled(Link)`
  font-size: 24px;
  font-weight: bold;
  color: var(--primary-blue);
  text-decoration: none;
  margin-right: 30px;
`;

const NavMenu = styled.nav`
  display: flex;
  gap: 25px;
  margin-right: auto;
  a {
    color: var(--text-dark);
    font-weight: 500;
    text-decoration: none;
    transition: color 0.2s;
    &:hover { color: var(--primary-blue); }
  }
`;

const SearchContainer = styled.div`
  display: flex;
  align-items: center;
  margin-right: 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 5px 15px;
  background-color: #f9f9f9;
`;

const SearchInput = styled.input`
  border: none;
  background: none;
  outline: none;
  padding: 5px;
  font-size: 14px;
  width: 150px;
`;

const SearchBtn = styled.button`
  border: none;
  background: none;
  cursor: pointer;
  font-weight: bold;
  color: #666;
  &:hover { color: var(--primary-blue); }
`;

const AuthContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`;

const LoginButton = styled.button`
  padding: 8px 20px;
  background-color: var(--primary-blue);
  color: #ffffff;
  border-radius: 20px;
  border: none;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.2s;
  &:hover { opacity: 0.9; }
`;

const LogoutButton = styled(LoginButton)`
  background-color: #6c757d; 
`;

function Header() {
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  
  const [user, setUser] = useState(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  // ⭐ [수정됨] 백엔드 API 호출을 포함한 로그아웃
  const handleLogout = async () => {
    if (!user || !user.email) {
        // 유저 정보가 없으면 그냥 로컬만 지우고 끝냄
        localStorage.clear();
        setUser(null);
        navigate('/');
        return;
    }

    try {
        // 1. 백엔드에 로그아웃 요청 (Refresh Token 삭제용)
        // 명세서: POST /auth/logout, param: email
        // (이메일 중복확인 때처럼 params로 보냄)
        await axios.post('/auth/logout', null, {
            params: { email: user.email }
        });
        
        console.log("서버 로그아웃 성공");

    } catch (error) {
        console.error("서버 로그아웃 실패 (그래도 클라이언트는 로그아웃 처리함):", error);
    } finally {
        // 2. 성공하든 실패하든 브라우저의 정보는 싹 지워야 함
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        
        setUser(null);
        alert('로그아웃되었습니다.'); // 명세서 메시지와 일치시킴
        navigate('/');
    }
  };

  const openModal = () => setIsModalOpen(true);
  const closeModal = () => setIsModalOpen(false);

  const handleSearch = () => {
    if (keyword.trim()) {
      navigate(`/search?keyword=${keyword.trim()}`);
    } else {
      alert("검색어를 입력해주세요.");
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') handleSearch();
  };

  return (
    <>
      <StyledHeader>
        <Logo to="/">K-Stock Insight</Logo>
        
        <NavMenu>
          <Link to="/">홈</Link>
          <Link to="/dashboard">감성 대시보드</Link>
          <Link to="/trend">키워드 트렌드</Link>
          <Link to="/marketcap">시총 순위</Link>
        </NavMenu>

        <AuthContainer>
          <SearchContainer>
            <SearchInput 
              type="text" 
              placeholder="종목 검색"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyPress={handleKeyPress}
            />
            <SearchBtn onClick={handleSearch}>🔍</SearchBtn>
          </SearchContainer>

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span style={{ fontSize: '14px', fontWeight: 'bold' }}>
                {user.fullName || user.email}님
              </span>
              <LogoutButton onClick={handleLogout}>로그아웃</LogoutButton>
            </div>
          ) : (
            <LoginButton onClick={openModal}>로그인</LoginButton>
          )}

        </AuthContainer>
      </StyledHeader>

      {isModalOpen && <LoginModal onClose={closeModal} />}
    </>
  );
}

export default Header;