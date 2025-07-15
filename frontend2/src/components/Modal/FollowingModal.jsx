import React from "react";
import styles from "./FollowingModal.module.css";

const FollowingModal = ({ followings, onClose }) => {

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContent}>
        <div className={styles.modalHeader}>
          <div className={styles.modalHeaderLeft}>
            <h3 className={styles.title}>팔로잉 목록</h3>
            <div className={styles.followingCount}>{followings.length}명</div>
          </div>
          <button className={styles.closeBtn} onClick={onClose}>×</button>
        </div>
        <hr className={styles.divider} />
        <div className={styles.list}>
          {followings.length === 0 ? (
            <div className={styles.empty}>팔로잉이 없습니다.</div>
          ) : (
            followings.map((following, idx) => (
              <div key={following.id} className={styles.followerItem}>
                <img
                  src={following.profileImageUrl || require("../../assets/user_icon.png")}
                  alt="프로필"
                  className={styles.profileImg}
                />
                <a 
                  href={`/mypage/${following.id}`}
                  className={styles.nickname}
                  style={{ cursor: 'pointer', textDecoration: 'none', color: 'inherit' }}
                >
                  {following.nickname}
                </a>
                {idx !== followings.length - 1 && <div className={styles.divider} />}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default FollowingModal; 