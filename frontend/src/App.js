import React from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import SignUpPage from './SignUpPage';
import LoginPage from './LoginPage';
import SocialLoginPage from './SocialLoginPage';
import FindIdPage from './FindIdPage';
import FindPwPage from './FindPwPage';
import ResetPasswordPage from './ResetPasswordPage';
import SocialJoinPage from './SocialJoinPage';
import { useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';

function MainPage() {
  const location = useLocation();
  const [message, setMessage] = useState('');
  const [keyword, setKeyword] = useState('');
  const [recentKeywords, setRecentKeywords] = useState([]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const nickname = params.get('nickname');
    if (nickname) {
      setMessage(`${nickname}님 환영합니다!`);
    } else {
      setMessage('');
    }
  }, [location]);

  // 최근 검색어 불러오기
  useEffect(() => {
    fetch('http://localhost:8080/api/search-history', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        setRecentKeywords(Array.isArray(data) ? data.map(item => item.keyword) : []);
      });
  }, []);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!keyword.trim()) return;

    // 1. 검색어 저장
    await fetch(`http://localhost:8080/api/search-history?keyword=${encodeURIComponent(keyword)}`, {
      method: 'POST',
      credentials: 'include'
    });

    // 2. 검색 API 호출 (파라미터 이름은 query로 가정)
    const res = await fetch(`http://localhost:8080/api/search?query=${encodeURIComponent(keyword)}`, {
      credentials: 'include'
    });
    const data = await res.json();
    console.log('검색 결과:', data);
    // 추후: 검색 결과를 화면에 렌더링

    // 3. 검색 후 최근 검색어 갱신
    fetch('http://localhost:8080/api/search-history', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        setRecentKeywords(Array.isArray(data) ? data.map(item => item.keyword) : []);
      });
  };

  return (
    <div style={{ display: 'flex', alignItems: 'flex-start' }}>
      <div style={{ flex: 1 }}>
        {message && <div style={{ color: 'green', marginBottom: 16 }}>{message}</div>}
        <h2>메인 페이지</h2>
        <form onSubmit={handleSearch} style={{ marginBottom: 16 }}>
          <input
            type="text"
            placeholder="영화 검색어를 입력하세요"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            style={{ width: 240, marginRight: 8 }}
          />
          <button type="submit">검색</button>
        </form>
        {/* 추후: 검색 결과 리스트 렌더링 */}
      </div>
      <div style={{ width: 200, marginLeft: 32 }}>
        <h4>최근 검색어</h4>
        <ul>
          {recentKeywords.length === 0 && <li style={{ color: '#aaa' }}>최근 검색어 없음</li>}
          {recentKeywords.map((kw, idx) => (
            <li key={idx}>{kw}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function App() {
  // 로그아웃 핸들러
  const handleLogout = async () => {
    await fetch('http://localhost:8080/logout', { method: 'POST', credentials: 'include' });
    window.location.href = '/';
  };

  return (
    <BrowserRouter>
      <nav style={{ margin: 20 }}>
        <Link to="/" style={{ marginRight: 10 }}>메인</Link>
        <Link to="/signup" style={{ marginRight: 10 }}>회원가입</Link>
        <Link to="/login" style={{ marginRight: 10 }}>로그인</Link>
        <Link to="/social" style={{ marginRight: 10 }}>소셜로그인</Link>
        <Link to="/find-id" style={{ marginRight: 10 }}>아이디 찾기</Link>
        <Link to="/find-pw" style={{ marginRight: 10 }}>비밀번호 찾기</Link>
        <button onClick={handleLogout} style={{ marginLeft: 20 }}>로그아웃</button>
      </nav>
      <Routes>
        <Route path="/" element={<MainPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/social" element={<SocialLoginPage />} />
        <Route path="/find-id" element={<FindIdPage />} />
        <Route path="/find-pw" element={<FindPwPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/social-join" element={<SocialJoinPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
