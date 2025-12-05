import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios'; 

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

// 드롭다운 관련 스타일
const UserProfile = styled.div`
  position: relative; /* 드롭다운 위치 기준점 */
  cursor: pointer;
  font-weight: bold;
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 20px;
  transition: background 0.2s;

  &:hover {
    background-color: #f1f1f1;
  }
`;

const DropdownMenu = styled.div`
  position: absolute;
  top: 120%; /* 이름 바로 아래 */
  right: 0;
  width: 160px;
  background-color: white;
  border: 1px solid #eee;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  overflow: hidden;
  z-index: 100;
  display: flex;
  flex-direction: column;
`;

const DropdownItem = styled.button`
  background: white;
  border: none;
  padding: 12px 15px;
  text-align: left;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  border-bottom: 1px solid #f9f9f9;

  &:hover {
    background-color: #f8f9fa;
    color: var(--primary-blue);
  }

  &:last-child {
    border-bottom: none;
    color: #dc3545; /* 로그아웃은 빨간색 느낌 */
  }
`;


function Header() {
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  
  const [user, setUser] = useState(null);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  // 로그아웃
  const handleLogout = async () => {
    if (!user || !user.email) {
        localStorage.clear();
        setUser(null);
        setIsDropdownOpen(false);
        navigate('/');
        return;
    }

    try {
        // 백엔드에 로그아웃 요청 (JSON 바디)
        await axios.post('/auth/logout', { 
            email: user.email 
        });
        console.log("서버 로그아웃 성공");
    } catch (error) {
        console.error("서버 로그아웃 실패:", error);
    } finally {
        // 로컬 토큰 모두 삭제
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        
        setUser(null);
        setIsDropdownOpen(false);
        alert('로그아웃되었습니다.');
        navigate('/');
    }
  };

  const openModal = () => setIsModalOpen(true);
  const closeModal = () => setIsModalOpen(false);

  const toggleDropdown = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

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
            <UserProfile onClick={toggleDropdown}>
              {/* 이름 클릭 영역 */}
              <span>{user.fullName || user.email}님 ▼</span>
              
              {/* 드롭다운 메뉴 */}
              {isDropdownOpen && (
                <DropdownMenu>
                  <DropdownItem onClick={() => navigate('/dashboard')}>
                    마이페이지
                  </DropdownItem>
                  <DropdownItem onClick={() => navigate('/find-pw')}>
                    비밀번호 변경
                  </DropdownItem>
                  <DropdownItem onClick={handleLogout}>
                    로그아웃
                  </DropdownItem>
                </DropdownMenu>
              )}
            </UserProfile>
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