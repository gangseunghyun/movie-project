import React, { useState, useEffect } from "react";
import styles from "./MyPageHeader.module.css";
import userProfile from "../../assets/user_icon.png";
import pencilIcon from "../../assets/pencil_icon.png";
import { useUser } from "../../contexts/UserContext";
import TagEditModal from "./TagEditModal";
import { useNavigate } from 'react-router-dom';
import ProfileImageUploadModal from '../Modal/ProfileImageUploadModal';
import FollowersModal from '../Modal/FollowersModal';
import FollowingModal from '../Modal/FollowingModal';

const MyPageHeader = ({ targetUserId, tempUserInfo, targetUser: propTargetUser }) => {
  const { user, setUser } = useUser();
  const navigate = useNavigate();

  // 태그 모달 상태 및 태그 상태 관리
  const [isTagModalOpen, setIsTagModalOpen] = useState(false);
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [targetUser, setTargetUser] = useState(propTargetUser);

  // 표시할 사용자 결정 (targetUser가 있으면 targetUser, localTempUserInfo가 있으면 localTempUserInfo, 없으면 현재 로그인한 user)
  const [localTempUserInfo, setLocalTempUserInfo] = useState(tempUserInfo);
  const displayUserId = (targetUser ? targetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id)));
  const isOwnPage = String(displayUserId) === String(user?.id);
  const displayUser = isOwnPage ? user : (targetUser || localTempUserInfo || user);
  


  // 팔로워/팔로잉 수 상태 추가
  const [followersCount, setFollowersCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isFollowersModalOpen, setIsFollowersModalOpen] = useState(false);
  const [followersList, setFollowersList] = useState([]);
  const [isFollowingModalOpen, setIsFollowingModalOpen] = useState(false);
  const [followingList, setFollowingList] = useState([]);

  // 팔로워/팔로잉 수 가져오기 함수 (useEffect보다 위에 선언)
  const fetchFollowCounts = async () => {
    const userId = targetUser ? targetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id));
    if (!userId) {
      console.log('사용자 정보가 없습니다.');
      return;
    }
    try {
      // 팔로워 수 가져오기
      const followersResponse = await fetch(`/api/users/${userId}/followers`, {
        credentials: 'include',
      });
      if (followersResponse.ok) {
        const followersData = await followersResponse.json();
        setFollowersCount(followersData.data.length);
      } else {
        console.error('팔로워 정보를 가져오는데 실패했습니다.');
      }
      // 팔로잉 수 가져오기 (API 엔드포인트가 있다면)
      const followingResponse = await fetch(`/api/users/${userId}/following`, {
        credentials: 'include',
      });
      if (followingResponse.ok) {
        const followingData = await followingResponse.json();
        setFollowingCount(followingData.data.length);
      } else {
        console.error('팔로잉 정보를 가져오는데 실패했습니다.');
      }
    } catch (error) {
      console.error('팔로워/팔로잉 정보를 가져오는 중 오류가 발생했습니다:', error);
    }
  };

  const fetchFollowersList = async () => {
    const userId = targetUser ? targetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id));
    if (!userId) return;
    try {
      const response = await fetch(`/api/users/${userId}/followers`, {
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();

        setFollowersList(data.data || []);
      } else {
        setFollowersList([]);
      }
    } catch {
      setFollowersList([]);
    }
  };

  const fetchFollowingList = async () => {
    const userId = targetUser ? targetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id));
    if (!userId) return;
    try {
      const response = await fetch(`/api/users/${userId}/following`, {
        credentials: 'include',
      });
      if (response.ok) {
        const data = await response.json();

        setFollowingList(data.data || []);
      } else {
        setFollowingList([]);
      }
    } catch {
      setFollowingList([]);
    }
  };

  // sessionStorage에서 tempUserInfo 확인 (새로고침 시에도 유지)
  useEffect(() => {
    const storedUserInfo = sessionStorage.getItem('tempUserInfo');
    if (storedUserInfo) {
      try {
        const userInfo = JSON.parse(storedUserInfo);
        setLocalTempUserInfo(userInfo);
        //console.log('임시 유저 정보 로드:', userInfo);

      } catch (error) {
        console.error('임시 유저 정보 파싱 실패:', error);
      }
    }
  }, []);

  // propTargetUser가 변경되면 targetUser 상태 업데이트
  useEffect(() => {
    if (propTargetUser) {
      setTargetUser(propTargetUser);
      setLoading(false);
    }
  }, [propTargetUser]);

  // 사용자의 선호 태그 가져오기
  useEffect(() => {
    const fetchUserPreferredGenres = async () => {
      const userId = targetUser ? targetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id));
      if (!userId) {
        console.log('사용자 정보가 없습니다.');
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(`/api/users/${userId}/preferred-genres`, {
          credentials: 'include',
        });
        if (response.ok) {
          const preferredGenres = await response.json();
          
          setTags(preferredGenres);
        } else {
          console.error('사용자 선호 태그를 가져오는데 실패했습니다.');
        }
      } catch (error) {
        console.error('사용자 선호 태그를 가져오는 중 오류가 발생했습니다:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUserPreferredGenres();
  }, [targetUserId, user, localTempUserInfo, targetUser]);

  // 마이페이지 진입 시 팔로워/팔로잉 수를 가져옴
  useEffect(() => {
    fetchFollowCounts();
  }, [targetUserId, user, localTempUserInfo]);

  // 팔로우 상태 확인 (자신의 페이지가 아닐 때만)
  useEffect(() => {
    if (isOwnPage || !displayUserId || !user?.id) return;

    const checkFollowStatus = async () => {
      try {
        // 현재 로그인한 사용자가 해당 유저를 팔로우하고 있는지 확인
        const response = await fetch(`/api/users/${displayUserId}/followers`, {
          credentials: 'include',
        });
        if (response.ok) {
          const data = await response.json();
          // data.data가 팔로워 배열이라고 가정
          const isFollowingMe = Array.isArray(data.data) && data.data.some(follower => String(follower.id) === String(user.id));
          setIsFollowing(isFollowingMe);
        } else {
          setIsFollowing(false);
        }
      } catch (error) {
        console.error('팔로우 상태 확인 중 오류가 발생했습니다:', error);
        setIsFollowing(false);
      }
    };

    checkFollowStatus();
  }, [displayUserId, isOwnPage, user?.id]);

  const handleFollowClick = async () => {
    if (!displayUserId || isLoading || isOwnPage) return;

    setIsLoading(true);
    try {
      const url = isFollowing 
        ? `/api/users/${displayUserId}/unfollow`
        : `/api/users/${displayUserId}/follow`;
      
      const method = isFollowing ? 'DELETE' : 'POST';
      
      const response = await fetch(url, {
        method: method,
        credentials: 'include',
      });

      console.log('팔로우 API 응답 상태:', response.status);
      
      if (response.ok) {
        const data = await response.json();
        console.log('팔로우 API 응답 데이터:', data);
        setIsFollowing(!isFollowing);
        await fetchFollowCounts(); // 팔로우/언팔로우 후 최신화
      } else {
        console.error('팔로우/언팔로우 실패, 상태:', response.status);
      }
    } catch (error) {
      console.error('팔로우/언팔로우 중 오류가 발생했습니다:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      const response = await fetch("/api/logout", {
        method: "POST",
        credentials: "include",
      });
      const data = await response.json();
      if (response.ok && data.success) {
        setUser(null);
        localStorage.removeItem("autoLogin");
        localStorage.removeItem("user");
        //("로그아웃 성공(test).");
        window.location.href = "/login";
      } else {
        alert(data.message || "로그아웃에 실패했습니다.");
      }
    } catch (error) {
      alert("서버 오류가 발생했습니다.");
    }
  };

  // 태그 수정 저장
  const handleSaveTags = (selectedTags) => {
    setTags(selectedTags);
    setIsTagModalOpen(false);
  };

  // 내 페이지일 때 세션 데이터 삭제
  useEffect(() => {
    if (isOwnPage) {
      sessionStorage.removeItem('tempUserInfo');
      setLocalTempUserInfo(null);
    }
  }, [isOwnPage]);

  const profileImageUrl = displayUser && displayUser.profileImageUrl ? displayUser.profileImageUrl.replace('/api/profile/images/', '/uploads/profile-images/') : userProfile;

  if (loading) {
    return (
      <header className={styles.header}>
        <div className={styles.profileSection}>
          <div className={styles.infoSection}>
            <div className={styles.loadingMessage}>사용자 정보를 불러오는 중...</div>
          </div>
        </div>
      </header>
    );
  }

  return (
    <header className={styles.header}>
      <div className={styles.profileSection}>
        <div className={styles.infoSection}>
          <div className={styles.leftSection}>
            <div className={styles.profileImgWrapper}>
              <img src={profileImageUrl} alt="프로필" className={styles.profileImg} />
              {isOwnPage && (
                <span
                  className={styles.profileImgSettingIcon}
                  onClick={() => setIsProfileModalOpen(true)}
                  title="프로필 사진 변경"
                  style={{
                    backgroundImage: `url(${require('../../assets/setting_icon.png')})`,
                    backgroundSize: 'cover',
                    backgroundPosition: 'center',
                  }}
                />
              )}
            </div>
            <div className={styles.userInfo}>
              <div className={styles.nicknameRow}>
                <span className={styles.nickname}>{displayUser?.nickname || "닉네임"}</span>
                {isOwnPage && (
                  <img src={pencilIcon} alt="닉네임 수정" className={styles.pencilIcon} style={{ cursor: 'pointer' }} onClick={() => navigate('/profile-edit')} />
                )}
              </div>
              <div
                className={styles.followStats}
                style={{ display: 'flex',  cursor: 'pointer' }}
              >
                <span
                  onClick={async () => {
                    await fetchFollowersList();
                    setIsFollowersModalOpen(true);
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  팔로워 {followersCount}명
                </span>
                <span>·</span>
                <span
                  onClick={async () => {
                    await fetchFollowingList();
                    setIsFollowingModalOpen(true);
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  팔로잉 {followingCount}명
                </span>
              </div>
              {!isOwnPage && (
                <button 
                  className={`${styles.followButton} ${isFollowing ? styles.following : ''}`}
                  onClick={handleFollowClick}
                  disabled={isLoading}
                >
                  {isLoading ? '' : (isFollowing ? '언팔로우' : '팔로우')}
                </button>
              )}
            </div>
          </div>
          <div className={styles.middleSection}>
            <div className={styles.tagRow}>
              <span className={styles.tagLabel}>선호 태그</span>
              {isOwnPage && (
                <img
                  src={pencilIcon}
                  alt="태그 수정"
                  className={styles.pencilIconTag}
                  onClick={() => setIsTagModalOpen(true)}
                  style={{ cursor: "pointer" }}
                />
              )}
            </div>
            <div className={styles.tagsWrapper}>
              {tags.length > 0 ? (
                tags.map((tag, idx) => (
                  <span className={styles.tag} key={tag + idx}>
                    #{tag}

                  </span>
                ))
              ) : (
                <span className={styles.noTagsMessage}>설정된 선호 태그가 없습니다.
                </span>

              )}
            </div>
          </div>
          <div className={styles.rightSection}>
            {isOwnPage && (
              <div className={styles.actionSection}>
                <button className={styles.logoutBtn} onClick={handleLogout}>
                  <span className={styles.logoutIcon} />로그아웃
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
      {isTagModalOpen && (
        <TagEditModal
          initialSelected={tags}
          onClose={() => setIsTagModalOpen(false)}
          onSave={handleSaveTags}
        />
      )}
      {isFollowersModalOpen && (
        <FollowersModal
          followers={followersList}
          onClose={() => setIsFollowersModalOpen(false)}
        />
      )}
      {isFollowingModalOpen && (
        <FollowingModal
          followings={followingList}
          onClose={() => setIsFollowingModalOpen(false)}
        />
      )}
      {/* 프로필 이미지 변경 모달 */}
      {isProfileModalOpen && (
        <ProfileImageUploadModal
          currentImageUrl={profileImageUrl}
          onImageUpdate={() => window.location.reload()}
          onClose={() => setIsProfileModalOpen(false)}
        />
      )}
    </header>
  );
};

export default MyPageHeader; 