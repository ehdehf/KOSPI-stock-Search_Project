import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

// 스타일 객체 정의 (styled-components 대체)
const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: 'calc(100vh - 80px)',
    backgroundColor: '#f8f9fa',
  },
  box: {
    width: '450px',
    padding: '40px',
    backgroundColor: 'white',
    borderRadius: '8px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
    textAlign: 'center',
  },
  h2: {
    marginBottom: '20px',
    color: '#333',
  },
  input: {
    width: '100%',
    padding: '12px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    marginBottom: '15px',
    boxSizing: 'border-box',
  },
  button: {
    width: '100%',
    padding: '12px',
    backgroundColor: '#007bff', // 기본 파란색
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    fontSize: '16px',
    fontWeight: 'bold',
    cursor: 'pointer',
    marginTop: '10px',
  }
};

function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [isValidToken, setIsValidToken] = useState(false); // 토큰 유효성 상태
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  
  const isRun = useRef(false); // 중복 실행 방지

  // 1. 페이지 로드 시 토큰 검증
  useEffect(() => {
    if (isRun.current) return;
    if (!token) {
      alert("잘못된 접근입니다.");
      navigate('/');
      return;
    }

    const verifyToken = async () => {
      isRun.current = true;
      try {
        // 명세서: GET /auth/reset/verify?token=...
        const response = await axios.get(`/auth/reset/verify?token=${token}`);
        if (response.status === 200) {
          setIsValidToken(true); // 유효함 -> 입력창 보여주기
        }
      } catch (error) {
        console.error("토큰 검증 실패:", error);
        alert("유효하지 않거나 만료된 링크입니다.");
        navigate('/');
      }
    };

    verifyToken();
  }, [token, navigate]);

  // 2. 비밀번호 변경 요청
  const handleReset = async () => {
    if (newPassword !== confirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      // 명세서: POST /auth/reset/confirm (JSON Body)
      await axios.post('/auth/reset/confirm', {
        token: token,
        newPassword: newPassword
      });

      alert("비밀번호가 성공적으로 변경되었습니다.\n새 비밀번호로 로그인해주세요.");
      navigate('/'); // 메인으로 이동

    } catch (error) {
      console.error("변경 실패:", error);
      alert("비밀번호 변경에 실패했습니다.");
    }
  };

  if (!isValidToken) {
    return <div style={styles.container}>토큰 검증 중...</div>;
  }

  return (
    <div style={styles.container}>
      <div style={styles.box}>
        <h2 style={styles.h2}>새 비밀번호 설정</h2>
        <input 
          type="password" 
          placeholder="새 비밀번호" 
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          style={styles.input}
        />
        <input 
          type="password" 
          placeholder="새 비밀번호 확인" 
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          style={styles.input}
        />
        <button onClick={handleReset} style={styles.button}>비밀번호 변경</button>
      </div>
    </div>
  );
}

export default ResetPasswordPage;