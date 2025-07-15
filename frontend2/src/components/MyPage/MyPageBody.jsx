import React, { useEffect, useState } from "react";
import styles from "./MyPageBody.module.css";
import MovieCard from '../MainPage/MovieCard';
import MovieHorizontalSlider from '../MainPage/MovieHorizontalSlider';
import previousIcon from '../../assets/previous_icon.png';
import nextIcon from '../../assets/next_icon.png';
import { useUser } from '../../contexts/UserContext';
import { Link } from 'react-router-dom';

// SearchResultPage.jsx에서 복사한 ActorCard, ActorHorizontalSlider
function ActorCard({ actor }) {
  // 인물 상세페이지 링크 생성
  const personLink = actor.role === '감독'
    ? `/person/director/${actor.id}`
    : `/person/actor/${actor.id}`;
  return (
    <Link to={personLink} style={{ textDecoration: 'none', color: 'inherit' }}>
      <div className={styles.actorCard}>
        <div
          className={styles.actorImg}
          style={{
            backgroundImage: actor.photoUrl ? `url(${actor.photoUrl})` : 'none',
            backgroundSize: 'cover',
            backgroundPosition: 'center'
          }}
        ></div>
        <div className={styles.actorName}>{actor.name}</div>
        <div className={styles.actorRole}>{actor.role}</div>
      </div>
    </Link>
  );
}

function ActorHorizontalSlider({ data }) {
  // MovieHorizontalSlider 구조 참고
  const [start, setStart] = React.useState(0);
  // 카드 크기와 gap은 MovieHorizontalSlider와 동일하게 적용
  React.useEffect(() => {
    // CSS 변수 적용을 위해 마운트 시 한 번만 실행
    setCardWidth(parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--card-width')) || 180);
    setCardGap(17);
  }, []);
  function remToPx(rem) {
    return rem * parseFloat(getComputedStyle(document.documentElement).fontSize);
  }
  const [cardWidth, setCardWidth] = React.useState(180);
  const [cardGap, setCardGap] = React.useState(16);
  const visible = 9;
  const prev = () => setStart(Math.max(0, start - visible));
  const next = () => setStart(Math.min(data.length - visible, start + visible));

  return (
    <div className={styles.sliderWrapper}>
      {start > 0 && (
        <button
          className={`${styles.navBtn} ${styles.left}`}
          style={{ top: '30%' }}
          onClick={prev}
        >
          <img src={previousIcon} alt="이전" />
        </button>
      )}
      <div
        className={`${styles.slider} ${start === 0 ? styles.first : ''}`}
        style={{
          transform: `translateX(-${start * (cardWidth + cardGap)}px)`,
          transition: 'transform 0.4s'
        }}
      >
        {data.map((actor, idx) => (
          <ActorCard key={actor.name + actor.role} actor={actor} />
        ))}
      </div>
      {start + visible < data.length && (
        <button
          className={`${styles.navBtn} ${styles.right}`}
          style={{ top: '30%  ' }}
          onClick={next}
        >
          <img src={nextIcon} alt="다음" />
        </button>
      )}
    </div>
  );
}

// 더미 likedMovies 제거

const MyPageBody = ({ targetUserId, tempUserInfo, targetUser: propTargetUser }) => {
  const { user, isLoading: userLoading } = useUser();
  const [likedMovies, setLikedMovies] = useState([]);
  const [likedActors, setLikedActors] = useState([]);
  const [likedDirectors, setLikedDirectors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actorsLoading, setActorsLoading] = useState(true);
  const [directorsLoading, setDirectorsLoading] = useState(true);
  const [localTempUserInfo, setLocalTempUserInfo] = useState(tempUserInfo);

  // sessionStorage에서 tempUserInfo 확인 (새로고침 시에도 유지)
  useEffect(() => {
    const storedUserInfo = sessionStorage.getItem('tempUserInfo');
    if (storedUserInfo) {
      try {
        const userInfo = JSON.parse(storedUserInfo);
        setLocalTempUserInfo(userInfo);
        
      } catch (error) {
        console.error('임시 유저 정보 파싱 실패:', error);
      }
    }
  }, []);

  // 표시할 유저 결정 (propTargetUser가 있으면 propTargetUser, localTempUserInfo가 있으면 localTempUserInfo, 없으면 현재 로그인한 user)
  const isOwnPage = String(targetUserId || user?.id) === String(user?.id);
  const displayUserId = isOwnPage ? user?.id : (propTargetUser ? propTargetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id)));
  const displayUser = isOwnPage ? user : (propTargetUser || localTempUserInfo || user);

  

  useEffect(() => {
    if (!displayUserId) return;
    setLoading(true);
    fetch(`http://localhost:80/api/users/${displayUserId}/liked-movies`)
      .then(res => res.json())
      .then(data => setLikedMovies(data.data || []))
      .catch(() => setLikedMovies([]))
      .finally(() => setLoading(false));
  }, [displayUserId]);

  useEffect(() => {
    if (!displayUserId) return;
    setDirectorsLoading(true);
    fetch(`http://localhost:80/api/users/${displayUserId}/liked-directors`)
      .then(res => res.json())
      .then(data => setLikedDirectors(data.data || []))
      .catch(() => setLikedDirectors([]))
      .finally(() => setDirectorsLoading(false));
  }, [displayUserId]);

  useEffect(() => {
    if (!displayUserId) return;
    setActorsLoading(true);
    fetch(`http://localhost:80/api/users/${displayUserId}/liked-actors`)
      .then(res => res.json())
      .then(data => setLikedActors(data.data || []))
      .catch(() => setLikedActors([]))
      .finally(() => setActorsLoading(false));
  }, [displayUserId]);

  // 배우와 감독을 합쳐서 전달 (role 추가)
  const likedPeople = [
    ...likedActors.map(actor => ({ ...actor, role: '배우' })),
    ...likedDirectors.map(director => ({ ...director, role: '감독' }))
  ];
  const peopleLoading = actorsLoading || directorsLoading;

  return (
    <main className={styles.body}>
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>
          {isOwnPage ? '내가 찜한 영화' : `${displayUser?.nickname}님이 찜한 영화`}
        </h2>
        {userLoading || loading ? (
          <div>로딩 중...</div>
        ) : (
          likedMovies.length === 0 ? (
            <div className={styles.emptyMessage}>
              {isOwnPage ? '아직 찜한 영화가 없습니다.' : '아직 찜한 영화가 없습니다.'}
            </div>
          ) : (
            <MovieHorizontalSlider data={likedMovies} sectionKey="like" />
          )
        )}
      </section>
      <hr className={styles.divider} />
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>
          {isOwnPage ? '내가 좋아요한 인물' : `${displayUser?.nickname}님이 좋아요한 인물`}
        </h2>
        {userLoading || peopleLoading ? (
          <div>로딩 중...</div>
        ) : (
          likedPeople.length === 0 ? (
            <div className={styles.emptyMessage}>
              {isOwnPage ? '아직 좋아요한 인물이 없습니다.' : '아직 좋아요한 인물이 없습니다.'}
            </div>
          ) : (
            <ActorHorizontalSlider data={likedPeople} />
          )
        )}
      </section>
      <hr className={styles.divider} />
    </main>
  );
};

export default MyPageBody; 