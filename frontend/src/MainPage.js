import React, { useState, useEffect } from 'react';

const menuList = [
  { icon: '📊', label: '통계' },
  { icon: '🎬', label: '영화 목록' },
  { icon: '📝', label: '영화 상세' },
  { icon: '🔥', label: '박스오피스' },
  { icon: '🗂️', label: '박스오피스 DTO' },
  { icon: '🏷️', label: '영화 상세 DTO' },
  { icon: '📄', label: '영화 목록 DTO' },
  { icon: '⭐', label: '평점 높은 영화' },
  { icon: '🔥', label: '인기 영화' },
  { icon: '🎬', label: '개봉예정작' },
  { icon: '🎬', label: '개봉중' },
  { icon: '🎬', label: '상영종료' },
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
  renderPopularMovies,
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
  handlePopularKeywordClick
}) => {
  // 검색창 포커스 상태 추가
  const [searchFocus, setSearchFocus] = useState(false);
  const [localRecentKeywords, setLocalRecentKeywords] = useState(recentKeywords || []);

  // recentKeywords prop이 바뀌면 동기화
  useEffect(() => {
    setLocalRecentKeywords(recentKeywords || []);
  }, [recentKeywords]);

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

  // 탭별 렌더링 함수 매핑
  const renderByMenu = {
    '통계': renderStats,
    '영화 목록': renderMovieList,
    '영화 상세': renderMovieDetail,
    '박스오피스': renderBoxOffice,
    '박스오피스 DTO': renderBoxOfficeDto,
    '영화 상세 DTO': renderMovieDetailDto,
    '영화 목록 DTO': renderMovieListDto,
    '평점 높은 영화': renderTopRated,
    '인기 영화': renderPopularMovies,
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
        <span className="mainpage-title">영화 데이터 관리 시스템</span>
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
          {/* 최근 검색어 드롭다운 */}
          {currentUser && localRecentKeywords && localRecentKeywords.length > 0 && searchFocus && (
            <ul className="recent-keywords-dropdown">
              {localRecentKeywords.filter(keyword => keyword && keyword.trim().length > 1).map((keyword, idx) => (
                <li
                  key={keyword}
                  className="recent-keyword-item"
                  onClick={() => handleRecentKeywordClick(keyword)}
                >
                  {keyword}
                  <span
                    className="recent-keyword-delete"
                    tabIndex={-1}
                    onClick={e => {
                      e.stopPropagation(); // 부모 클릭 이벤트 방지
                      handleDeleteKeyword(keyword, e);
                    }}
                    title="삭제"
                  >×</span>
                </li>
              ))}
            </ul>
          )}
          
          {/* 인기 검색어 드롭다운 */}
          {popularKeywords && popularKeywords.length > 0 && searchFocus && (
            <ul className="popular-keywords-dropdown">
              <li className="popular-keywords-header">
                📈 인기 검색어 (최근 7일)
              </li>
              {popularKeywords.map((item, idx) => (
                <li
                  key={item.keyword}
                  className="popular-keyword-item"
                  onClick={() => handlePopularKeywordClick(item.keyword)}
                >
                  <span className="popular-keyword-rank">{idx + 1}</span>
                  <span className="popular-keyword-text">{item.keyword}</span>
                  <span className="popular-keyword-count">({item.searchCount}회)</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </header>
      {/* 로그인/로그아웃/환영 메시지 (검색창 아래) */}
      <div className="mainpage-user-area" style={{ textAlign: 'right', margin: '16px 32px 0 0' }}>
        {currentUser ? (
          <>
            <span>안녕하세요, <b>{currentUser.nickname}</b>님!</span>
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
              <li
                key={menu.label}
                className={activeMenu === menu.label ? 'active' : ''}
                onClick={() => setActiveMenu(menu.label)}
                style={{ userSelect: 'none' }}
              >
                <span className="mainpage-menu-icon">{menu.icon}</span>
                <span className="mainpage-menu-label">{menu.label}</span>
              </li>
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
                  ? renderByMenu[activeMenu]()
                  : <div style={{marginTop: '80px', textAlign: 'center', color: '#a18cd1', fontSize: '1.3rem'}}>
                      "{activeMenu}" 페이지는 아직 구현되지 않았습니다.
                    </div>
                }
              </section>
            </>
          )}
        </main>
      </div>
      
      {/* 영화 상세보기 모달 */}
      {renderMovieDetailModal && renderMovieDetailModal()}
      
      {/* 영화 등록/수정 폼 모달 */}
      {renderMovieForm && renderMovieForm()}
    </div>
  );
};

export default MainPage; 