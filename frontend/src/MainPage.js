import React, { useState, useEffect } from 'react';
import RatingDistributionChart from './components/RatingDistributionChart';
import { useNavigate } from 'react-router-dom';
import ReasonBadges from './components/ReasonBadges';
import axios from 'axios';
import ReviewPreview from './components/ReviewPreview';
import ReviewList from './components/ReviewList';

const menuList = [
  { icon: '🏠', label: '메인 페이지' },
  { icon: '🎬', label: '영화 목록' },
  { icon: '📝', label: '영화 상세' },
  { icon: '🔥', label: '박스오피스' },
  { icon: '🗂️', label: '박스오피스 DTO' },
  { icon: '🏷️', label: '영화 상세 DTO' },
  { icon: '📄', label: '영화 목록 DTO' },
  { icon: '⭐', label: '평점 높은 영화' },
  { icon: '🎬', label: '개봉예정작' },
  { icon: '🎬', label: '개봉중' },
  { icon: '🎬', label: '상영종료' },
  { icon: '🎯', label: '태그추천영화', requireLogin: true },
];

const MainPage = ({
  searchKeyword,
  setSearchKeyword,
  handleSearch,
  handleClearSearch,
  searchResults,
  loading,
  renderStats,
  renderMovieList,
  renderMovieDetail,
  renderBoxOffice,
  renderBoxOfficeDto,
  renderMovieDetailDto,
  renderMovieListDto,
  renderTopRated,
  renderRecommendedMovies,
  renderComingSoon,
  renderNowPlaying,
  renderEnded,
  renderSearchResults,
  searchExecuted,
  currentUser,
  handleLogout,
  activeMenu,
  setActiveMenu,
  handleMovieClick,
  handleEditMovie,
  handleAddMovie,
  handleDeleteMovie,
  handleSaveMovie,
  handleLikeMovie,
  selectedMovie,
  showMovieDetail,
  setShowMovieDetail,
  showMovieForm,
  setShowMovieForm,
  editingMovie,
  movieForm,
  setMovieForm,
  renderMovieDetailModal,
  renderMovieForm,
  recentKeywords,
  handleRecentKeywordClick,
  handleDeleteRecentKeyword,
  popularKeywords,
  handlePopularKeywordClick,
  newGenreRecommendation,
  fetchNewGenreRecommendation
}) => {
  // 검색창 포커스 상태 추가
  const [searchFocus, setSearchFocus] = useState(false);
  const [localRecentKeywords, setLocalRecentKeywords] = useState(recentKeywords || []);
  const navigate = useNavigate();
  const [showRecommendation, setShowRecommendation] = useState(false);
  const [personalizedRecommendations, setPersonalizedRecommendations] = useState([]);
  const [showAllReviews, setShowAllReviews] = useState(false);
  const [reviews, setReviews] = useState([]);

  // recentKeywords prop이 바뀌면 동기화
  useEffect(() => {
    setLocalRecentKeywords(recentKeywords || []);
  }, [recentKeywords]);

  useEffect(() => {
    if (activeMenu === '영화 상세' && selectedMovie) {
      fetch(`/api/reviews/dto/list?movieId=${selectedMovie.movieDetailId}`)
        .then(res => res.json())
        .then(data => setReviews(data.content || []));
    }
  }, [activeMenu, selectedMovie]);

  const handleToggleRecommendation = async () => {
    if (!showRecommendation) {
      if (!currentUser || !currentUser.id) return;
      try {
        const res = await axios.get('http://localhost:80/api/recommendations/personalized/liked-people?page=0&size=10', { withCredentials: true });
        setPersonalizedRecommendations(res.data);
      } catch (e) {
        setPersonalizedRecommendations([]);
      }
    }
    setShowRecommendation(!showRecommendation);
  };

  // 최근 검색어 삭제 핸들러
  const handleDeleteKeyword = async (keyword, e) => {
    e.preventDefault();
    try {
      // 부모 컴포넌트의 삭제 핸들러 사용
      await handleDeleteRecentKeyword(keyword);
      // 로컬 상태도 즉시 업데이트
      setLocalRecentKeywords(prev => prev.filter(k => k !== keyword));
    } catch (err) {
      console.error('삭제 실패:', err);
      alert('삭제에 실패했습니다.');
    }
  };

  const handleShowAllReviews = () => setShowAllReviews(true);

  // 탭별 렌더링 함수 매핑
  const renderByMenu = {
    '메인 페이지': renderStats,
    '영화 목록': renderMovieList,
    '영화 상세': renderMovieDetail,
    '박스오피스': renderBoxOffice,
    '박스오피스 DTO': renderBoxOfficeDto,
    '영화 상세 DTO': renderMovieDetailDto,
    '영화 목록 DTO': renderMovieListDto,
    '평점 높은 영화': renderTopRated,
    '태그추천영화': renderRecommendedMovies,
    '개봉예정작': renderComingSoon,
    '개봉중': renderNowPlaying,
    '상영종료': renderEnded,
  };

  // 로그인 버튼 클릭 시 /login으로 이동
  const handleLoginClick = () => {
    window.location.href = '/login';
  };

  return (
    <div className="mainpage-root">
      {/* 상단 헤더 */}
      <header className="mainpage-header">
        <span className="mainpage-title">Filmer</span>
        <div style={{ flex: 1 }} />
        {/* 통합 검색창 */}
        <div className="mainpage-global-search">
          <input
            type="text"
            value={searchKeyword}
            onChange={e => setSearchKeyword(e.target.value)}
            onFocus={() => setSearchFocus(true)}
            onBlur={e => {
              // 드롭다운 내부 클릭 시에는 닫히지 않게
              if (
                e.relatedTarget &&
                (e.relatedTarget.classList?.contains('recent-keyword-delete') ||
                 e.relatedTarget.classList?.contains('recent-keyword-item'))
              ) {
                return;
              }
              setTimeout(() => setSearchFocus(false), 200);
            }}
            onKeyDown={e => {
              if (e.key === 'Enter') {
                handleSearch();
              } else if (e.key === 'Escape') {
                setSearchFocus(false);
              }
            }}
            placeholder="영화/유저 검색..."
            className="mainpage-search-input"
            style={{
              paddingRight: 40
            }}
          />
          <button
            className="mainpage-search-icon-btn"
            onClick={handleSearch}
            tabIndex={-1}
            type="button"
          >
            <span role="img" aria-label="검색">🔍</span>
          </button>
          {/* 2열 드롭다운: 최근 검색어(왼쪽) + 인기 검색어(오른쪽) */}
          {searchFocus && (
            <div className="search-dropdown-container">
              {/* 왼쪽: 최근 검색어 (로그인한 경우에만 표시) */}
              {currentUser && (
                <div className="search-dropdown-section">
                  <div className="search-dropdown-title">최근 검색어</div>
                  <ul className="search-dropdown-list">
                    {(!localRecentKeywords || localRecentKeywords.length === 0) && (
                      <li className="search-dropdown-item" style={{color:'#aaa'}}>기록 없음</li>
                    )}
                    {localRecentKeywords && localRecentKeywords.length > 0 &&
                      localRecentKeywords.filter(keyword => keyword && keyword.trim().length > 0).map((keyword, idx) => (
                        <li
                          key={keyword}
                          className="search-dropdown-item"
                          onClick={() => handleRecentKeywordClick(keyword)}
                        >
                          {keyword}
                          <span
                            className="recent-keyword-delete"
                            tabIndex={-1}
                            onClick={e => {
                              e.stopPropagation();
                              handleDeleteKeyword(keyword, e);
                            }}
                            title="삭제"
                            style={{marginLeft:8, color:'#e57373', cursor:'pointer'}}
                          >×</span>
                        </li>
                      ))
                    }
                  </ul>
                </div>
              )}
              {/* 오른쪽: 인기 검색어 (항상 표시) */}
              <div className={`search-dropdown-section ${!currentUser ? 'single-section' : ''}`}>
                <div className="search-dropdown-title">인기 검색어</div>
                <ul className="search-dropdown-list">
                  {(!popularKeywords || popularKeywords.length === 0) && (
                    <li className="search-dropdown-item" style={{color:'#aaa'}}>데이터 없음</li>
                  )}
                  {popularKeywords && popularKeywords.length > 0 &&
                    popularKeywords.map((item, idx) => (
                      <li
                        key={item.keyword || idx}
                        className="search-dropdown-item"
                        onClick={() => handlePopularKeywordClick(item.keyword)}
                      >
                        <span style={{color:'#a18cd1', fontWeight:700, marginRight:6}}>{idx+1}.</span>
                        {item.keyword}
                        <span style={{color:'#bbb', fontSize:'0.95em', marginLeft:6}}>{item.searchCount ? `(${item.searchCount}회)` : ''}</span>
                      </li>
                    ))
                  }
                </ul>
              </div>
            </div>
          )}
        </div>
      </header>
      {/* 로그인/로그아웃/환영 메시지 (검색창 아래) */}
      <div className="mainpage-user-area" style={{ textAlign: 'right', margin: '16px 32px 0 0' }}>
        {currentUser ? (
          <>
            <span>안녕하세요, <b>{currentUser.nickname}</b>님!</span>
            <button className="mainpage-mypage-btn" style={{ marginLeft: 8 }} onClick={() => navigate(`/user/${currentUser.nickname}`)}>마이페이지</button>
            <button className="mainpage-logout-btn" onClick={handleLogout} style={{ marginLeft: 8 }}>로그아웃</button>
          </>
        ) : (
          <button className="mainpage-login-btn" onClick={handleLoginClick}>로그인</button>
        )}
      </div>
      <div className="mainpage-body">
        {/* 좌측 넓은 사이드바 */}
        <aside className="mainpage-sidebar">
          <ul>
            {menuList.map((menu) => (
              // 로그인이 필요한 메뉴는 로그인한 사용자만 표시
              (!menu.requireLogin || currentUser) && (
                <li
                  key={menu.label}
                  className={activeMenu === menu.label ? 'active' : ''}
                  onClick={() => setActiveMenu(menu.label)}
                  style={{ userSelect: 'none' }}
                >
                  <span className="mainpage-menu-icon">{menu.icon}</span>
                  <span className="mainpage-menu-label">{menu.label}</span>
                </li>
              )
            ))}
          </ul>
        </aside>
        {/* 메인 콘텐츠 */}
        <main className="mainpage-content">
          {searchExecuted && searchKeyword.trim() ? (
            renderSearchResults && renderSearchResults()
          ) : (
            <>
              {activeMenu === '영화 상세 DTO' && currentUser && currentUser.isAdmin && (
                <div className="mainpage-register-btn-row">
                  <button className="mainpage-register-btn" onClick={handleAddMovie}>+ 영화 등록</button>
                </div>
              )}
              <section className="mainpage-movie-list-section">
                {renderByMenu[activeMenu]
                  ? (activeMenu === '영화 상세 DTO'
                      ? renderByMenu[activeMenu]({
                          currentUser,
                          handleEditMovie,
                          handleDeleteMovie,
                          handleLikeMovie
                        })
                      : renderByMenu[activeMenu]())
                  : <div style={{marginTop: '80px', textAlign: 'center', color: '#a18cd1', fontSize: '1.3rem'}}>
                      "{activeMenu}" 페이지는 아직 구현되지 않았습니다.
                    </div>
                }
              </section>
              {activeMenu === '영화 상세' && selectedMovie && (
                !showAllReviews ? (
                  <ReviewPreview reviews={reviews} onShowAll={handleShowAllReviews} />
                ) : (
                  <ReviewList movieCd={selectedMovie.movieCd} currentUser={currentUser} />
                )
              )}
            </>
          )}
        </main>
      </div>
      
      {/* 영화 상세보기 모달 */}
      {renderMovieDetailModal && renderMovieDetailModal()}
      
      {/* 영화 등록/수정 폼 모달 */}
      {renderMovieForm && renderMovieForm()}

      {/* 추천 결과 토글 버튼 및 조건부 렌더링 */}
      <div style={{ margin: '32px auto', maxWidth: 900 }}>
        <button
          onClick={handleToggleRecommendation}
          style={{
            marginBottom: 20,
            padding: '10px 24px',
            fontSize: '1.1em',
            borderRadius: 8,
            background: '#a9a6f7',
            color: '#fff',
            border: 'none',
            cursor: 'pointer'
          }}
        >
          {showRecommendation ? '추천 숨기기' : '추천 결과 보기'}
        </button>
        {showRecommendation && (
          <div>
            <h2 style={{ margin: '16px 0 8px 0', color: '#333' }}>맞춤형 추천 결과</h2>
            {personalizedRecommendations.length === 0 ? (
              <div style={{ color: '#aaa', fontSize: '1.1em' }}>추천 결과가 없습니다.</div>
            ) : (
              <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', marginTop: 16 }}>
                {personalizedRecommendations.map(item => (
                  <div
                    key={item.movieId}
                    style={{
                      width: 180,
                      background: '#fff',
                      borderRadius: 12,
                      boxShadow: '0 2px 8px #eee',
                      padding: 12,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      cursor: 'pointer'
                    }}
                    onClick={() => handleMovieClick({ ...item, movieCd: item.movieCd })}
                  >
                    {item.posterUrl ? (
                      <img
                        src={item.posterUrl}
                        alt={item.movieNm}
                        style={{ width: '100%', height: 240, objectFit: 'cover', borderRadius: 8, marginBottom: 8 }}
                      />
                    ) : (
                      <div style={{
                        width: '100%', height: 240, background: '#eee', borderRadius: 8, marginBottom: 8,
                        display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#aaa'
                      }}>
                        No Image
                      </div>
                    )}
                    <div style={{ fontWeight: 'bold', fontSize: 16, marginBottom: 4, textAlign: 'center' }}>{item.movieNm}</div>
                    <div style={{ fontSize: 13, color: '#888', marginBottom: 4 }}>{item.genres && item.genres.join(', ')}</div>
                    <div style={{ fontSize: 13, color: '#888', marginBottom: 4 }}>
                      {item.averageRating !== undefined ? `${item.averageRating.toFixed(1)}⭐` : '0.0⭐'}
                    </div>
                    <div style={{ marginBottom: 8 }}>
                      {item.reasons && item.reasons.slice(0, 3).map((reason, idx) => (
                        <span
                          key={idx}
                          style={{
                            background: '#a9a6f7',
                            color: '#fff',
                            borderRadius: '12px',
                            padding: '2px 10px',
                            marginRight: '4px',
                            fontSize: '0.9em'
                          }}
                        >
                          {reason}
                        </span>
                      ))}
                      {item.reasons && item.reasons.length > 3 && (
                        <span style={{
                          background: '#a9a6f7',
                          color: '#fff',
                          borderRadius: '12px',
                          padding: '2px 10px',
                          fontSize: '0.9em'
                        }}>
                          +{item.reasons.length - 3}
                        </span>
                      )}
                    </div>
                    <div style={{ fontSize: 13, color: '#a9a6f7', fontWeight: 700 }}>추천 점수: {item.score}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default MainPage; 