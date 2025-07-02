import React, { useState } from 'react';
import axios from 'axios';
import './Login.css';
import { useNavigate } from 'react-router-dom';

function Signup({ onSignupSuccess }) {
  const [formData, setFormData] = useState({
    loginId: '',
    password: '',
    passwordConfirm: '',
    email: '',
    nickname: '',
    verificationCode: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [codeSent, setCodeSent] = useState(false);
  const [validations, setValidations] = useState({
    loginId: { available: null, message: '' },
    email: { available: null, message: '' },
    nickname: { available: null, message: '' }
  });
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const checkDuplicate = async (type, value) => {
    if (!value) return;
    
    try {
      let endpoint = '';
      switch (type) {
        case 'loginId':
          endpoint = `http://localhost:80/api/users/check-login-id?loginId=${value}`;
          break;
        case 'email':
          endpoint = `http://localhost:80/api/users/check-email?email=${value}`;
          break;
        case 'nickname':
          endpoint = `http://localhost:80/api/users/check-nickname?nickname=${value}`;
          break;
        default:
          return;
      }

      const response = await axios.get(endpoint);
      setValidations(prev => ({
        ...prev,
        [type]: {
          available: !response.data.duplicate,
          message: response.data.message
        }
      }));
    } catch (err) {
      console.error(`${type} 중복 확인 오류:`, err);
    }
  };

  const handleBlur = (e) => {
    const { name, value } = e.target;
    if (value) {
      checkDuplicate(name, value);
    }
  };

  const sendVerificationCode = async () => {
    if (!formData.email) {
      setError('이메일을 입력해주세요.');
      return;
    }

    try {
      await axios.post('http://localhost:80/api/mail/send-verification', {
        email: formData.email
      });
      setCodeSent(true);
      setError('');
    } catch (err) {
      setError(err.response?.data?.message || '인증 코드 발송에 실패했습니다.');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    // 비밀번호 확인
    if (formData.password !== formData.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      setLoading(false);
      return;
    }

    // 중복 확인 검사
    const hasInvalidValidation = Object.values(validations).some(
      validation => validation.available === false
    );
    if (hasInvalidValidation) {
      setError('중복 확인이 필요한 항목이 있습니다.');
      setLoading(false);
      return;
    }

    try {
      const response = await axios.post('http://localhost:80/api/users/join', formData);
      if (response.data.success) {
        onSignupSuccess(response.data);
        navigate('/login');
      } else {
        setError(response.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || '회원가입 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const getRecommendNicknames = async () => {
    try {
      const response = await axios.get('http://localhost:80/api/users/recommend-nickname');
      if (response.data.nicknames && response.data.nicknames.length > 0) {
        setFormData(prev => ({
          ...prev,
          nickname: response.data.nicknames[0]
        }));
      }
    } catch (err) {
      console.error('닉네임 추천 오류:', err);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>회원가입</h2>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="loginId">아이디</label>
            <input
              type="text"
              id="loginId"
              name="loginId"
              value={formData.loginId}
              onChange={handleChange}
              onBlur={handleBlur}
              required
            />
            {validations.loginId.message && (
              <span className={`validation-message ${validations.loginId.available ? 'success' : 'error'}`}>
                {validations.loginId.message}
              </span>
            )}
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

          <div className="form-group">
            <label htmlFor="passwordConfirm">비밀번호 확인</label>
            <input
              type="password"
              id="passwordConfirm"
              name="passwordConfirm"
              value={formData.passwordConfirm}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">이메일</label>
            <div className="email-group">
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                onBlur={handleBlur}
                required
              />
              <button type="button" onClick={sendVerificationCode} className="send-code-button">
                인증코드 발송
              </button>
            </div>
            {validations.email.message && (
              <span className={`validation-message ${validations.email.available ? 'success' : 'error'}`}>
                {validations.email.message}
              </span>
            )}
          </div>

          {codeSent && (
            <div className="form-group">
              <label htmlFor="verificationCode">인증코드</label>
              <input
                type="text"
                id="verificationCode"
                name="verificationCode"
                value={formData.verificationCode}
                onChange={handleChange}
                required
              />
            </div>
          )}

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

          <button type="submit" disabled={loading} className="login-button">
            {loading ? '가입 중...' : '회원가입'}
          </button>
        </form>
        <div className="login-footer">
          <button onClick={() => navigate('/login')} className="switch-button">
            로그인으로 돌아가기
          </button>
        </div>
      </div>
    </div>
  );
}

export default Signup; 