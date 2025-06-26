import React, { useState } from 'react';
import { safeFetch } from './api';

function SignUpPage() {
  const [form, setForm] = useState({
    username: '',
    password: '',
    passwordConfirm: '',
    nickname: '',
    email: '',
    emailCode: ''
  });
  const [result, setResult] = useState(null);
  const [idCheckMsg, setIdCheckMsg] = useState('');
  const [nicknameCheckMsg, setNicknameCheckMsg] = useState('');
  const [nicknameRecommend, setNicknameRecommend] = useState('');
  const [emailCodeSent, setEmailCodeSent] = useState(false);
  const [emailCodeMsg, setEmailCodeMsg] = useState('');
  const [pwMatch, setPwMatch] = useState(true);
  const [emailCheckMsg, setEmailCheckMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const handleChange = e => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (e.target.name === 'password' || e.target.name === 'passwordConfirm') {
      setPwMatch(
        e.target.name === 'password'
          ? e.target.value === form.passwordConfirm
          : form.password === e.target.value
      );
    }
  };

  const handleIdCheck = async () => {
    const data = await safeFetch(`/api/users/check-login-id?loginId=${encodeURIComponent(form.username)}`);
    setIdCheckMsg(data.message);
  };

  const handleNicknameCheck = async () => {
    const data = await safeFetch(`/api/users/check-nickname?nickname=${encodeURIComponent(form.nickname)}`);
    setNicknameCheckMsg(data.message);
  };

  const handleNicknameRecommend = async () => {
    const data = await safeFetch('/api/users/recommend-nickname');
    if (data && Array.isArray(data.nicknames) && data.nicknames.length > 0) {
      setForm({ ...form, nickname: data.nicknames[0] });
      setNicknameRecommend(data.nicknames.join(', '));
      setNicknameCheckMsg('추천 닉네임을 입력란에 넣었습니다. 중복확인을 해주세요!');
    } else {
      setNicknameRecommend('추천 닉네임 없음');
    }
  };

  const handleEmailCheck = async () => {
    const data = await safeFetch(`/api/users/check-email?email=${encodeURIComponent(form.email)}`);
    setEmailCheckMsg(data.message);
  };

  const handleEmailSend = async () => {
    const data = await safeFetch('/api/mail/send-verification', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: form.email }),
    });
    if (data.success) {
      setEmailCodeSent(true);
      setEmailCodeMsg(data.message || '인증코드가 전송되었습니다.');
    } else {
      setEmailCodeMsg(data.message || '이메일 전송 실패');
    }
  };

  const handleEmailVerify = async () => {
    const data = await safeFetch('/api/mail/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: form.email, verificationCode: form.emailCode }),
    });
    setEmailCodeMsg(data.success ? '이메일 인증 성공!' : (data.message || '이메일 인증 실패'));
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setErrorMsg('');
    if (!pwMatch) {
      setErrorMsg('비밀번호가 일치하지 않습니다.');
      return;
    }
    const data = await safeFetch('/api/users/join', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        loginId: form.username,
        password: form.password,
        passwordConfirm: form.passwordConfirm,
        nickname: form.nickname,
        email: form.email,
        verificationCode: form.emailCode
      }),
    });
    if (data.success) {
      setResult(data);
      setErrorMsg('');
    } else {
      setErrorMsg(data.message || '회원가입에 실패했습니다.');
    }
  };

  return (
    <div>
      <h2>회원가입 테스트</h2>
      <form onSubmit={handleSubmit}>
        <input name="username" placeholder="아이디" value={form.username} onChange={handleChange} />
        <button type="button" onClick={handleIdCheck}>아이디 중복확인</button>
        <span style={{ marginLeft: 8 }}>{idCheckMsg}</span><br />
        <input name="password" type="password" placeholder="비밀번호" value={form.password} onChange={handleChange} /><br />
        <input name="passwordConfirm" type="password" placeholder="비밀번호 확인" value={form.passwordConfirm} onChange={handleChange} />
        {!pwMatch && <span style={{ color: 'red' }}>비밀번호가 일치하지 않습니다.</span>}<br />
        <input name="nickname" placeholder="닉네임" value={form.nickname} onChange={handleChange} />
        <button type="button" onClick={handleNicknameCheck}>닉네임 중복확인</button>
        <button type="button" onClick={handleNicknameRecommend}>닉네임 추천</button>
        <span style={{ marginLeft: 8 }}>{nicknameCheckMsg}</span><br />
        {nicknameRecommend && <div>추천 닉네임: {nicknameRecommend}</div>}
        <input name="email" placeholder="이메일" value={form.email} onChange={handleChange} />
        <button type="button" onClick={handleEmailCheck}>이메일 중복확인</button>
        <span style={{ marginLeft: 8 }}>{emailCheckMsg}</span>
        <button type="button" onClick={handleEmailSend}>이메일 인증코드 전송</button><br />
        {emailCodeSent && (
          <>
            <input name="emailCode" placeholder="인증코드 입력" value={form.emailCode} onChange={handleChange} />
            <button type="button" onClick={handleEmailVerify}>인증코드 확인</button><br />
          </>
        )}
        {emailCodeMsg && <div>{emailCodeMsg}</div>}
        <button type="submit">회원가입</button>
      </form>
      {errorMsg && <div style={{ color: 'red', marginTop: 8 }}>{errorMsg}</div>}
      <pre>{result && JSON.stringify(result, null, 2)}</pre>
    </div>
  );
}

export default SignUpPage; 