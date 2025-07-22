import React, { useState } from 'react';
import styles from './FindIdPage.module.css';
import Footer from '../Footer/Footer';
import { useNavigate } from 'react-router-dom';

export default function FindIdPage() {
  const [email, setEmail] = useState('');
  const [result, setResult] = useState(null); // { success, message, maskedLoginId }
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setResult(null);
    try {
      const response = await fetch('/api/find-id', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });
      const data = await response.json();
      console.log(data);
      if (data.success && data.maskedLoginId) {
        //alert('아이디를 찾았습니다!');
        navigate('/find-id-result', { state: { maskedLoginId: data.maskedLoginId } });
      } else if (data.message && data.message.includes('소셜')) {
        alert(data.message);
        navigate('/login');
      } else {
        //alert(data.message || '일치하는 정보가 없습니다.');
        navigate('/find-id-result', { state: { maskedLoginId: '' } });
      }
    } catch (error) {
      alert('서버 오류가 발생했습니다.');
      navigate('/find-id-result', { state: { maskedLoginId: '' } });
    }
  };

  return (
    <div className={styles.outer}>
      <div className={styles.formBox}>
        <h1 className={styles.heading}>아이디 찾기</h1>
        <form onSubmit={handleSubmit}>
          <div className={styles.descBox}>
            <div className={styles.descTitle}>이메일로 찾기</div>
            <div className={styles.descSub}>가입 시 등록한 이메일을 입력해주세요.</div>
          </div>
          <div className={styles.buttonGroup}>
            <input
              type="email"
              placeholder="이메일"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className={styles.input}
              required
            />
            <button
              type="submit"
              className={styles.primaryBtn}
              disabled={!email}
            >
              확인
            </button>
            <button
              type="button"
              className={`${styles.secondaryBtn}`}
              onClick={() => navigate('/login')}
            >
              로그인으로 돌아가기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
} 