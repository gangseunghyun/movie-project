import React, { useState, useEffect } from 'react';
import axios from 'axios';

function validatePassword(pw) {
  if (!pw || pw.length < 8) return '비밀번호는 8자 이상이어야 합니다.';
  if (!/[a-zA-Z]/.test(pw)) return '영문자를 포함해야 합니다.';
  if (!/\d/.test(pw)) return '숫자를 포함해야 합니다.';
  if (!/[!@#$%^&*(),.?":{}|<>]/.test(pw)) return '특수문자를 포함해야 합니다.';
  return '';
}

function ResetPassword() {
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [pwError, setPwError] = useState('');
  const [pwConfirmError, setPwConfirmError] = useState('');
  const [result, setResult] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setToken(params.get('token') || '');
  }, []);

  const handlePwChange = (e) => {
    setPassword(e.target.value);
    setPwError(validatePassword(e.target.value));
  };

  const handlePwConfirmChange = (e) => {
    setPasswordConfirm(e.target.value);
    setPwConfirmError(e.target.value !== password ? '비밀번호가 일치하지 않습니다.' : '');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setResult('');
    if (!token) {
      setError('유효하지 않은 접근입니다.');
      return;
    }
    const pwErr = validatePassword(password);
    const pwcErr = password !== passwordConfirm ? '비밀번호가 일치하지 않습니다.' : '';
    setPwError(pwErr);
    setPwConfirmError(pwcErr);
    if (pwErr || pwcErr) return;
    setLoading(true);
    try {
      const res = await axios.post('http://localhost:80/api/reset-password', null, {
        params: {
          token,
          newPassword: password,
          newPasswordConfirm: passwordConfirm
        }
      });
      if (res.data && res.data.success) {
        setResult('비밀번호가 성공적으로 변경되었습니다. 이제 로그인해 주세요.');
      } else {
        setError(res.data.message || '비밀번호 재설정에 실패했습니다.');
      }
    } catch (err) {
      setError('비밀번호 재설정 요청에 실패했습니다.');
    }
    setLoading(false);
  };

  return (
    <div style={{ padding: 40, maxWidth: 400, margin: '40px auto', textAlign: 'center', background: '#fff', borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.07)' }}>
      <h2>비밀번호 재설정</h2>
      <form onSubmit={handleSubmit} style={{ marginTop: 24 }}>
        <div style={{ marginBottom: 16 }}>
          <input
            type="password"
            placeholder="새 비밀번호"
            value={password}
            onChange={handlePwChange}
            required
            style={{ width: '100%', padding: 10, borderRadius: 6, border: '1.5px solid #dadce0', fontSize: 16 }}
          />
          {pwError && <div style={{ color: '#e57373', fontSize: 13, marginTop: 4 }}>{pwError}</div>}
        </div>
        <div style={{ marginBottom: 16 }}>
          <input
            type="password"
            placeholder="새 비밀번호 확인"
            value={passwordConfirm}
            onChange={handlePwConfirmChange}
            required
            style={{ width: '100%', padding: 10, borderRadius: 6, border: '1.5px solid #dadce0', fontSize: 16 }}
          />
          {pwConfirmError && <div style={{ color: '#e57373', fontSize: 13, marginTop: 4 }}>{pwConfirmError}</div>}
        </div>
        <button type="submit" disabled={loading} style={{ width: '100%', padding: 10, borderRadius: 6, background: '#a18cd1', color: '#fff', fontWeight: 600, fontSize: 16, border: 'none', cursor: 'pointer' }}>
          {loading ? '변경 중...' : '비밀번호 변경'}
        </button>
      </form>
      {result && (
        <div style={{ marginTop: 24, color: '#333', fontWeight: 500 }}>{result}</div>
      )}
      {error && (
        <div style={{ marginTop: 24, color: '#e57373', fontWeight: 500 }}>{error}</div>
      )}
    </div>
  );
}

export default ResetPassword; 