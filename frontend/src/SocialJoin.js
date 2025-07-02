import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './Login.css';

function SocialJoin() {
  const [formData, setFormData] = useState({
    nickname: '',
    agree: false
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [validations, setValidations] = useState({
    nickname: { available: null, message: '' }
  });

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const checkNicknameDuplicate = async (nickname) => {
    if (!nickname) return;
    
    try {
      const response = await axios.get(`http://localhost:80/api/users/check-nickname?nickname=${nickname}`);
      setValidations(prev => ({
        ...prev,
        nickname: {
          available: !response.data.duplicate,
          message: response.data.message
        }
      }));
    } catch (err) {
      console.error('닉네임 중복 확인 실패:', err);
    }
  };

  const handleBlur = (e) => {
    const { name, value } = e.target;
    if (name === 'nickname') {
      checkNicknameDuplicate(value);
    }
  };

  const getRecommendNicknames = async () => {
    try {
      const response = await axios.get('http://localhost:80/api/users/recommend-nickname');
      if (response.data.nicknames && response.data.nicknames.length > 0) {
        const recommendedNickname = response.data.nicknames[0];
        setFormData(prev => ({ ...prev, nickname: recommendedNickname }));
        checkNicknameDuplicate(recommendedNickname);
      }
    } catch (err) {
      console.error('닉네임 추천 실패:', err);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (!formData.nickname || formData.nickname.trim() === '') {
      setError('닉네임을 입력해 주세요.');
      setLoading(false);
      return;
    }

    if (!formData.agree) {
      setError('약관에 동의해야 가입이 완료됩니다.');
      setLoading(false);
      return;
    }

    try {
      const response = await axios.post('http://localhost:80/api/social-join-complete', {
        nickname: formData.nickname,
        agree: formData.agree
      });

      if (response.data.success) {
        alert('소셜 회원가입이 완료되었습니다!');
        window.location.href = '/';
      } else {
        setError(response.data.message);
      }
    } catch (err) {
      console.error('소셜 회원가입 실패:', err);
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
          setError(errorMessage || '회원가입 중 오류가 발생했습니다.');
        }
      } else {
        setError('회원가입 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
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
        <h2>소셜 회원가입 완료</h2>
        <p className="subtitle">추가 정보를 입력해 주세요</p>
        
        {error && <div className="error-message">{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="nickname">닉네임</label>
            <div className="nickname-group">
              <input
                type="text"
                id="nickname"
                name="nickname"
                value={formData.nickname}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="닉네임을 입력하세요"
                required
              />
              <button type="button" onClick={getRecommendNicknames} className="recommend-button">
                추천
              </button>
            </div>
            {validations.nickname.message && (
              <span className={`validation-message ${validations.nickname.available ? 'success' : 'error'}`}>
                {validations.nickname.message}
              </span>
            )}
          </div>

          <div className="form-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                name="agree"
                checked={formData.agree}
                onChange={handleChange}
                required
              />
              <span className="checkmark"></span>
              이용약관 및 개인정보처리방침에 동의합니다.
            </label>
          </div>

          <button type="submit" disabled={loading} className="login-button">
            {loading ? '가입 중...' : '회원가입 완료'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default SocialJoin; 