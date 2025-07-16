import React from 'react';
import styles from './MovieCard.module.css';
import { useNavigate } from 'react-router-dom';

export default function MovieCard({ movie, index, sectionKey, actorInfo, showOpenDt = true }) {
  const navigate = useNavigate();
  
  // 포스터가 없는 영화는 렌더링하지 않음
  if (!movie.posterUrl || movie.posterUrl.trim() === '' || movie.posterUrl === 'null') {
    return null;
  }
  
  function getDDay(openDt) {
    if (!openDt) return null;
    const today = new Date();
    const openDate = new Date(openDt);
    // 시차 보정 (UTC로 들어올 경우)
    openDate.setHours(0, 0, 0, 0);
    today.setHours(0, 0, 0, 0);
    const diff = Math.ceil((openDate - today) / (1000 * 60 * 60 * 24));
    if (diff < 0) return null; // 이미 개봉
    return `D-${diff}`;
  }

  // 카드 클릭 핸들러
  const handleCardClick = () => {
    if (movie.movieCd) {
      // 모든 섹션에서 영화 데이터를 state로 전달
      navigate(`/movie-detail/${movie.movieCd}`, { 
        state: { 
          movieData: movie,
          sectionKey: sectionKey
        } 
      });
    }
  };

  // 인기영화 섹션이면 제목과 별점만 표시
  if (sectionKey === 'popular') {
    return (
      <div className={styles.card} onClick={handleCardClick} style={{ cursor: movie.movieCd ? 'pointer' : 'default' }}>
        <div className={styles.poster} style={{ position: 'relative' }}>
          <span className={styles.posterRank}>{index}</span>
          {movie.posterUrl && (
            <img
              src={movie.posterUrl}
              alt={movie.movieNm || '-'}
              className={styles.posterImg}
            />
          )}
        </div>
        <div className={styles.title}>{movie.movieNm || '-'}</div>
        <div className={styles.ratingLine}>
          <span className={styles.star}>⭐</span>
          <span className={styles.rating}>
            {movie.averageRating ?? 0}
          </span>
        </div>
      </div>
    );
  }
  // 개봉예정영화 섹션
  if (sectionKey === 'upcoming') {
    return (
      <div className={styles.card} style={{ position: 'relative', cursor: movie.movieCd ? 'pointer' : 'default' }} onClick={handleCardClick}>
        {/* D-day 뱃지 */}
        {movie.openDt && getDDay(movie.openDt) && (
          <div className={styles.dDayBadge}>{getDDay(movie.openDt)}</div>
        )}
        <div className={styles.poster} style={{ position: 'relative' }}>
          {/* <span className={styles.posterRank}>{index}</span> 순위는 개봉예정작에서 숨김 */}
          {movie.posterUrl && (
            <img
              src={movie.posterUrl}
              alt={movie.movieNm || '-'}
              className={styles.posterImg}
            />
          )}
        </div>
        <div className={styles.title}>{movie.movieNm || '-'}</div>
        <div className={styles.info}>
          <span className={styles.comingSoonDate}>
            {showOpenDt ? (movie.year || movie.openDt || '-') : null}
          </span>
        </div>
      </div>
    );
  }
  // 배우 출연 영화 섹션이면 배우 정보와 대표작 표시
  if (sectionKey === 'actor' || sectionKey === 'director' || sectionKey === 'toprated' || 
    sectionKey.startsWith('like-') || sectionKey === 'social-recommend' || sectionKey.startsWith('new-genre-')) {
    return (
      <div className={styles.card} onClick={handleCardClick} style={{ cursor: movie.movieCd ? 'pointer' : 'default' }}>
        {/* 배우 정보 표시 */}
        
        <div className={styles.poster} style={{ position: 'relative' }}>
          {movie.posterUrl && (
            <img
              src={movie.posterUrl}
              alt={movie.movieNm || '-'}
              className={styles.posterImg}
            />
          )}
        </div>
        <div className={styles.title}>{movie.movieNm || movie.title || '-'}</div>
        <div className={styles.info}>
          <span>
            {showOpenDt ? (movie.year || movie.openDt || '-') : null}
            {movie.showTm ? ` · ${movie.showTm}분` : ''}
          </span>
        </div>
        <div className={styles.ratingLine}>
          <span className={styles.star}>⭐</span>
          <span className={styles.rating}>
            {movie.averageRating ?? 0}
          </span>
        </div>
      </div>
    );
  }

  if (sectionKey === 'boxoffice') {
    return (
      <div className={styles.card} onClick={handleCardClick} style={{ cursor: movie.movieCd ? 'pointer' : 'default' }}>
      <div className={styles.poster} style={{ position: 'relative' }}>
      <span className={styles.posterRank}>{index}</span>
        {movie.posterUrl && (
          <img
            src={movie.posterUrl}
            alt={movie.movieNm || '-'}
            className={styles.posterImg}
          />
        )}
      </div>
      <div className={styles.title}>{movie.movieNm || '-'}</div>
      <div className={styles.info}>
        <span>
          {showOpenDt ? (movie.year || movie.openDt || '-') : null}
          {movie.showTm ? ` · ${movie.showTm}분` : ''}
        </span>
      </div>
      <div className={styles.ratingLine}>
        <span className={styles.star}>⭐</span>
        <span className={styles.rating}>
          {movie.averageRating ?? 0}
        </span>
      </div>
      <div className={styles.audience}>누적관객 {movie.audience || movie.formattedAudiAcc || '-'}</div>
    </div>
    );
  }

  if (sectionKey === 'personalized') {
    return (
      <div className={styles.card} onClick={handleCardClick} style={{ cursor: movie.movieCd ? 'pointer' : 'default' }}>
      <div className={styles.poster} style={{ position: 'relative' }}>

        {movie.posterUrl && (
          <img
            src={movie.posterUrl}
            alt={movie.movieNm || '-'}
            className={styles.posterImg}
          />
        )}
      </div>
      <div className={styles.title}>{movie.movieNm || '-'}</div>
      
      <div className={styles.ratingLine}>
        <span className={styles.star}>⭐</span>
        <span className={styles.rating}>
          {movie.averageRating ?? 0}
        </span>
      </div>
      
    </div>
    );
  }
  
  // 그 외 섹션은 기존 정보 모두 표시
  return (
    <div className={styles.card} onClick={handleCardClick} style={{ cursor: movie.movieCd ? 'pointer' : 'default' }}>
      <div className={styles.poster} style={{ position: 'relative' }}>
   
        {movie.posterUrl && (
          <img
            src={movie.posterUrl}
            alt={movie.movieNm || '-'}
            className={styles.posterImg}
          />
        )}
      </div>
      <div className={styles.title}>{movie.movieNm || '-'}</div>
      <div className={styles.info}>
        <span>
          {showOpenDt ? (movie.year || movie.openDt || '-') : null}
        </span>
      </div>
      
    </div>
  );
}