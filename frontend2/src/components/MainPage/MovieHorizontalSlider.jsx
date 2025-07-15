import React from 'react';
import styles from './MovieHorizontalSlider.module.css';
import MovieCard from './MovieCard';
import previousIcon from '../../assets/previous_icon.png';
import nextIcon from '../../assets/next_icon.png';

const dummyMovies = Array.from({ length: 20 }, (_, i) => ({
  id: i,
  title: `영화 제목`,
  year: 2025,
  country: '미국',
  rating: 5.0,
  audience: '24만',
}));

export default function MovieHorizontalSlider({ data, sectionKey, ratings, actorInfo, CardComponent }) {
  function remToPx(rem) {
    return rem * parseFloat(getComputedStyle(document.documentElement).fontSize);
  }

  const getNavBtnTop = () => {
    switch (sectionKey) {
      case 'person':
        return '200px';    // 출연/제작
      case 'comment':
        return '125px';    // 코멘트
      case 'similar':
      case 'stillcut':
        return '180px';    // 비슷한 작품, 스틸컷
      default:
        return '50%';      // 기본값
    }
  };

  const [start, setStart] = React.useState(0);
  const rootStyle = getComputedStyle(document.documentElement);
  const cardWidth = parseFloat(rootStyle.getPropertyValue('--card-width')) || 240;
  const cardGapRem = parseFloat(rootStyle.getPropertyValue('--card-gap')) || 0.9;
  const cardGap = remToPx(cardGapRem);
  const visible = 7;

  // data 사용, 아니면 더미데이터 사용
  const movies = data && data.length > 0 ? data : dummyMovies;

  const prev = () => setStart(Math.max(0, start - visible));
  const next = () => setStart(Math.min(movies.length - visible, start + visible));

  // 카드 컴포넌트 결정
  const Card = CardComponent || MovieCard;

  return (
    <div className={styles.sliderWrapper}>
      {start > 0 && (
        <button
          className={`${styles.navBtn} ${styles.left}`}
          style={{ top: getNavBtnTop() }}
          onClick={prev}
        >
          <img src={previousIcon} alt="이전" />
        </button>
      )}
      <div
        className={`${styles.slider} ${start === 0 ? styles.first : ''}`}
        style={{
          transform: `translateX(-${start * (cardWidth + cardGap) - (start === 0 ? 0 : 70)}px)`,
          transition: 'transform 0.4s'
        }}
      >
        {movies.map((movie, idx) => (
          <Card
          key={`${movie.movieCd || movie.id || 'movie'}-${idx}`}
            {...(sectionKey === 'person' ? { person: movie } :
              sectionKey === 'comment' ? { comment: movie } :
                sectionKey === 'similar' ? { movie } :
                  { movie })}
            index={idx + 1}
            sectionKey={sectionKey}
            actorInfo={sectionKey === 'actor' ? actorInfo : undefined}
            ratings={ratings}
          />
        ))}
      </div>
      {start + visible < movies.length && (
        <button
          className={`${styles.navBtn} ${styles.right}`}
          onClick={next}
        >
          <img src={nextIcon} alt="다음" />
        </button>
      )}
    </div>
  );
} 