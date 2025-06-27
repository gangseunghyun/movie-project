import React from 'react';

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
  currentUser,
  handleLogout,
  activeMenu,
  setActiveMenu
}) => {
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

  return (
    <div className="mainpage-root">
      {/* 상단 헤더 */}
      <header className="mainpage-header">
        <span className="mainpage-title">영화 데이터 관리 시스템</span>
        <div className="mainpage-user-area">
          {currentUser && (
            <>
              <span>안녕하세요, <b>{currentUser.nickname}</b>님!</span>
              <button className="mainpage-logout-btn" onClick={handleLogout}>로그아웃</button>
            </>
          )}
        </div>
      </header>
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
          {/* 검색창/등록버튼/검색결과 안내는 영화 상세 DTO 탭에서만 표시 */}
          {activeMenu === '영화 상세 DTO' && (
            <>
              <section className="mainpage-search-section">
                <h3 className="mainpage-search-title">🔍 영화 검색</h3>
                <div className="mainpage-search-row">
                  <input
                    type="text"
                    value={searchKeyword}
                    onChange={e => setSearchKeyword(e.target.value)}
                    placeholder="영화 제목을 입력하세요"
                    className="mainpage-search-input"
                  />
                  <button onClick={handleSearch} className="mainpage-search-btn">검색</button>
                  <button onClick={handleClearSearch} className="mainpage-clear-btn">초기화</button>
                  <span className="mainpage-search-result-info">
                    📊 검색 결과: <span style={{ color: '#4baf50', fontWeight: 700 }}>{searchResults?.data?.length || 0}</span>개 영화
                  </span>
                </div>
              </section>
              <div className="mainpage-register-btn-row">
                <button className="mainpage-register-btn">+ 영화 등록</button>
              </div>
            </>
          )}
          {/* 탭별 정보 렌더링 */}
          <section className="mainpage-movie-list-section">
            {renderByMenu[activeMenu]
              ? renderByMenu[activeMenu]()
              : <div style={{marginTop: '80px', textAlign: 'center', color: '#a18cd1', fontSize: '1.3rem'}}>
                  "{activeMenu}" 페이지는 아직 구현되지 않았습니다.
                </div>
            }
          </section>
        </main>
      </div>
    </div>
  );
};

export default MainPage; 