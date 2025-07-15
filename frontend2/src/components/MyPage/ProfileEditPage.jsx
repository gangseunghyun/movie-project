import React, { useState } from 'react';
import styles from './ProfileEditPage.module.css';
import { useNavigate } from 'react-router-dom';
import { useUser } from '../../contexts/UserContext';

export default function ProfileEditPage() {
  const [nickname, setNickname] = useState('');
  const [error, setError] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const navigate = useNavigate();
  const { user } = useUser();

  // 닉네임 유효성 검사 함수
  const validateNickname = (value) => {
    if (value.length < 2 || value.length > 10) {
      return '닉네임은 2자 이상 10자 이내여야 합니다.';
    }
    if (!/^[가-힣a-zA-Z0-9]+$/.test(value)) {
      return '한글, 영문, 숫자만 입력 가능합니다.';
    }
    return '';
  };

  // 닉네임 저장 핸들러 (API 연동 필요시 추가)
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (error) return;
    try {
      const response = await fetch('http://localhost:80/api/update-nickname', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ nickname }),
        credentials: 'include',
      });
      const data = await response.json();
      if (data.success) {
        alert('프로필이 수정되었습니다!');
        navigate(-1);
        setTimeout(() => {
          window.location.reload();
        }, 100);
      } else {
        alert(data.message || '닉네임 변경에 실패했습니다.');
      }
    } catch (error) {
      alert('서버 오류가 발생했습니다.');
    }
  };

  // input value 결정
  const inputValue =
    !isFocused && nickname === '' && user && user.nickname
      ? user.nickname
      : nickname;

  // 닉네임 유효성 검사 통과 여부
  const isNicknameValid = !error && nickname.length >= 2 && nickname.length <= 10;

  return (
    <div className={styles.outer}>
      <div className={styles.formBox}>
        <h1 className={styles.heading}>닉네임 편집</h1>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder={user && user.nickname ? user.nickname : "닉네임"}
            value={inputValue}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            onChange={e => {
              const value = e.target.value;
              setNickname(value);
              setError(validateNickname(value));
            }}
            className={styles.input}
            minLength={2}
            maxLength={10}
            required
          />
          {/* 에러가 있으면 빨간 글씨로, 없으면 안내문구 */}
          {error ? (
            <div className={styles.errorMsg}>{error}</div>
          ) : (
            <div className={styles.descSub}>*2자 이상 10자 이내의 한글, 영문, 숫자 입력 가능합니다.</div>
          )}
          <div className={styles.buttonGroup}>
            <button type="submit" className={styles.primaryBtn} disabled={!isNicknameValid}>확인</button>
            <button type="button" className={styles.secondaryBtn} onClick={() => navigate(-1)}>취소</button>
          </div>
        </form>
      </div>
    </div>
  );
} 