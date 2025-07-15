import React from "react";
import styles from "./FollowersModal.module.css";

const FollowersModal = ({ followers, onClose }) => {

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContent}>
        <div className={styles.modalHeader}>
          <div className={styles.modalHeaderLeft}>
            <h3 className={styles.title}>팔로워 목록</h3>
            <div className={styles.follwerCount}>{followers.length}명</div>
          </div>
          <button className={styles.closeBtn} onClick={onClose}>×</button>
        </div>
        <hr className={styles.divider} />
        <div className={styles.list}>
          {followers.length === 0 ? (
            <div className={styles.empty}>팔로워가 없습니다.</div>
          ) : (
            followers.map((follower) => (
              <div key={follower.id} className={styles.followerItem}>
                <img
                  src={follower.profileImageUrl || require("../../assets/user_icon.png")}
                  alt="프로필"
                  className={styles.profileImg}
                />
                <a 
                  href={`/mypage/${follower.id}`}
                  className={styles.nickname}
                  style={{ cursor: 'pointer', textDecoration: 'none', color: 'inherit' }}
                >
                  {follower.nickname}
                </a>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default FollowersModal; 