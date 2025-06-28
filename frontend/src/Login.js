import React, { useState } from 'react';
import axios from 'axios';
import './Login.css';
import { useNavigate } from 'react-router-dom';

function Login({ onLoginSuccess }) {
  const [formData, setFormData] = useState({
    loginId: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

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
        navigate('/');
      } else {
        setError(response.data.message);
      }
    } catch (err) {
      console.error('로그인 실패:', err);
      if (err.response && err.response.data) {
        const errorMessage = err.response.data.message;
        
        // Provider별 에러 메시지 처리
        if (errorMessage.includes('PROVIDER:')) {
          const parts = errorMessage.split('|');
          if (parts.length >= 2) {
            const provider = parts[0].replace('PROVIDER:', '');
            const message = parts[1];
            
            if (provider === 'local') {
              setError('이 이메일은 일반 계정으로 가입되어 있습니다. 아이디/비밀번호로 로그인해 주세요.');
            } else {
              setError(`이 이메일은 ${getProviderDisplayName(provider)} 계정입니다. ${getProviderDisplayName(provider)} 로그인을 이용해 주세요.`);
            }
          } else {
            setError(errorMessage);
          }
        } else {
          setError(errorMessage || '로그인에 실패했습니다.');
        }
      } else {
        setError('로그인에 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleOAuth2Login = (provider) => {
    window.location.href = `http://localhost:80/oauth2/authorization/${provider}`;
  };

  // Provider 표시명 변환 함수
  const getProviderDisplayName = (provider) => {
    switch (provider.toUpperCase()) {
      case 'GOOGLE':
        return 'Google';
      case 'KAKAO':
        return 'Kakao';
      case 'NAVER':
        return 'Naver';
      case 'FACEBOOK':
        return 'Facebook';
      default:
        return provider;
    }
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
          <button onClick={() => navigate('/signup')} className="switch-button">
            회원가입
          </button>
        </div>
      </div>
    </div>
  );
}

export default Login;