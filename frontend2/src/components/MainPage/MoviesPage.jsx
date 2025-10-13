import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import GenreModal from '../Modal/GenreModal';
import styles from './MoviesPage.module.css';
import previousIcon from '../../assets/previous_icon.png';
import nextIcon from '../../assets/next_icon.png';

const genres = [
  '판타지', '공포', '다큐멘터리', '가족', '전쟁', '범죄', '모험', '역사', '애니메이션', '스릴러',
  '미스터리', '액션', '코미디', 'TV 영화', '로맨스', 'SF', '음악', '드라마', '서부',
];

const sortOptions = [
  { value: 'tmdb_popularity', label: '인기순' },
  { value: 'openDt', label: '신작순' },
  { value: 'name', label: '제목순' },
  { value: 'random', label: '랜덤순' },
];

export default function MoviesPage() {
  const navigate = useNavigate();
  const [showModal, setShowModal] = useState(false);
  const [showSortDropdown, setShowSortDropdown] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false); // 사이드바 토글 상태 추가
  const [isClosing, setIsClosing] = useState(false); // 닫기 애니메이션 상태 추가
  // 필터/무한스크롤용 독립 상태
  const [movies, setMovies] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedGenres, setSelectedGenres] = useState([]);
  const [selectedSort, setSelectedSort] = useState('tmdb_popularity');
  const isFirstLoad = useRef(true);
  // movieListRef 관련 코드 제거

  // 완전히 독립적인 fetchMovies
  const fetchFilteredMovies = async (targetPage = 0, genresArr = selectedGenres, sortType = selectedSort) => {
    if (loading) return;
    setLoading(true);
    let url = `/api/movies/filter?page=${targetPage}&size=14&sort=${sortType}`;
    if (genresArr.length > 0) {
      url += `&genres=${encodeURIComponent(genresArr.join(','))}`;
    }
    try {
      const res = await fetch(url);
      const data = await res.json();
      const newMovies = data.data || [];
      setMovies(prev => targetPage === 0 ? newMovies : [...prev, ...newMovies]);
      // hasMore 계산을 page, totalPages 기반으로 변경
      setHasMore((data.page + 1) < (data.totalPages || 1));
      setLoading(false);
    } catch (err) {
      setError('영화 목록을 불러오지 못했습니다.');
      setLoading(false);
    }
  };

  // 장르 변경 시 즉시 API 호출
  useEffect(() => {
    setMovies([]);
    setPage(0);
    setHasMore(true);
    setError(null);
    isFirstLoad.current = true;
    fetchFilteredMovies(0, selectedGenres, selectedSort);
  }, [selectedGenres, selectedSort]);

  // page 변경 시 추가 로드 (0은 장르변경에서 이미 처리)
  useEffect(() => {
    if (page === 0 && isFirstLoad.current) {
      isFirstLoad.current = false;
      return;
    }
    if (page > 0) {
      fetchFilteredMovies(page, selectedGenres, selectedSort);
    }
  }, [page]);

  // 무한스크롤 이벤트 (window에 붙임)
  useEffect(() => {
    const handleScroll = () => {
      if (
        window.innerHeight + document.documentElement.scrollTop >=
        document.documentElement.offsetHeight - 100
      ) {
        if (!loading && hasMore) setPage(prev => prev + 1);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [loading, hasMore]);

  // 장르 체크박스 핸들러
  const handleGenreChange = (genre) => {
    setSelectedGenres(prev =>
      prev.includes(genre) ? prev.filter(g => g !== genre) : [...prev, genre]
    );
  };

  // 전체 초기화 핸들러
  const handleResetAll = () => {
    setSelectedGenres([]);
  };

  // 정렬 변경 핸들러
  const handleSortChange = (sortValue) => {
    setSelectedSort(sortValue);
    setShowSortDropdown(false);
  };

  // 현재 선택된 정렬 옵션의 라벨 가져오기
  const getCurrentSortLabel = () => {
    return sortOptions.find(option => option.value === selectedSort)?.label || '인기순';
  };

  // 영화 카드 클릭 핸들러
  const handleMovieClick = (movie) => {
    if (movie.movieCd) {
      navigate(`/movie-detail/${movie.movieCd}`);
    }
  };

  // 사이드바 닫기 핸들러
  const handleCloseSidebar = () => {
    setIsClosing(true);
    setTimeout(() => {
      setSidebarOpen(false);
      setIsClosing(false);
    }, 300); // 애니메이션 시간과 동일
  };

  // 사이드바 열기 핸들러
  const handleOpenSidebar = () => {
    setSidebarOpen(true);
  };

  return (
          <div className={styles.container}>
        {/* 사이드바 - 화면 위에 띄우기 */}
        {sidebarOpen && (
          <aside className={`${styles.sidebar} ${isClosing ? styles.sidebarClosing : ''}`}>
            {/* 사이드바 토글 버튼 */}
            <button
              className={styles.sidebarToggle}
              onClick={handleCloseSidebar}
              title="사이드바 닫기"
            >
              <img
                src={previousIcon}
                alt="사이드바 닫기"
                className={styles.toggleIcon}
              />
            </button>


          {/* 필터 섹션 */}
          <div className={styles.filterSection}>
            <div className={styles.sectionTitle}>
              필터
              <button
                className={styles.resetButton}
                onClick={handleResetAll}
                disabled={selectedGenres.length === 0}
              >
                <span className={styles.resetIcon}>↻</span>
                전체 초기화
              </button>
            </div>
          </div>
          <hr className={styles.divider} />

          {/* 장르 섹션 */}
          <div className={styles.genreSection}>
            <div className={styles.sectionTitle}>
              장르 <span className={styles.moreBtn} onClick={() => setShowModal(true)}>더보기</span>
            </div>
            <div className={styles.genreGrid}>
              {genres.slice(0, 10).map((genre) => (
                <span
                  key={genre}
                  className={`${styles.genreLabel} ${selectedGenres.includes(genre) ? styles.selected : ''}`}
                  onClick={() => handleGenreChange(genre)}
                >
                  {genre}
                </span>
              ))}
            </div>
          </div>
        </aside>
      )}

      {/* 오른쪽 영화 리스트 */}
      <main className={styles.movieList}>
        {/* 사이드바 열기 버튼 - 화면 왼쪽 중앙에 고정 */}
        <button
          className={styles.sidebarOpenButton}
          onClick={handleOpenSidebar}
          title="사이드바 열기"
        >
          <img
            src={nextIcon}
            alt="사이드바 열기"
            className={styles.toggleIcon}
          />
        </button>

        <div className={styles.headerSection}>
          <h2 className={styles.pageTitle}>태그검색</h2>
          <div className={styles.sortContainer}>
            <button
              className={styles.sortButton}
              onClick={() => setShowSortDropdown(!showSortDropdown)}
            >
              {getCurrentSortLabel()}
              <span className={styles.sortArrow}>▲</span>
            </button>
            {showSortDropdown && (
              <div className={styles.sortDropdown}>
                {sortOptions.map((option) => (
                  <button
                    key={option.value}
                    className={`${styles.sortOption} ${selectedSort === option.value ? styles.selected : ''}`}
                    onClick={() => handleSortChange(option.value)}
                  >
                    {option.label}
                    {selectedSort === option.value && <span className={styles.checkmark}>✓</span>}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
        {error ? (
          <div style={{ color: 'red', padding: 40 }}>{error}</div>
        ) : (
          <div className={styles.moviesGrid}>
            {movies.map((movie) => (
              <div
                key={movie.movieCd}
                className={styles.card}
                onClick={() => handleMovieClick(movie)}
                style={{ cursor: 'pointer' }}
              >
                <div className={styles.poster}>
                  <img src={movie.posterUrl} alt={movie.movieNm} className={styles.posterImg} />
                </div>
                <div className={styles.title}>{movie.movieNm}</div>
                <div style={{ color: '#bdbdbd', fontSize: '0.95em', marginTop: 2 }}>{movie.genreNm}</div>
              </div>
            ))}
          </div>
        )}
        {loading && <div style={{ color: 'var(--color-text)', padding: 40 }}>로딩 중...</div>}
        {!hasMore && !loading && movies.length === 0 && (
          <div style={{ color: 'var(--color-text)', padding: 40 }}>영화가 없습니다.</div>
        )}
      </main>

      {/* 모달을 container 밖으로 이동 */}
      <GenreModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        selectedGenres={selectedGenres}
        onGenresChange={setSelectedGenres}
      />
    </div>
  );
}