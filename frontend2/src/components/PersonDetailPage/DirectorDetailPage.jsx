import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import styles from './DirectorDetailPage.module.css';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import MovieHorizontalSlider from '../MainPage/MovieHorizontalSlider';
import userIcon from '../../assets/user_icon.png';

export default function DirectorDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [person, setPerson] = useState(null);
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [likedByMe, setLikedByMe] = useState(false);
  const [likeCount, setLikeCount] = useState(0);

  // 좋아요 상태 조회 함수 (useEffect 밖으로 이동)
  const fetchLikeStatus = async () => {
    try {
      const res = await fetch(`/api/person/director/${id}/like-status`, {
        credentials: 'include',
      });
      if (res.ok) {
        const data = await res.json();
        setLikedByMe(data.likedByMe);
        setLikeCount(data.likeCount);
      }
    } catch (e) {
      // 무시 또는 에러 처리
    }
  };
  useEffect(() => {
    window.scrollTo(0, 0);
    async function fetchActorDetail() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`/api/person/director/${id}`, {
          credentials: 'include',
        });
        if (!res.ok) throw new Error('감독 정보를 불러오지 못했습니다.');
        const data = await res.json();
        console.log(data);
        setPerson(data.person);
        setMovies(data.movies);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    if (id) {
      fetchActorDetail();
      fetchLikeStatus();
    }
  }, [id]);

  // 좋아요 토글 함수 수정
  const handleLikeToggle = async () => {
    const prevLikedByMe = likedByMe;
    const prevLikeCount = likeCount;

    // optimistic update
    if (!likedByMe) {
      setLikedByMe(true);
      setLikeCount(prev => prev + 1);
    } else {
      setLikedByMe(false);
      setLikeCount(prev => (prev > 0 ? prev - 1 : 0));
    }

    try {
      const method = prevLikedByMe ? 'DELETE' : 'POST';
      const res = await fetch(`/api/person/director/${id}/like`, {
        method,
        credentials: 'include',
      });
      if (res.status === 401) {
        alert('로그인이 필요합니다.');
        navigate('/login');
        setLikedByMe(prevLikedByMe);
        setLikeCount(prevLikeCount);
        return;
      }
      if (res.ok) {
        // POST/DELETE 모두 상태 재조회
        fetchLikeStatus();
      } else {
        const data = await res.json();
        alert(data.message || '좋아요 처리 중 오류가 발생했습니다.');
        setLikedByMe(prevLikedByMe);
        setLikeCount(prevLikeCount);
      }
    } catch (e) {
      alert('좋아요 처리 중 오류가 발생했습니다.');
      setLikedByMe(prevLikedByMe);
      setLikeCount(prevLikeCount);
    }
  };

  if (error) return <div>에러: {error}</div>;
  if (!person) return <div></div>;

  function DirectorCard({ director }) {
    return (
      <div className={styles.actorCard}>
        <div className={styles.actorImg}>
          <img
            src={director.photoUrl ? director.photoUrl : userIcon}
            alt={director.name}
            className={styles.actorPhoto}
          />
        </div>
        <div className={styles.actorName}>{director.name}</div>
        <div className={styles.actorRole}>{director.role}</div>
      </div>
    );
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.profileSection}>
        <DirectorCard director={{
          name: person.name,
          role: '감독',
          photoUrl: person.photoUrl
        }}
        />
        <div className={styles.likeSection}>
          <img
            src={likedByMe ? likeIconTrue : likeIcon}
            alt="like"
            className={styles.likeIcon}
            style={{ cursor: 'pointer' }}
            onClick={handleLikeToggle}
          />
          <span className={styles.likeCount}>
            {likeCount}명
          </span>
        </div>
      </div>
      <hr className={styles.divider} />
      <div className={styles.sliderSection}>
        <h2 className={styles.sectionTitle}>제작</h2>
        <MovieHorizontalSlider
          data={movies.filter(m => m && m.movieCd)}
          sectionKey="actor"
        />
      </div>
    </div>
  );
} 