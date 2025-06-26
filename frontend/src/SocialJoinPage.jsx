import React, { useState } from 'react';
import { safeFetch } from './api';

function SocialJoinPage() {
  const [nickname, setNickname] = useState('');
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [nicknameMessage, setNicknameMessage] = useState('');
  const [agree, setAgree] = useState(false);
  const [result, setResult] = useState(null);
  const [recommending, setRecommending] = useState(false);

  const handleNicknameChange = e => {
    setNickname(e.target.value);
    setNicknameChecked(false);
    setNicknameMessage('');
  };

  const handleCheckNickname = async () => {
    if (!nickname) {
      setNicknameMessage('닉네임을 입력하세요.');
      return;
    }
    const data = await safeFetch(`/api/users/check-nickname?nickname=${encodeURIComponent(nickname)}`);
    if (data === true) {
      setNicknameChecked(false);
      setNicknameMessage('이미 사용 중인 닉네임입니다.');
    } else {
      setNicknameChecked(true);
      setNicknameMessage('사용 가능한 닉네임입니다!');
    }
  };

  const handleRecommendNickname = async () => {
    setRecommending(true);
    const data = await safeFetch('/api/users/recommend-nickname');
    if (data && Array.isArray(data.nicknames) && data.nicknames.length > 0) {
      setNickname(data.nicknames[0]);
      setNicknameChecked(false);
      setNicknameMessage('추천 닉네임을 입력란에 넣었습니다. 중복확인을 해주세요!');
    }
    setRecommending(false);
  };

  const handleSubmit = async e => {
    e.preventDefault();
    if (!nicknameChecked) {
      setResult('닉네임 중복확인을 해주세요.');
      return;
    }
    if (!agree) {
      setResult('약관에 동의해야 가입이 완료됩니다.');
      return;
    }
    const data = await safeFetch('/api/social-join-complete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nickname, agree }),
    });
    setResult(data.success ? '가입 완료! 이제 로그인하세요.' : (data.message || '가입 실패'));
  };

  return (
    <div>
      <h2>소셜 회원가입 추가 정보</h2>
      <form onSubmit={handleSubmit}>
        <input name="nickname" placeholder="닉네임" value={nickname} onChange={handleNicknameChange} />
        <button type="button" onClick={handleCheckNickname} style={{ marginLeft: 8 }}>중복확인</button>
        <button type="button" onClick={handleRecommendNickname} style={{ marginLeft: 8 }} disabled={recommending}>닉네임 추천</button>
        <div style={{ color: nicknameChecked ? 'green' : 'red', marginBottom: 8 }}>{nicknameMessage}</div>
        <label>
          <input type="checkbox" name="agree" checked={agree} onChange={e => setAgree(e.target.checked)} />
          약관에 동의합니다.
        </label><br />
        <button type="submit">가입 완료</button>
      </form>
      <div>{result}</div>
    </div>
  );
}

export default SocialJoinPage; 