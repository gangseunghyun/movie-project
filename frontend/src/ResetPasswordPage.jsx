import React, { useState } from 'react';
import { safeFetch } from './api';

function ResetPasswordPage() {
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [result, setResult] = useState(null);

  // URL에서 토큰 추출
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token');

  const handleSubmit = async e => {
    e.preventDefault();
    const data = await safeFetch(`/api/reset-password?token=${encodeURIComponent(token)}&newPassword=${encodeURIComponent(newPassword)}&newPasswordConfirm=${encodeURIComponent(newPasswordConfirm)}`, {
      method: 'POST'
    });
    setResult(data);
  };

  return (
    <div>
      <h2>비밀번호 재설정</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="password"
          placeholder="새 비밀번호"
          value={newPassword}
          onChange={e => setNewPassword(e.target.value)}
        /><br />
        <input
          type="password"
          placeholder="새 비밀번호 확인"
          value={newPasswordConfirm}
          onChange={e => setNewPasswordConfirm(e.target.value)}
        /><br />
        <button type="submit">비밀번호 재설정</button>
      </form>
      <pre>{result && JSON.stringify(result, null, 2)}</pre>
    </div>
  );
}

export default ResetPasswordPage; 