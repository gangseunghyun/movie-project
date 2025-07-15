import React, { useState, useEffect } from 'react';
import styles from "./SocialJoinComplete.module.css";
import { toast } from 'react-toastify';
import checkTrue2 from '../../assets/check_true2.png';
import checkFalse2 from '../../assets/check_false2.png';
import checkTrue from '../../assets/check_true.png';
import checkFalse from '../../assets/check_false.png';
import nextIcon from '../../assets/next.png';
import TermsModal from '../Modal/TermsModal';
import PrivacyModal from '../Modal/PrivacyModal';
import { useUser } from '../../contexts/UserContext';
import { useNavigate } from 'react-router-dom';


export default function FilmerExtraInfoPage() {
  const [showTermsModal, setShowTermsModal] = useState(false);
  const [showPrivacyModal, setShowPrivacyModal] = useState(false);
  const { setUser } = useUser();
  const [form, setForm] = useState({
    nickname: '',
    agreeAll: false,
    age: false,
    agreeTerms: false,
    agreePrivacy: false
  });

  const navigate = useNavigate();

  const handleChange = e => {
    const { name, value, type, checked } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));

  };

  useEffect(() => {
    const allChecked = form.age && form.agreeTerms && form.agreePrivacy;
    if (form.agreeAll !== allChecked) {
      setForm(prev => ({ ...prev, agreeAll: allChecked }));
    }
  }, [form.age, form.agreeTerms, form.agreePrivacy]);

  const isNicknameValid = /^[a-zA-Z가-힣0-9]{2,8}$/.test(form.nickname);
  const isFormValid =
    isNicknameValid &&
    form.agreeAll &&
    form.agreeTerms &&
    form.agreePrivacy;

  const [nickname, setNickname] = useState("");
  const [checked, setChecked] = useState({
    age: false,
    terms: false,
    privacy: false,
  });

  const handleToggle = (key) => {
    if (key === 'agreeAll') {
      const newValue = !form.agreeAll;
      setForm(prev => ({
        ...prev,
        agreeAll: newValue,
        age: newValue,
        agreeTerms: newValue,
        agreePrivacy: newValue,
      }));
    } else {
      setForm(prev => ({
        ...prev,
        [key]: !prev[key],
      }));
    }
  };

  const handleRecommendNickname = async () => {
    try {
      const response = await fetch('http://localhost:80/api/users/recommend-nickname');
      const data = await response.json();
      if (data.nicknames && data.nicknames.length > 0) {
        // 예시: 첫 번째 추천 닉네임을 입력란에 자동으로 넣기
        setForm(prev => ({
          ...prev,
          nickname: data.nicknames[0]
        }));

      } else {
        toast.error("추천 닉네임을 가져오지 못했습니다.");
      }
    } catch (error) {
      toast.error("닉네임 추천 중 오류가 발생했습니다.");
    }
  };

  //소셜 회원가입
  const handleSocialJoinComplete = async (e) => {
    e.preventDefault();

    try {
      const response = await fetch('http://localhost:80/api/social-join-complete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          nickname: form.nickname,
          agree: form.agreeAll // 또는 true (약관 전체 동의 체크)
        })
      });

      const data = await response.json();

      if (data.success) {
        // 소셜 회원가입 성공 후 현재 유저 정보 받아오기
        fetch('http://localhost:80/api/current-user', { credentials: 'include' })
          .then(res => res.json())
          .then(currentUserData => {
            if (currentUserData.success && currentUserData.user) {
              setUser(currentUserData.user);
              localStorage.setItem('user', JSON.stringify(currentUserData.user));
              localStorage.setItem('autoLogin', 'true');
              setTimeout(() => {
                navigate('/');
              }, 100);
            }
          });
        toast.success(data.message || "소셜 회원가입이 완료되었습니다!");
      } else {
        toast.error(data.message || "회원가입에 실패했습니다.");
      }
    } catch (error) {
      toast.error("서버 오류가 발생했습니다.");
    }
  };

  const allChecked = checked.age && checked.terms && checked.privacy && nickname;

  return (
    <div className={styles.outer}>
      <h3 className={styles.title}>추가 정보 입력</h3>
      <div className={styles.inputGroup}>
        <div className={styles.inputGroupRow}>
          <input
            name="nickname"
            placeholder="닉네임"
            className={styles.input}
            value={form.nickname}
            onChange={handleChange}
          />
          <button type="button" className={styles.button} onClick={handleRecommendNickname}>닉네임 추천</button>
        </div>
        <p className={`${styles.inputDesc} ${!isNicknameValid && form.nickname ? styles.invalid : ''}`}>닉네임은 영문 또는 한글 2~8자</p>
      </div>

      <div className={styles.checkboxGroup}>
        <div className={styles.checkboxRow} onClick={() => handleToggle('agreeAll')}>
          <img className={styles.bigCheckIcon} src={form.agreeAll ? checkTrue : checkFalse} alt="check" />
          <span>필수 및 선택 항목을 모두 포함하여 동의합니다.</span>
        </div>
        <div className={styles.checkboxRow} onClick={() => handleToggle('age')}>
          <img src={form.age ? checkTrue2 : checkFalse2} alt="check" />
          <span>만 14세 이상입니다.</span>

        </div>
        <div className={styles.checkboxRow} onClick={() => handleToggle('agreeTerms')}>
          <img src={form.agreeTerms ? checkTrue2 : checkFalse2} alt="check" />
          <span>[필수] 서비스 이용약관 동의</span>
          <img
            className={styles.nextIcon}
            src={nextIcon}
            alt=">"
            style={{ cursor: 'pointer' }}
            onClick={e => {
              e.stopPropagation(); // 이벤트 버블링 방지!
              setShowTermsModal(true);
            }
            }
          />
        </div>
        <div className={styles.checkboxRow} onClick={() => handleToggle('agreePrivacy')}>
          <img src={form.agreePrivacy ? checkTrue2 : checkFalse2} alt="check" />
          <span>[필수] 개인정보 수집 및 이용 동의</span>
          <img
            className={styles.nextIcon}
            src={nextIcon}
            alt=">"
            style={{ cursor: 'pointer' }}
            onClick={e => {
              e.stopPropagation(); // 이벤트 버블링 방지
              setShowPrivacyModal(true);
            }}
          />
        </div>
        <button
          type="submit"
          className={styles.submitBtn}
          disabled={!isFormValid}
          onClick={handleSocialJoinComplete}
        >
          가입하기
        </button>

        <p className={styles.loginLink}>
          이미 계정이 있으신가요? <a href="/login">로그인</a>
        </p>
      </div>
      <TermsModal show={showTermsModal} onClose={() => setShowTermsModal(false)} />
      <PrivacyModal show={showPrivacyModal} onClose={() => setShowPrivacyModal(false)} />
    </div>
  );
}