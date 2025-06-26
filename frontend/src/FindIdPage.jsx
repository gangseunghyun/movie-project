import React, { useState } from 'react';
import { safeFetch } from './api';

function FindIdPage() {
  const [email, setEmail] = useState('');
  const [result, setResult] = useState(null);

  const handleSubmit = async e => {
    e.preventDefault();
    const data = await safeFetch('/api/find-id', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
    });
    setResult(data);
  };

  return (
    <div>
      <h2>아이디 찾기 테스트</h2>
      <form onSubmit={handleSubmit}>
        <input placeholder="이메일" value={email} onChange={e => setEmail(e.target.value)} /><br />
        <button type="submit">아이디 찾기</button>
      </form>
      <pre>{result && JSON.stringify(result, null, 2)}</pre>
    </div>
  );
}

export default FindIdPage; 