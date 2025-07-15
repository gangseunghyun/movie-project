import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import MyPageHeader from "./MyPageHeader";
import MyPageBody from "./MyPageBody";
import MyPageFooter from "./MyPageFooter";
import styles from "./MyPage.module.css";

const MyPage = () => {
  const { userId } = useParams();
  const [tempUserInfo, setTempUserInfo] = useState(null);

  useEffect(() => {
    // 항상 맨 위로 이동
    window.scrollTo(0, 0);
    // 모든 모달 닫기(커스텀 이벤트 활용)
    window.dispatchEvent(new Event('closeAllModals'));
    
    // sessionStorage에서 임시 유저 정보 확인 (새로고침 시에도 유지)
    const storedUserInfo = sessionStorage.getItem('tempUserInfo');
    if (storedUserInfo) {
      try {
        const userInfo = JSON.parse(storedUserInfo);
        setTempUserInfo(userInfo);
      } catch (error) {
        console.error('임시 유저 정보 파싱 실패:', error);
        sessionStorage.removeItem('tempUserInfo');
      }
    }
  }, []);

  return (
    <div className={styles.container}>
      <MyPageHeader targetUserId={tempUserInfo ? tempUserInfo.id : userId} tempUserInfo={tempUserInfo} />
      <MyPageBody targetUserId={tempUserInfo ? tempUserInfo.id : userId} tempUserInfo={tempUserInfo} />
      <MyPageFooter targetUserId={tempUserInfo ? tempUserInfo.id : userId} tempUserInfo={tempUserInfo} />
    </div>
  );
};

export default MyPage; 