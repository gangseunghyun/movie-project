// src/components/FilmerLoginPage/FilmerLoginPage.jsx
import React, { useState, useRef, useEffect } from 'react';
import styles from './FilmerLoginPage.module.css';
import eyeOpen from '../../assets/eye_icon.png';
import eyeClosed from '../../assets/closedeye_icon.png';
import checkFalse from '../../assets/check_false.png';
import checkTrue from '../../assets/check_true.png';
import { useNavigate } from 'react-router-dom';
import { useUser } from '../../contexts/UserContext';

export default function FilmerLoginPage() {
  const [form, setForm] = useState({ username: '', password: '' });
  const [showPw, setShowPw] = useState(false);
  const [autoLogin, setAutoLogin] = useState(false);
  const navigate = useNavigate();
  const { user, setUser } = useUser();
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [menuVisible, setMenuVisible] = useState(false);

  const handleChange = e => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };


  //로그인하기
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch('/api/user-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          loginId: form.username,
          password: form.password
        }),
        credentials: 'include' // 세션/쿠키 인증 시 필요
      });
      const data = await response.json();
      if (response.ok && data.success) {
        // 로그인 성공
        setShowProfileMenu(false);
        setMenuVisible(false);
        setUser(data.user);
        if (autoLogin) {
          // 자동 로그인 체크 시 localStorage에 저장
          localStorage.setItem('autoLogin', 'true');
          localStorage.setItem('user', JSON.stringify(data.user));
        } else {
          localStorage.removeItem('autoLogin');
          localStorage.removeItem('user');
        }
        //alert(`${data.user.nickname}님, 환영합니다!`);
        // 또는 toast.success(`${data.user.nickname}님, 환영합니다!`);
        // 메인 페이지 등으로 이동
        navigate('/');
      } else {
        alert(data.message || '로그인에 실패했습니다.');
        // 또는 toast.error(data.message || '로그인에 실패했습니다.');
      }
    } catch (error) {
      alert('서버 오류가 발생했습니다.');
      // 또는 toast.error('서버 오류가 발생했습니다.');
    }
  };

  const handleLogin = () => {
    // 자체 ID/PW 로그인 폼으로 이동
    navigate('/login');
  };

  return (
    <div className={styles.outer}>
      <h1 className={styles.title}>Filmer ID 로그인</h1>

      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.inputGroup}>
          <input
            name="username"
            type="text"
            placeholder="아이디"
            value={form.username}
            onChange={handleChange}
            required
            className={styles.input}
          />
        </div>

        <div className={styles.inputGroup}>
          <input
            name="password"
            type={showPw ? 'text' : 'password'}
            placeholder="비밀번호"
            value={form.password}
            onChange={handleChange}
            required
            className={styles.input}
          />
          <button
            type="button"
            className={styles.eyeBtn}
            onClick={() => setShowPw(v => !v)}
          >
            <img
              src={showPw ? eyeOpen : eyeClosed}
              alt={showPw ? "비밀번호 숨김" : "비밀번호 표시"}
              className={styles.eyeIcon}
            />
          </button>
        </div>

        <label
          className={styles.autoLogin}
          onClick={() => setAutoLogin(v => !v)}
        >
          <img
            src={autoLogin ? checkTrue : checkFalse}
            alt="자동 로그인"
            className={styles.checkIcon}
          />
          <span>자동 로그인</span>
        </label>

        <button
          type="submit"
          className={styles.primaryBtn}
          disabled={!(form.username && form.password)}
        >
          로그인하기
        </button>
        <button type="button" className={styles.secondaryBtn} onClick={handleLogin}>
          계정선택으로 돌아가기
        </button>
      </form>

      <div className={styles.links}>
        <a href="/find-id">아이디 찾기</a><span>|</span>
        <a href="/find-pw">비밀번호 찾기</a><span>|</span>
        <a href="/signup">회원가입</a>
      </div>

      <p className={styles.note}>
        이 사이트는 Google reCAPTCHA로 보호되며,<br></br>&nbsp;
        <a href="https://policies.google.com/privacy" target="_blank" rel="noreferrer">
          Google 개인정보 처리방침
        </a>
        와&nbsp;
        <a href="https://policies.google.com/terms" target="_blank" rel="noreferrer">
          서비스 약관
        </a>이 적용됩니다.
      </p>
    </div >
  );
}
