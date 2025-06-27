import React, { useState } from 'react';
import './LoginChoice.css';

function LoginChoice() {
  const [showLocalLogin, setShowLocalLogin] = useState(false);
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');

  // 소셜 로그인
  const handleSocialLogin = (provider) => {
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  // 자체 로그인
  const handleLocalLogin = async (e) => {
    e.preventDefault();
    const res = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username: loginId, password })
    });
    const data = await res.json();
    alert(data.success ? '로그인 성공' : data.message);
  };

  return (
    <div className="login-bg">
      <div className="login-main">
        <h2>로그인 방법을 선택하세요</h2>
        <div className="login-btns">
          <button className="login-btn google" onClick={() => handleSocialLogin('google')}>Google 로그인</button>
          <button className="login-btn naver" onClick={() => handleSocialLogin('naver')}>Naver 로그인</button>
          <button className="login-btn kakao" onClick={() => handleSocialLogin('kakao')}>Kakao 로그인</button>
        </div>
        <div style={{ margin: '20px 0' }}>
          <button className="login-btn local" onClick={() => setShowLocalLogin(!showLocalLogin)}>
            자체 로그인
          </button>
        </div>
        {showLocalLogin && (
          <form onSubmit={handleLocalLogin} className="local-login-form">
            <input
              type="text"
              placeholder="아이디"
              value={loginId}
              onChange={e => setLoginId(e.target.value)}
              required
            />
            <input
              type="password"
              placeholder="비밀번호"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
            <button type="submit">로그인</button>
          </form>
        )}
      </div>
    </div>
  );
}

export default LoginChoice; 