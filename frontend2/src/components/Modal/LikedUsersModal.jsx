import React, { useEffect, useState } from 'react';
import styles from './LikedUsersModal.module.css';
import userIcon from '../../assets/user_icon.png';

export default function LikedUsersModal({ isOpen, onClose, reviewId }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [followLoading, setFollowLoading] = useState({});
  const [myId, setMyId] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    if (!isOpen || !reviewId) return;
    setLoading(true);
    setError(null);
          fetch(`/api/reviews/${reviewId}/liked-users`, {
      credentials: 'include',
    })
      .then(res => res.json())
      .then(data => {
        if (data.success) {
          setUsers(data.data);
        } else {
          setError('유저 목록을 불러오지 못했습니다.');
        }
      })
      .catch(() => setError('네트워크 오류'))
      .finally(() => setLoading(false));
  }, [isOpen, reviewId]);

  // 내 id 가져오기 (로그인 상태 확인)
  useEffect(() => {
    if (!isOpen) return;
            fetch('/api/current-user', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        if (data.success && data.user && data.user.id) {
          setMyId(data.user.id);
          setIsLoggedIn(true);
        } else {
          setMyId(null);
          setIsLoggedIn(false);
        }
      })
      .catch(() => {
        setMyId(null);
        setIsLoggedIn(false);
      });
  }, [isOpen]);

  const handleFollow = async (userId, isFollowing) => {
    if (followLoading[userId]) return;
    setFollowLoading(prev => ({ ...prev, [userId]: true }));
    try {
      const method = isFollowing ? 'DELETE' : 'POST';
      const url = `/api/users/${userId}/${isFollowing ? 'unfollow' : 'follow'}`;
      const response = await fetch(url, {
        method,
        credentials: 'include',
      });
      if (response.ok) {
        setUsers(prev => prev.map(user => user.id === userId ? { ...user, isFollowing: !isFollowing } : user));
      } else {
        alert('팔로우 처리에 실패했습니다.');
      }
    } catch (error) {
      alert('네트워크 오류가 발생했습니다.');
    } finally {
      setFollowLoading(prev => ({ ...prev, [userId]: false }));
    }
  };

  if (!isOpen) return null;

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={e => e.stopPropagation()}>
        <div className={styles.header}>
          <h2 className={styles.title}>좋아요한 사람들</h2>
          <button className={styles.closeButton} onClick={onClose}>✕</button>
        </div>
        <div className={styles.usersSection}>
          {loading && <div>로딩 중...</div>}
          {error && <div style={{ color: 'red' }}>{error}</div>}
          {!loading && !error && users.length === 0 && <div>아직 좋아요한 사람이 없습니다.</div>}
          {!loading && !error && users.map((user, idx) => (
            <React.Fragment key={user.id}>
              <div className={styles.userRow}>
                <img
                  className={styles.profileImage}
                  src={user.profileImageUrl ? user.profileImageUrl.replace('/api/profile/images/', '/uploads/profile-images/') : userIcon}
                  alt="프로필"
                  onError={e => { e.target.src = userIcon; }}
                />
                <div className={styles.userInfo}>
                  <div className={styles.nickname}>{user.nickname}</div>
                  {/* subInfo 등 추가 정보 필요시 여기에 */}
                </div>
                {isLoggedIn && myId !== user.id && (
                  <button
                    className={`${styles.followBtn} ${user.isFollowing ? styles.following : ''}`}
                    onClick={() => handleFollow(user.id, user.isFollowing)}
                    disabled={followLoading[user.id]}
                  >
                    {followLoading[user.id] ? '처리중...' : user.isFollowing ? '언팔로우' : '팔로우'}
                  </button>
                )}
              </div>
              {idx !== users.length - 1 && <div className={styles.divider}></div>}
            </React.Fragment>
          ))}
        </div>
      </div>
    </div>
  );
} 