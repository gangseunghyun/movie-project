import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styles from './FindIdResultPage.module.css';
import checkIcon from '../../assets/check_green.png';
import failIcon from '../../assets/x_mark.png';

export default function FindIdResultPage() {
  const location = useLocation();
  const navigate = useNavigate();
  // maskedLoginId는 location.state 또는 쿼리스트링 등에서 받아옴
  const maskedLoginId = location.state?.maskedLoginId || '';

  const isSuccess = !!maskedLoginId;

  return (
    <div className={styles.outer}>
      <div className={styles.resultBox}>
        {isSuccess ? (
          <>
            <img src={checkIcon} alt="성공" className={styles.checkIcon} />
            <div className={styles.title}>입력하신 정보와 일치하는 결과입니다.</div>
            <div className={styles.desc}>
              개인정보 보호를 위해<br />
              아이디 또는 이메일의 일부만 제공합니다.
            </div>
            <div className={styles.idBox}>{maskedLoginId}</div>
            <button
              className={styles.loginBtn}
              onClick={() => navigate('/login')}
            >
              로그인 하러 가기
            </button>
          </>
        ) : (
          <>
            <img src={failIcon} alt="실패" className={styles.checkIcon} />
            <div className={styles.title} >일치하는 정보가 없습니다.</div>
            <div className={styles.desc}>
              입력하신 정보와 일치하는 아이디가 없습니다.<br />
              이메일을 다시 확인하거나 회원가입을 진행해 주세요.
            </div>
            <button
              className={styles.retry}
              onClick={() => navigate('/find-id')}
            >
              아이디 다시 찾기
            </button>
            <button
              className={styles.loginBtn}
              
              onClick={() => navigate('/login')}
            >
              로그인 하러 가기
            </button>
          </>
        )}
      </div>
    </div>
  );
} 