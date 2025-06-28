import React, { useState } from 'react';
import axios from 'axios';

function FindId() {
  const [email, setEmail] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const res = await axios.post('http://localhost:80/api/find-id', { email });
      if (res.data && res.data.success && res.data.maskedLoginId) {
        setResult(res.data.maskedLoginId);
      } else {
        setError(res.data.message || '아이디를 찾을 수 없습니다.');
      }
    } catch (err) {
      setError('아이디 찾기 요청에 실패했습니다.');
    }
    setLoading(false);
  };

  const goToLogin = () => {
    window.location.href = '/login';
  };

  return (
    <div style={{ padding: 40, maxWidth: 400, margin: '40px auto', textAlign: 'center', background: '#fff', borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.07)' }}>
      <h2>아이디 찾기</h2>
      <form onSubmit={handleSubmit} style={{ marginTop: 24 }}>
        <div style={{ marginBottom: 16 }}>
          <input
            type="email"
            placeholder="가입한 이메일을 입력하세요"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
            style={{ width: '100%', padding: 10, borderRadius: 6, border: '1.5px solid #dadce0', fontSize: 16 }}
          />
        </div>
        <button type="submit" disabled={loading} style={{ width: '100%', padding: 10, borderRadius: 6, background: '#a18cd1', color: '#fff', fontWeight: 600, fontSize: 16, border: 'none', cursor: 'pointer' }}>
          {loading ? '조회 중...' : '아이디 찾기'}
        </button>
      </form>
      {result && (
        <div style={{ marginTop: 24, color: '#333', fontWeight: 500 }}>
          <span>회원님의 아이디: </span>
          <span style={{ color: '#7c5fa3', fontWeight: 700 }}>{result}</span>
        </div>
      )}
      {error && (
        <div style={{ marginTop: 24, color: '#e57373', fontWeight: 500 }}>{error}</div>
      )}
      {(result || error) && (
        <div style={{ marginTop: 24 }}>
          <button 
            onClick={goToLogin}
            style={{ 
              padding: '10px 20px', 
              borderRadius: 6, 
              background: '#f5f5f5', 
              color: '#333', 
              fontWeight: 500, 
              fontSize: 14, 
              border: '1px solid #ddd', 
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
            onMouseOver={(e) => e.target.style.background = '#e8e8e8'}
            onMouseOut={(e) => e.target.style.background = '#f5f5f5'}
          >
            로그인하러가기
          </button>
        </div>
      )}
    </div>
  );
}

export default FindId; 