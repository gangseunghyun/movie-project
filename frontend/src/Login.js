import React, { useState } from 'react';
import axios from 'axios';
import './Login.css';

function Login({ onLoginSuccess, onSwitchToSignup }) {
  const [formData, setFormData] = useState({
    loginId: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await axios.post('http://localhost:80/api/user-login', formData, {
        withCredentials: true
      });

      if (response.data.success) {
        onLoginSuccess(response.data.user);
      } else {
        setError(response.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || '로그인 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleOAuth2Login = (provider) => {
    window.location.href = `http://localhost:80/oauth2/authorization/${provider}`;
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>로그인</h2>
        {error && <div className="error-message">{error}</div>}
        
        {/* OAuth2 로그인 버튼들 */}
        <div className="oauth-buttons">
          <button 
            type="button" 
            onClick={() => handleOAuth2Login('google')}
            className="oauth-button google"
          >
            Google로 로그인
          </button>
          <button 
            type="button" 
            onClick={() => handleOAuth2Login('kakao')}
            className="oauth-button kakao"
          >
            Kakao로 로그인
          </button>
          <button 
            type="button" 
            onClick={() => handleOAuth2Login('naver')}
            className="oauth-button naver"
          >
            Naver로 로그인
          </button>
        </div>
        
        <div className="divider">
          <span>또는</span>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="loginId">아이디</label>
            <input
              type="text"
              id="loginId"
              name="loginId"
              value={formData.loginId}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">비밀번호</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
            />
          </div>
          <button type="submit" disabled={loading} className="login-button">
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <div className="login-footer">
          <button onClick={onSwitchToSignup} className="switch-button">
            회원가입
          </button>
        </div>
      </div>
    </div>
  );
}

export default Login; 