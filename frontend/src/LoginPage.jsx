import React, { useState, useEffect } from 'react';
import { safeFetch } from './api';
import { useNavigate, useLocation } from 'react-router-dom';

function LoginPage() {
  const [form, setForm] = useState({ username: '', password: '' });
  const [result, setResult] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const message = params.get('message');
    if (message) {
      setResult(decodeURIComponent(message));
    }
  }, [location]);

  const handleChange = e => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async e => {
    e.preventDefault();
    console.log('form.username:', form.username);
    console.log('form.password:', form.password);
    const res = await fetch('http://localhost:8080/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        username: form.username,
        password: form.password
      })
    });
    let data;
    try {
      data = await res.json();
    } catch {
      data = {};
    }
    if (data.success && data.nickname) {
      setResult(`로그인 성공! 닉네임: ${decodeURIComponent(data.nickname)}`);
    } else if (data.success) {
      setResult('로그인 성공!');
    } else {
      setResult(data.message || '로그인 실패');
    }
  };

  return (
    <div>
      <h2>로그인 테스트</h2>
      <form onSubmit={handleSubmit}>
        <input name="username" placeholder="아이디" value={form.username} onChange={handleChange} /><br />
        <input name="password" type="password" placeholder="비밀번호" value={form.password} onChange={handleChange} /><br />
        <button type="submit">로그인</button>
      </form>
      <div>{result}</div>
    </div>
  );
}

export default LoginPage; 