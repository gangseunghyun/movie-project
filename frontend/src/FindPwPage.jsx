import React, { useState } from 'react';
import { safeFetch } from './api';

function FindPwPage() {
  const [form, setForm] = useState({ username: '', email: '' });
  const [result, setResult] = useState(null);

  const handleChange = e => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async e => {
    e.preventDefault();
    const data = await safeFetch('/api/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form),
    });
    setResult(data);
  };

  return (
    <div>
      <h2>비밀번호 찾기 테스트</h2>
      <form onSubmit={handleSubmit}>
        <input name="username" placeholder="아이디" value={form.username} onChange={handleChange} /><br />
        <input name="email" placeholder="이메일" value={form.email} onChange={handleChange} /><br />
        <button type="submit">비밀번호 찾기</button>
      </form>
      <pre>{result && JSON.stringify(result, null, 2)}</pre>
    </div>
  );
}

export default FindPwPage; 