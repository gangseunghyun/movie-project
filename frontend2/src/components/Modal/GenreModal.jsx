import React from 'react';
import styles from './GenreModal.module.css';

const genres = [
  '판타지',
  '공포',
  '다큐멘터리',
  '가족',
  '전쟁',
  '범죄',
  '모험',
  '역사',
  '애니메이션',
  '스릴러',
  '미스터리',
  '액션',
  '코미디',
  'TV 영화',
  '로맨스',
  'SF',
  '음악',
  '드라마',
  '서부'
];

export default function GenreModal({ isOpen, onClose, selectedGenres, onGenresChange }) {
  const [tempSelectedGenres, setTempSelectedGenres] = React.useState([]);

  // 모달이 열릴 때마다 현재 선택된 장르로 초기화
  React.useEffect(() => {
    if (isOpen) {
      setTempSelectedGenres([...selectedGenres]);
    }
  }, [isOpen, selectedGenres]);

  if (!isOpen) return null;

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const handleGenreChange = (genre) => {
    const newSelectedGenres = tempSelectedGenres.includes(genre)
      ? tempSelectedGenres.filter(g => g !== genre)
      : [...tempSelectedGenres, genre];
    setTempSelectedGenres(newSelectedGenres);
  };

  const handleResetAll = () => {
    setTempSelectedGenres([]);
  };

  const handleConfirm = () => {
    onGenresChange(tempSelectedGenres);
    onClose();
  };

  const selectedCount = tempSelectedGenres.length;

  return (
    <div className={styles.overlay} onClick={handleOverlayClick}>
      <div className={styles.modal}>
        <button className={styles.closeButton} onClick={onClose}>
          ×
        </button>

        <div className={styles.header}>
          <h2 className={styles.title}>장르 전체</h2>
          <p className={styles.subtitle}>
            원치 않는 필터는 체크 박스를 한번 더 누르면 제외 할 수 있어요.
          </p>
          {selectedCount > 0 && (
            <div className={styles.selectedCount}>
              선택된 장르: <span className={styles.count}>{selectedCount}</span>개
            </div>
          )}
        </div>

        <div className={styles.genreGrid}>
          {genres.map((genre) => (
            <span
              key={genre}
              className={`${styles.genreLabel} ${tempSelectedGenres.includes(genre) ? styles.selected : ''}`}
              onClick={() => handleGenreChange(genre)}
            >
              {genre}
            </span>
          ))}
        </div>

        <div className={styles.footer}>
          <button
            className={styles.resetButton}
            onClick={handleResetAll}
            disabled={selectedCount === 0}
          >
            <span>전체 초기화</span>
          </button>
          <button className={styles.confirmButton} onClick={handleConfirm}>
            확인
          </button>
        </div>
      </div>
    </div>
  );
}
