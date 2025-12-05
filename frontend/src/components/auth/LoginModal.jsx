import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

// 스타일 객체 (라이브러리 없이 작동)
const styles = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000,
  },
  content: {
    backgroundColor: 'white',
    padding: '40px',
    borderRadius: '10px',
    width: '400px',
    boxShadow: '0 5px 15px rgba(0,0,0,0.3)',
    position: 'relative',
    textAlign: 'center',
  },
  closeBtn: {
    position: 'absolute',
    top: '15px',
    right: '15px',
    background: 'none',
    border: 'none',
    fontSize: '20px',
    cursor: 'pointer',
    color: '#666',
  },
  input: {
    width: '100%',
    padding: '12px',
    marginBottom: '15px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    boxSizing: 'border-box',
  },
  button: {
    width: '100%',
    padding: '12px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontWeight: 'bold',
    fontSize: '16px',
  },
  footer: {
    marginTop: '25px',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px', // 두 줄 사이의 간격
  },
  // ⭐ [핵심] 두 줄에 공통으로 적용될 스타일 (완벽 통일)
  footerRow: {
    fontSize: '13px',
    color: '#666',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    gap: '5px' // 글자와 링크 사이 간격
  },
  link: {
    color: '#007bff',
    cursor: 'pointer',
    fontWeight: 'bold',
    textDecoration: 'none'
  }
};

function LoginModal({ onClose }) {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', password: '' });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.email || !formData.password) {
      alert("이메일과 비밀번호를 입력해주세요.");
      return;
    }

    try {
      const response = await axios.post('/auth/login', {
        email: formData.email,
        password: formData.password
      });

      if (response.status === 200) {
        const { accessToken, refreshToken, token } = response.data;
        const user = response.data.user || response.data.userInfo;

        // 토큰 저장 (Access + Refresh)
        localStorage.setItem('accessToken', accessToken || token);
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken);

        // 유저 정보 저장
        const userInfo = user || { email: formData.email, fullName: '회원' };
        localStorage.setItem('user', JSON.stringify(userInfo));

        alert("로그인 성공!");
        onClose();
        window.location.reload(); 
      }

    } catch (error) {
      console.error("로그인 실패:", error);
      if (error.response) {
        const status = error.response.status;
        const msg = error.response.data;

        // 이메일 미인증 처리 (403)
        if (status === 403 && typeof msg === 'string' && msg.includes('이메일 인증')) {
            if (window.confirm(`${msg}\n\n지금 인증 코드를 입력하시겠습니까?`)) {
                onClose();
                navigate(`/verify-email?email=${formData.email}`);
            }
            return;
        }
        // 비밀번호 틀림 처리 (401)
        if (status === 401) {
            alert("아이디 또는 비밀번호가 일치하지 않습니다.");
            return;
        }
        alert(typeof msg === 'string' ? msg : "로그인에 실패했습니다.");
      } else {
        alert("서버와 연결할 수 없습니다.");
      }
    }
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) onClose();
  };

  return (
    <div style={styles.overlay} onClick={handleOverlayClick}>
      <div style={styles.content}>
        <button style={styles.closeBtn} onClick={onClose}>X</button>
        <h2 style={{ marginBottom: '20px', color: '#333' }}>로그인</h2>
        
        <form onSubmit={handleSubmit}>
          <input 
            type="email" name="email" placeholder="이메일"
            value={formData.email} onChange={handleChange}
            style={styles.input}
          />
          <input 
            type="password" name="password" placeholder="비밀번호"
            value={formData.password} onChange={handleChange}
            style={styles.input}
          />
          <button type="submit" style={styles.button}>로그인</button>
        </form>

        {/* ⭐ 하단 링크 영역 (스타일 통일됨) */}
        <div style={styles.footer}>
            {/* 줄 1: 회원가입 */}
            <div style={styles.footerRow}>
                <span>계정이 없으신가요?</span>
                <span 
                  style={styles.link}
                  onClick={() => { onClose(); navigate('/signup'); }}
                >
                  회원가입
                </span>
            </div>
            
            {/* 줄 2: 비밀번호 찾기 */}
            <div style={styles.footerRow}>
                <span>비밀번호를 잊으셨나요?</span>
                <span 
                  style={styles.link}
                  onClick={() => { onClose(); navigate('/find-pw'); }}
                >
                  비밀번호 찾기
                </span>
            </div>
        </div>

      </div>
    </div>
  );
}

export default LoginModal;