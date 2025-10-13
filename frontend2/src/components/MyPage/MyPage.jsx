import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import MyPageHeader from "./MyPageHeader";
import MyPageBody from "./MyPageBody";
import MyPageFooter from "./MyPageFooter";
import styles from "./MyPage.module.css";

const MyPage = () => {
  const { userId } = useParams();
  const [tempUserInfo, setTempUserInfo] = useState(null);
  const [targetUser, setTargetUser] = useState(null);

  // targetUserId로 사용자 정보 조회
  useEffect(() => {
    const fetchTargetUser = async () => {
      if (!userId) return;

      // tempUserInfo가 있으면 우선적으로 사용
      if (tempUserInfo && tempUserInfo.id === userId) {
        setTargetUser(tempUserInfo);
        return;
      }

      // userId로 사용자 정보 조회
      try {
        const response = await fetch(`/api/users/${userId}/info`, {
          credentials: 'include',
        });
        
        if (response.ok) {
          const userData = await response.json();
          setTargetUser(userData);
        } else {
          console.error('사용자 정보를 가져오는데 실패했습니다.');
        }
      } catch (error) {
        console.error('사용자 정보를 가져오는 중 오류가 발생했습니다:', error);
      }
    };

    fetchTargetUser();
  }, [userId, tempUserInfo]);

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
      <MyPageHeader targetUserId={userId} tempUserInfo={tempUserInfo} targetUser={targetUser} />
      <MyPageBody targetUserId={userId} tempUserInfo={tempUserInfo} targetUser={targetUser} />
      <MyPageFooter targetUserId={userId} tempUserInfo={tempUserInfo} targetUser={targetUser} />
    </div>
  );
};

export default MyPage; 