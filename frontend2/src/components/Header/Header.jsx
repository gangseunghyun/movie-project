import React, { useState, useRef, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import styles from './Header.module.css';
import logo from '../../assets/logo.png';
import { useUser } from '../../contexts/UserContext';
import searchIcon from '../../assets/search_icon.png';
import userProfile from '../../assets/user_profile.png';
import SearchModal from '../Modal/SearchModal';
import closeIcon from '../../assets/close_icon.png';

export default function Header() {
  const { user, setUser, isLoading } = useUser();
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [menuVisible, setMenuVisible] = useState(false);
  const menuTimeout = useRef(null);
  const [showSearchModal, setShowSearchModal] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const SERVER_URL = "https://ec2-13-222-249-145.compute-1.amazonaws.com";

  // user 객체 전체 디버깅용 출력
  useEffect(() => {
    console.log('user 전체 정보:', user);
  }, [user]);

  useEffect(() => {
    setShowProfileMenu(false);
    setMenuVisible(false);
  }, [user]);

  // 영화 등록 핸들러
  const handleMovieRegister = () => {
    if (!user || user.role !== 'ADMIN') {
      alert('관리자만 영화를 등록할 수 있습니다.');
      return;
    }
    
    // 영화 등록 페이지로 이동
    navigate('/admin/movie/register');
  };

  //로그아웃
  const handleLogout = async () => {
    console.log('로그아웃 시작');
    try {
              const response = await fetch('/api/logout', {
        method: 'POST',
        credentials: 'include', // 세션/쿠키 인증 시 필요
      });
      const data = await response.json();
      console.log('로그아웃 응답:', data);
      if (response.ok && data.success) {
        console.log('로그아웃 성공, 사용자 상태 초기화');
        setUser(null); // Context에서 유저 정보 제거(로그아웃)
        // 필요하다면 로그아웃 후 메인 페이지로 이동
        // navigate('/');
        localStorage.removeItem('autoLogin');
        localStorage.removeItem('user');
        //alert('로그아웃 성공(test).');
        window.location.href = "http://localhost:3000/login";
      } else {
        alert(data.message || '로그아웃에 실패했습니다.');
      }
    } catch (error) {
      console.error('로그아웃 오류:', error);
      alert('서버 오류가 발생했습니다.');
    }
  };

  const handleProfileEnter = () => {
    if (menuTimeout.current) clearTimeout(menuTimeout.current);
    setShowProfileMenu(true);
    setMenuVisible(true);
    setTimeout(() => setMenuVisible(true), 10);
  };

  const handleProfileLeave = () => {
    menuTimeout.current = setTimeout(() => {
      setMenuVisible(false);
      setTimeout(() => setShowProfileMenu(false), 350);
    }, 100);
  };

  // 언마운트 시 타이머 정리
  React.useEffect(() => {
    return () => {
      if (menuTimeout.current) clearTimeout(menuTimeout.current);
    };
  }, []);

  // 로딩 중에는 헤더를 렌더링하지 않음
  if (isLoading) {
    return null;
  }

  return (
    <header className={`${styles.stickyHeader} ${showSearchModal ? styles.activeSearch : ''}`}>
      <div className={styles.headerContent}>
        <div className={styles.inner}>
          <Link to="/" className={styles.logo}
            onClick={() => setShowSearchModal(false)}
          >
            <img src={logo} alt="Filmer" />
          </Link>
          
          {/* 영화 탭 추가 */}
          <div className={styles.navTabs}>
            <Link to="/movies" className={`${styles.navTab} ${location.pathname.startsWith('/movies') ? styles.active : ''}`}>
              영화
            </Link>
          </div>
          
          <nav className={styles.nav}>
            {user && user.role === 'ADMIN' && (
              <div className={styles.adminSection}>
                <button className={styles.adminRegisterBtn} onClick={handleMovieRegister}>
                  영화 등록
                </button>
                <div className={styles.adminNotice}>
                  관리자 계정으로 로그인 중입니다
                </div>
              </div>
            )}
            {(!location.pathname.startsWith('/search')) && (
              showSearchModal ? (
                <button className={styles.searchCloseBtn} onClick={() => setShowSearchModal(false)}>
                  <img src={closeIcon} alt="닫기" style={{ width: '100%', height: '100%' }} />
                </button>
              ) : (
                <img src={searchIcon} alt="검색" className={styles.icon} onClick={() => setShowSearchModal(true)} />
              )
            )}
            {user ? (
              <div className={styles.profileWrapper} style={{ display: 'inline-block', position: 'relative' }}>
                <div
                  className={styles.profileInline}
                  onMouseEnter={handleProfileEnter}
                  onMouseLeave={handleProfileLeave}
                >
                  <img
                                src={user.profileImageUrl && user.profileImageUrl.trim() !== ''
              ? user.profileImageUrl.replace('/api/profile/images/', '/uploads/profile-images/')
              : userProfile}
                    alt="프로필"
                    className={styles.profileImg}
                    onError={e => { e.target.onerror = null; e.target.src = userProfile; }}
                  />
                  <span className={styles.nickname}>{user.nickname}</span>
                </div>
                {showProfileMenu && (
                  <div
                    className={`${styles.profileMenu} ${menuVisible ? styles.menuShow : styles.menuHide}`}
                    onMouseEnter={handleProfileEnter}
                    onMouseLeave={handleProfileLeave}
                  >
                    <div className={styles.profileTop}>
                      <img
                        src={user.profileImageUrl && user.profileImageUrl.trim() !== '' 
                          ? user.profileImageUrl.replace('/api/profile/images/', '/uploads/profile-images/')
                          : userProfile}
                        alt="프로필"
                        className={styles.menuProfileImg}
                      />
                      <div>
                        <div className={styles.menuNickname}>{user.nickname}</div>
                        {/* <div className={styles.menuSwitch}>프로필 전환 &gt;</div> */}
                      </div>
                    </div>
                    <div className={styles.menuDivider} />
                    <div className={styles.menuItem} onClick={() => navigate(`/mypage/${user.id}`)}>MY</div>
                    <div className={styles.menuItem} onClick={() => navigate('/reservations')}>예매정보</div>
                    <div className={styles.menuItem}>고객센터</div>
                    <div className={styles.menuItem} onClick={handleLogout}>로그아웃</div>
                  </div>
                )}
              </div>
            ) : (
              <Link to="/login">로그인</Link>
            )}
          </nav>
        </div>
      </div>
      {showSearchModal && (
        <SearchModal
          open={showSearchModal}
          onClose={() => setShowSearchModal(false)}
          top={64}
          height={'80vh'}
        />
      )}
    </header>
  );
}