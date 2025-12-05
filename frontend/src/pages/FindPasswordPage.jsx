import React, { useState } from 'react';
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
  title: {
    marginBottom: '20px',
    color: '#333',
  },
  description: {
    color: '#666',
    marginBottom: '30px',
    fontSize: '14px',
    lineHeight: '1.5',
  },
  input: {
    width: '100%',
    padding: '12px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '14px',
    marginBottom: '20px',
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
  }
};

function FindPasswordPage() {
  const [email, setEmail] = useState('');

const handleSubmit = async () => {
    if (!email) {
      alert("이메일을 입력해주세요.");
      return;
    }

    try {
      const response = await axios.post('/auth/reset/request', { email });
      
      // 👇 [수정] response.data가 바로 백엔드에서 보낸 그 문구입니다!
      // alert(`[${email}] 주소로 재설정 링크를 보냈습니다...`); (기존 코드 지우고)
      
      alert(response.data); // "비밀번호 재설정 토큰이 발급되었습니다. (dev token: ...)" 출력됨
      
      console.log("백엔드 응답:", response.data); // 콘솔에도 찍어줌

    } catch (error) {
      console.error("요청 실패:", error);
      alert("오류가 발생했습니다.");
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.box}>
        <h2 style={styles.title}>비밀번호 재설정</h2>
        <p style={styles.description}>
          가입하신 이메일 주소를 입력해 주시면,<br/>
          비밀번호를 재설정할 수 있는 링크를 보내드립니다.
        </p>
        
        <input 
          type="email" 
          placeholder="가입한 이메일 입력" 
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          style={styles.input}
        />
        
        <button onClick={handleSubmit} style={styles.button}>인증 메일 전송</button>
      </div>
    </div>
  );
}

export default FindPasswordPage;