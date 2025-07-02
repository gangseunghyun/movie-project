import React, { useState, useEffect } from 'react';
import './TestPage.css';

/**
 * 구현된 모든 기능을 테스트할 수 있는 종합 테스트 페이지
 */
function TestPage() {
  // 공통 상태
  const [testResults, setTestResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);

  // 회원가입
  const [signup, setSignup] = useState({ loginId: '', email: '', nickname: '', password: '', passwordConfirm: '' });
  // 로그인
  const [login, setLogin] = useState({ loginId: '', password: '' });
  // 아이디/비밀번호 찾기
  const [find, setFind] = useState({ email: '' });
  // 닉네임
  const [nickname, setNickname] = useState({ nickname: '' });
  // 비밀번호 재설정
  const [reset, setReset] = useState({ token: '', newPassword: '', newPasswordConfirm: '' });
  // 소셜 회원가입 추가정보
  const [socialJoin, setSocialJoin] = useState({ nickname: '', agree: false });
  // 영화 검색
  const [search, setSearch] = useState({ query: '' });

  // 결과 추가
  const addResult = (title, status, data) => {
    setTestResults(prev => [
      { title, status, data, time: new Date().toLocaleTimeString() },
      ...prev
    ]);
  };

  // 현재 사용자 확인
  const checkCurrentUser = async () => {
    try {
      const res = await fetch('http://localhost:80/api/current-user', { credentials: 'include' });
      if (res.ok) {
        const user = await res.json();
        setCurrentUser(user);
        addResult('현재 사용자', '성공', user);
      } else {
        setCurrentUser(null);
        addResult('현재 사용자', '실패', '로그인되지 않음');
      }
    } catch (e) { addResult('현재 사용자', '에러', e.message); }
  };

  useEffect(() => { checkCurrentUser(); }, []);

  // 회원가입
  const doSignup = async () => {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:80/api/users/join', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(signup)
      });
      const data = await res.json();
      addResult('회원가입', data.success ? '성공' : '실패', data);
    } catch (e) { addResult('회원가입', '에러', e.message); }
    setLoading(false);
  };

  // 로그인
  const doLogin = async () => {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:80/api/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include',
        body: JSON.stringify({ username: login.loginId, password: login.password })
      });
      const data = await res.json();
      addResult('로그인', data.success ? '성공' : '실패', data);
      if (data.success) await checkCurrentUser();
    } catch (e) { addResult('로그인', '에러', e.message); }
    setLoading(false);
  };

  // 로그아웃
  const doLogout = async () => {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:80/api/logout', { method: 'POST', credentials: 'include' });
      const data = await res.json();
      addResult('로그아웃', '성공', data);
      setCurrentUser(null);
    } catch (e) { addResult('로그아웃', '에러', e.message); }
    setLoading(false);
  };

  // 소셜 로그인
  const doSocialLogin = (provider) => {
    window.location.href = `/oauth2/authorization/${provider}`;
    addResult(`${provider} 소셜 로그인`, '진행중', '소셜 로그인 페이지로 이동');
  };

  // 아이디 찾기
  const doFindId = async () => {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:80/api/find-id', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: find.email })
      });
      const data = await res.json();
      addResult('아이디 찾기', data.success ? '성공' : '실패', data);
    } catch (e) { addResult('아이디 찾기', '에러', e.message); }
    setLoading(false);
  };

  // 비밀번호 찾기
  const doForgotPassword = async () => {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:80/api/forgot-password', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: find.email })
      });
      const data = await res.json();
      addResult('비밀번호 찾기', '성공', data);
    } catch (e) { addResult('비밀번호 찾기', '에러', e.message); }
    setLoading(false);
  };

  // 닉네임 중복 확인
  const doCheckNickname = async () => {
    setLoading(true);
    try {
      const res = await fetch(`http://localhost:80/api/users/check-nickname?nickname=${nickname.nickname}`);
      const data = await res.json();
      addResult('닉네임 중복 확인', '성공', data);
    } catch (e) { addResult('닉네임 중복 확인', '에러', e.message); }
    setLoading(false);
  };

  // 닉네임 추천
  const doRecommendNickname = async () => {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:80/api/users/recommend-nickname');
      const data = await res.json();
      addResult('닉네임 추천', '성공', data);
    } catch (e) { addResult('닉네임 추천', '에러', e.message); }
    setLoading(false);
  };

  // 영화 검색
  const doSearch = async () => {
    setLoading(true);
    try {
      const res = await fetch(`http://localhost:80/api/search?query=${encodeURIComponent(search.query)}`);
      const data = await res.json();
      addResult('영화 검색', '성공', data);
    } catch (e) { addResult('영화 검색', '에러', e.message); }
    setLoading(false);
  };

  // 입력 핸들러
  const handle = (setter) => (e) => {
    const { name, value, type, checked } = e.target;
    setter(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  return (
    <div className="test-page">
      <div className="test-header">
        <h1>🎬 서비스 기능별 테스트 페이지</h1>
        <p>각 서비스 기능을 실제로 테스트할 수 있습니다.</p>
      </div>

      {/* 현재 사용자 */}
      <div className="test-section">
        <h2>👤 현재 사용자</h2>
        {currentUser ? (
          <div className="user-details">
            <p><b>아이디:</b> {currentUser.loginId}</p>
            <p><b>닉네임:</b> {currentUser.nickname}</p>
            <p><b>이메일:</b> {currentUser.email}</p>
          </div>
        ) : <p className="not-logged-in">로그인되지 않음</p>}
        <button onClick={checkCurrentUser}>🔄 새로고침</button>
      </div>

      {/* 회원가입 */}
      <div className="test-section">
        <h2>📝 회원가입</h2>
        <input name="loginId" placeholder="아이디" value={signup.loginId} onChange={handle(setSignup)} />
        <input name="email" placeholder="이메일" value={signup.email} onChange={handle(setSignup)} />
        <input name="nickname" placeholder="닉네임" value={signup.nickname} onChange={handle(setSignup)} />
        <input name="password" type="password" placeholder="비밀번호" value={signup.password} onChange={handle(setSignup)} />
        <input name="passwordConfirm" type="password" placeholder="비밀번호 확인" value={signup.passwordConfirm} onChange={handle(setSignup)} />
        <button onClick={doSignup} disabled={loading}>회원가입</button>
      </div>

      {/* 로그인/로그아웃 */}
      <div className="test-section">
        <h2>🔐 로그인/로그아웃</h2>
        <input name="loginId" placeholder="아이디" value={login.loginId} onChange={handle(setLogin)} />
        <input name="password" type="password" placeholder="비밀번호" value={login.password} onChange={handle(setLogin)} />
        <button onClick={doLogin} disabled={loading}>로그인</button>
        <button onClick={doLogout} disabled={loading}>로그아웃</button>
      </div>

      {/* 소셜 로그인 */}
      <div className="test-section">
        <h2>🔗 소셜 로그인</h2>
        <button className="social-btn google" onClick={() => doSocialLogin('google')}>Google</button>
        <button className="social-btn naver" onClick={() => doSocialLogin('naver')}>Naver</button>
        <button className="social-btn kakao" onClick={() => doSocialLogin('kakao')}>Kakao</button>
      </div>

      {/* 아이디/비밀번호 찾기 */}
      <div className="test-section">
        <h2>🔎 아이디/비밀번호 찾기</h2>
        <input name="email" placeholder="이메일" value={find.email} onChange={handle(setFind)} />
        <button onClick={doFindId} disabled={loading}>아이디 찾기</button>
        <button onClick={doForgotPassword} disabled={loading}>비밀번호 찾기</button>
      </div>

      {/* 닉네임 중복/추천 */}
      <div className="test-section">
        <h2>💡 닉네임 중복/추천</h2>
        <input name="nickname" placeholder="닉네임" value={nickname.nickname} onChange={handle(setNickname)} />
        <button onClick={doCheckNickname} disabled={loading}>중복 확인</button>
        <button onClick={doRecommendNickname} disabled={loading}>닉네임 추천</button>
      </div>

      {/* 영화 검색 */}
      <div className="test-section">
        <h2>🎬 영화 검색</h2>
        <input name="query" placeholder="영화 제목" value={search.query} onChange={handle(setSearch)} />
        <button onClick={doSearch} disabled={loading}>검색</button>
      </div>

      {/* 결과 */}
      <div className="test-section">
        <h2>📊 테스트 결과</h2>
        <div className="test-results">
          {testResults.length === 0 && <p className="no-results">아직 테스트 결과가 없습니다.</p>}
          {testResults.map((r, i) => (
            <div key={i} className={`test-result ${r.status}`}>
              <div className="test-result-header">
                <span className="test-name">{r.title}</span>
                <span className={`test-status ${r.status}`}>{r.status}</span>
                <span className="test-time">{r.time}</span>
              </div>
              <div className="test-result-body">
                <pre>{JSON.stringify(r.data, null, 2)}</pre>
              </div>
            </div>
          ))}
        </div>
        <button className="clear-results" onClick={() => setTestResults([])}>결과 초기화</button>
      </div>
    </div>
  );
}

export default TestPage; 