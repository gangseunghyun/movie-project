import React, { useState, useEffect } from 'react';
import axios from 'axios';
import Login from './Login';
import Signup from './Signup';
import SocialJoin from './SocialJoin';
import './App.css';
import { safeFetch } from './api';

// axios 기본 설정 - baseURL 제거하고 절대 경로 사용
axios.defaults.withCredentials = true;

function App() {
  const [activeTab, setActiveTab] = useState('stats');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState(null);
  const [movieListData, setMovieListData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [movieDetailData, setMovieDetailData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [boxOfficeData, setBoxOfficeData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [boxOfficeDtoData, setBoxOfficeDtoData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [movieDetailDtoData, setMovieDetailDtoData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [movieListDtoData, setMovieListDtoData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [topRatedData, setTopRatedData] = useState([]);
  const [popularMoviesData, setPopularMoviesData] = useState([]);
  const [comingSoonData, setComingSoonData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [nowPlayingData, setNowPlayingData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [endedData, setEndedData] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  
  // 영화 검색 상태
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState({ data: [], total: 0, page: 0, totalPages: 0 });
  const [isSearching, setIsSearching] = useState(false);
  
  // 로그인/회원가입 상태
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [showLogin, setShowLogin] = useState(true);
  const [showAuth, setShowAuth] = useState(true);
  const [showSocialJoin, setShowSocialJoin] = useState(false);
  
  // 영화 관리 시스템 상태
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [showMovieDetail, setShowMovieDetail] = useState(false);
  const [showMovieForm, setShowMovieForm] = useState(false);
  const [editingMovie, setEditingMovie] = useState(null);
  const [movieForm, setMovieForm] = useState({
    movieNm: '',
    movieNmEn: '',
    description: '',
    directorName: '',
    actors: '',
    tags: '',
    companyNm: '',
    openDt: '',
    showTm: '',
    genreNm: '',
    nationNm: '',
    watchGradeNm: '',
    prdtYear: '',
    prdtStatNm: '',
    typeNm: '',
    directors: [],
    totalAudience: 0,
    reservationRate: 0,
    averageRating: 0
  });

  const [searchExecuted, setSearchExecuted] = useState(false);

  // API 기본 URL
  const API_BASE_URL = 'http://localhost:80/api';

  // 로그인 상태 확인
  useEffect(() => {
    checkLoginStatus();
    
    // URL 경로 확인하여 social-join 페이지 표시
    if (window.location.pathname === '/social-join') {
      setShowSocialJoin(true);
      setShowAuth(false);
    }
  }, []);

  const checkLoginStatus = async () => {
    try {
      console.log("=== 로그인 상태 확인 시작 ===");
      const response = await safeFetch('http://localhost:80/api/current-user');
      console.log("API 응답:", response);
      
      if (response.success) {
        console.log("로그인 성공 - 사용자 정보:", response.user);
        setIsLoggedIn(true);
        setCurrentUser(response.user);
        setShowAuth(false);
        console.log("설정된 currentUser:", response.user);
        console.log("isAdmin 값:", response.user.isAdmin);
        console.log("role 값:", response.user.role);
      } else {
        console.log("로그인되지 않은 상태:", response.message);
        setIsLoggedIn(false);
        setCurrentUser(null);
        setShowAuth(true);
      }
    } catch (err) {
      console.log('로그인 상태 확인 실패:', err);
      setIsLoggedIn(false);
      setCurrentUser(null);
      setShowAuth(true);
    }
  };

  const handleLoginSuccess = (user) => {
    console.log("=== 로그인 성공 ===");
    console.log("받은 사용자 정보:", user);
    setIsLoggedIn(true);
    setCurrentUser(user);
    setShowAuth(false);
    console.log("설정된 currentUser:", user);
    console.log("isAdmin 값:", user.isAdmin);
  };

  const handleSignupSuccess = (data) => {
    alert('회원가입이 완료되었습니다! 로그인해주세요.');
    setShowLogin(true);
  };

  const handleLogout = async () => {
    try {
      const response = await safeFetch('http://localhost:80/api/logout', {
        method: 'POST'
      });
      console.log("로그아웃 응답:", response);
      setIsLoggedIn(false);
      setCurrentUser(null);
      setShowAuth(true);
      setShowLogin(true);
    } catch (err) {
      console.error('로그아웃 오류:', err);
    }
  };

  const switchToSignup = () => {
    setShowLogin(false);
  };

  const switchToLogin = () => {
    setShowLogin(true);
  };

  const testApiConnection = async () => {
    try {
      const response = await axios.get('http://localhost:80/data/api/test');
      console.log('API 연결 테스트 성공:', response.data);
      return true;
    } catch (err) {
      console.error('API 연결 테스트 실패:', err);
      return false;
    }
  };

  const checkMovieStatusCounts = async () => {
    try {
      const response = await axios.get('http://localhost:80/data/api/movie-status-counts');
      console.log('영화 상태별 개수:', response.data);
      return response.data;
    } catch (err) {
      console.error('영화 상태별 개수 조회 실패:', err);
      return null;
    }
  };

  const fetchStats = async () => {
    try {
      const response = await axios.get('http://localhost:80/data/api/stats');
      setStats(response.data);
    } catch (error) {
      console.error('통계 조회 실패:', error);
      setError('통계 조회에 실패했습니다.');
    }
  };

  const fetchMovieList = async (page = 0) => {
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-list?page=${page}&size=20`);
      setMovieListData(response.data);
    } catch (error) {
      console.error('영화 목록 조회 실패:', error);
      setError('영화 목록 조회에 실패했습니다.');
    }
  };

  const fetchMovieListDto = async (page = 0) => {
    setLoading(true);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-list-dto?page=${page}&size=20`);
      console.log('MovieList DTO API Response:', response.data);
      setMovieListDtoData(response.data);
    } catch (err) {
      console.error('MovieList DTO API Error:', err);
      setError('MovieList DTO 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const fetchMovieDetail = async (page = 0) => {
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-detail?page=${page}&size=20`);
      setMovieDetailData(response.data);
    } catch (error) {
      console.error('영화 상세 조회 실패:', error);
      setError('영화 상세 조회에 실패했습니다.');
    }
  };

  const fetchMovieDetailDto = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-detail-dto?page=${page}&size=20`);
      console.log('MovieDetail DTO API Response:', response.data);
      setMovieDetailDtoData(response.data);
    } catch (err) {
      console.error('MovieDetail DTO API Error:', err);
      setError('MovieDetail DTO 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 영화 제목으로 검색하는 함수
  const searchMoviesByTitle = async (keyword, page = 0) => {
    if (!keyword || keyword.trim() === '') {
      // 검색어가 없으면 전체 목록을 가져옴
      fetchMovieDetailDto(page);
      return;
    }

    setIsSearching(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-detail-dto/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=20`);
      console.log('영화 검색 결과:', response.data);
      setSearchResults(response.data);
    } catch (err) {
      console.error('영화 검색 실패:', err);
      setError('영화 검색에 실패했습니다.');
      // 검색 실패 시 전체 목록을 가져옴
      fetchMovieDetailDto(page);
    } finally {
      setIsSearching(false);
    }
  };

  // 검색어 변경 핸들러
  const handleSearchChange = (e) => {
    setSearchKeyword(e.target.value);
  };

  // 검색 실행 핸들러
  const handleSearch = () => {
    setSearchExecuted(true);
    searchMoviesByTitle(searchKeyword, 0);
  };

  // 검색어 초기화 핸들러
  const handleClearSearch = () => {
    setSearchKeyword('');
    setSearchResults({ data: [], total: 0, page: 0, totalPages: 0 });
    setSearchExecuted(false);
    fetchMovieDetailDto(0);
  };

  const fetchBoxOffice = async (page = 0) => {
    try {
      const response = await axios.get(`http://localhost:80/data/api/box-office?page=${page}&size=20`);
      setBoxOfficeData(response.data);
    } catch (error) {
      console.error('박스오피스 조회 실패:', error);
      setError('박스오피스 조회에 실패했습니다.');
    }
  };

  const fetchBoxOfficeDto = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/box-office-dto?page=${page}&size=20`);
      console.log('BoxOffice DTO API Response:', response.data);
      setBoxOfficeDtoData(response.data);
    } catch (err) {
      console.error('BoxOffice DTO API Error:', err);
      setError('BoxOffice DTO 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const fetchTopRated = async () => {
    try {
      const response = await axios.get('http://localhost:80/data/api/ratings/top-rated?limit=10');
      setTopRatedData(response.data);
    } catch (error) {
      console.error('평점 높은 영화 조회 실패:', error);
      setError('평점 높은 영화 조회에 실패했습니다.');
    }
  };

  const fetchPopularMovies = async () => {
    try {
      const response = await axios.get('http://localhost:80/data/api/popular-movies?limit=100');
      setPopularMoviesData(response.data);
    } catch (error) {
      console.error('인기 영화 조회 실패:', error);
      setError('인기 영화 조회에 실패했습니다.');
    }
  };

  const fetchComingSoon = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movies/coming-soon?page=${page}&size=20`);
      console.log('Coming Soon API Response:', response.data);
      setComingSoonData(response.data);
    } catch (err) {
      console.error('Coming Soon API Error:', err);
      setError('개봉예정작 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const fetchNowPlaying = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movies/now-playing?page=${page}&size=20`);
      console.log('Now Playing API Response:', response.data);
      setNowPlayingData(response.data);
    } catch (err) {
      console.error('Now Playing API Error:', err);
      setError('개봉중인 영화 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const fetchEnded = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movies/ended?page=${page}&size=20`);
      console.log('Ended Movies API Response:', response.data);
      setEndedData(response.data);
    } catch (err) {
      console.error('Ended Movies API Error:', err);
      setError('상영종료된 영화 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const fetchTmdbRatings = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/ratings/fetch-tmdb');
      alert('TMDB 평점 가져오기가 완료되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('TMDB 평점 가져오기 실패:', error);
      alert('TMDB 평점 가져오기에 실패했습니다.');
    }
  };

  const handleFetchBoxOfficeData = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/boxoffice/daily');
      alert('박스오피스 데이터 가져오기가 완료되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('박스오피스 데이터 가져오기 실패:', error);
      alert('박스오피스 데이터 가져오기에 실패했습니다.');
    }
  };

  const handleReplaceWithPopularMovies = async () => {
    if (window.confirm('정말로 기존 영화 데이터를 인기 영화 100개로 교체하시겠습니까?')) {
      try {
        const response = await axios.post('http://localhost:80/api/admin/movies/replace-with-popular');
        alert('인기 영화 100개로 교체가 완료되었습니다.');
        handleRefresh();
      } catch (error) {
        console.error('인기 영화 교체 실패:', error);
        alert('인기 영화 교체에 실패했습니다.');
      }
    }
  };

  const handleUpdateCharacterNames = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/movies/update-character-names');
      alert('캐릭터명 한국어 업데이트가 완료되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('캐릭터명 업데이트 실패:', error);
      alert('캐릭터명 업데이트에 실패했습니다.');
    }
  };

  const handleFetchPosterUrlsFromTmdb = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/posters/fetch-tmdb');
      alert('TMDB 포스터 URL 가져오기가 완료되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('TMDB 포스터 URL 가져오기 실패:', error);
      alert('TMDB 포스터 URL 가져오기에 실패했습니다.');
    }
  };

  const handleFetchPosterUrlsFromNaver = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/posters/fetch-naver');
      alert('네이버 포스터 URL 가져오기가 완료되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('네이버 포스터 URL 가져오기 실패:', error);
      alert('네이버 포스터 URL 가져오기에 실패했습니다.');
    }
  };

  // 영화 관리 기능들
  const handleMovieClick = (movie) => {
    setSelectedMovie(movie);
    setShowMovieDetail(true);
    setShowMovieForm(false);
  };

  const handleEditMovie = (movie) => {
    setEditingMovie(movie);
    setMovieForm({
      movieNm: movie.movieNm || '',
      movieNmEn: movie.movieNmEn || '',
      description: movie.description || '',
      directorName: movie.directorName || '',
      actors: movie.actors || '',
      tags: movie.tags || '',
      companyNm: movie.companyNm || '',
      openDt: movie.openDt || '',
      showTm: movie.showTm || '',
      genreNm: movie.genreNm || '',
      nationNm: movie.nationNm || '',
      watchGradeNm: movie.watchGradeNm || '',
      prdtYear: movie.prdtYear || '',
      prdtStatNm: movie.prdtStatNm || '',
      typeNm: movie.typeNm || '',
      directors: movie.directors || [],
      totalAudience: movie.totalAudience || 0,
      reservationRate: movie.reservationRate || 0,
      averageRating: movie.averageRating || 0
    });
    setShowMovieForm(true);
    setShowMovieDetail(false);
  };

  const handleAddMovie = () => {
    setEditingMovie(null);
    setMovieForm({
      movieNm: '',
      movieNmEn: '',
      description: '',
      directorName: '',
      actors: '',
      tags: '',
      companyNm: '',
      openDt: '',
      showTm: '',
      genreNm: '',
      nationNm: '',
      watchGradeNm: '',
      prdtYear: '',
      prdtStatNm: '',
      typeNm: '',
      directors: [],
      totalAudience: 0,
      reservationRate: 0,
      averageRating: 0
    });
    setShowMovieForm(true);
    setShowMovieDetail(false);
  };

  const handleDeleteMovie = async (movieCd) => {
    if (window.confirm('정말로 이 영화를 삭제하시겠습니까?')) {
      try {
        const response = await axios.delete(`http://localhost:80/api/movies/${movieCd}`, {
          withCredentials: true,
          headers: {
            'Cache-Control': 'no-cache',
            'Pragma': 'no-cache'
          }
        });
        console.log("영화 삭제 응답:", response.data);
        
        // 응답이 HTML인지 확인
        if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE html>')) {
          alert('API 서버 연결에 문제가 있습니다. HTML이 반환되었습니다.');
          return;
        }
        
        // 응답이 성공인지 확인
        if (response.data && response.data.success) {
          alert('영화가 삭제되었습니다.');
          handleRefresh();
        } else {
          alert('영화 삭제에 실패했습니다: ' + (response.data?.message || '알 수 없는 오류'));
        }
      } catch (error) {
        console.error('영화 삭제 실패:', error);
        alert('영화 삭제에 실패했습니다.');
      }
    }
  };

  const handleSaveMovie = async () => {
    console.log("=== handleSaveMovie 함수 호출됨 ===");
    console.log("API_BASE_URL:", API_BASE_URL);
    console.log("현재 movieForm 데이터:", movieForm);
    console.log("로그인 상태:", isLoggedIn);
    console.log("현재 사용자:", currentUser);
    console.log("관리자 여부:", currentUser?.isAdmin);
    
    // 프론트엔드에서 로그인 상태 확인 (백엔드 재확인 제거)
    if (!isLoggedIn || !currentUser) {
      alert('로그인이 필요합니다. 다시 로그인해주세요.');
      setShowAuth(true);
      return;
    }
    
    if (!currentUser.isAdmin) {
      alert('관리자 권한이 필요합니다.');
      return;
    }
    
    console.log("인증 확인 완료 - 관리자:", currentUser.loginId);
    
    try {
      // 데이터 검증
      if (!movieForm.movieNm || !movieForm.movieNm.trim()) {
        alert('영화 제목을 입력해주세요.');
        return;
      }
      
      console.log("검증 통과, API 호출 시작...");
      
      // 데이터 변환
      const movieData = {
        movieNm: movieForm.movieNm,
        movieNmEn: movieForm.movieNmEn,
        description: movieForm.description,
        companyNm: movieForm.companyNm,
        openDt: movieForm.openDt,
        showTm: parseInt(movieForm.showTm) || 0,
        genreNm: movieForm.genreNm,
        nationNm: movieForm.nationNm,
        watchGradeNm: movieForm.watchGradeNm,
        prdtYear: movieForm.prdtYear,
        prdtStatNm: movieForm.prdtStatNm,
        typeNm: movieForm.typeNm,
        totalAudience: parseInt(movieForm.totalAudience) || 0,
        reservationRate: parseFloat(movieForm.reservationRate) || 0.0,
        averageRating: parseFloat(movieForm.averageRating) || 0.0,
        directors: movieForm.directorName ? [{
          peopleNm: movieForm.directorName
        }] : [],
        actors: movieForm.actors ? movieForm.actors.split(',').map(actor => ({
          peopleNm: actor.trim(),
          cast: actor.trim()
        })) : []
      };
      
      console.log("변환된 movieData:", movieData);
      
      if (editingMovie) {
        const response = await axios.put(`http://localhost:80/api/movies/${editingMovie.movieCd}`, movieData, {
          withCredentials: true,
          headers: {
            'Cache-Control': 'no-cache',
            'Pragma': 'no-cache'
          }
        });
        console.log("영화 수정 응답:", response.data);
        
        // 응답이 HTML인지 확인
        if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE html>')) {
          alert('API 서버 연결에 문제가 있습니다. HTML이 반환되었습니다.');
          return;
        }
        
        // 응답이 성공인지 확인
        if (response.data && response.data.success) {
          alert('영화가 수정되었습니다.');
        } else {
          alert('영화 수정에 실패했습니다: ' + (response.data?.message || '알 수 없는 오류'));
          return;
        }
      } else {
        console.log("=== 영화 등록 요청 시작 ===");
        const requestUrl = 'http://localhost:80/api/movies';
        console.log("요청 URL:", requestUrl);
        console.log("요청 데이터:", movieData);
        
        const response = await axios.post(requestUrl, movieData, {
          withCredentials: true,
          headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0',
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
          }
        });
        
        console.log("=== 영화 등록 응답 ===");
        console.log("응답 상태:", response.status);
        console.log("응답 헤더:", response.headers);
        console.log("응답 데이터:", response.data);
        console.log("응답 타입:", typeof response.data);
        
        // 응답이 HTML인지 확인
        if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE html>')) {
          alert('API 서버 연결에 문제가 있습니다. HTML이 반환되었습니다.');
          return;
        }
        
        // 응답이 성공인지 확인
        if (response.data && response.data.success) {
          alert('영화가 등록되었습니다.');
        } else {
          alert('영화 등록에 실패했습니다: ' + (response.data?.message || '알 수 없는 오류'));
          return;
        }
      }
      setShowMovieForm(false);
      handleRefresh();
    } catch (error) {
      console.error('영화 저장 실패:', error);
      console.error('에러 응답:', error.response?.data);
      console.error('요청 URL:', error.config?.url);
      
      // 401 오류 시 로그인 페이지로 이동
      if (error.response?.status === 401) {
        alert('로그인이 필요합니다. 다시 로그인해주세요.');
        setShowAuth(true);
        return;
      }
      
      alert('영화 저장에 실패했습니다: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleLikeMovie = async (movieCd) => {
    try {
      const response = await axios.post(`http://localhost:80/api/movies/${movieCd}/like`, {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache',
          'Pragma': 'no-cache'
        }
      });
      console.log("영화 좋아요 응답:", response.data);
      
      // 응답이 HTML인지 확인
      if (typeof response.data === 'string' && response.data.includes('<!DOCTYPE html>')) {
        alert('API 서버 연결에 문제가 있습니다. HTML이 반환되었습니다.');
        return;
      }
      
      // 응답이 성공인지 확인
      if (response.data && response.data.success) {
        alert('좋아요가 추가되었습니다.');
        handleRefresh();
      } else {
        alert('좋아요 추가에 실패했습니다: ' + (response.data?.message || '알 수 없는 오류'));
      }
    } catch (error) {
      console.error('좋아요 실패:', error);
      alert('좋아요에 실패했습니다.');
    }
  };

  useEffect(() => {
    fetchStats();
    if (activeTab === 'movie-list') fetchMovieList();
    if (activeTab === 'movie-detail') fetchMovieDetail();
    if (activeTab === 'box-office') fetchBoxOffice();
    if (activeTab === 'box-office-dto') fetchBoxOfficeDto();
    if (activeTab === 'movie-detail-dto') fetchMovieDetailDto();
    if (activeTab === 'movie-list-dto') fetchMovieListDto();
    if (activeTab === 'topRated') fetchTopRated();
    if (activeTab === 'popular-movies') fetchPopularMovies();
    if (activeTab === 'coming-soon') fetchComingSoon();
    if (activeTab === 'now-playing') fetchNowPlaying();
    if (activeTab === 'ended') fetchEnded();
  }, [activeTab]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSearchKeyword('');
    setSearchResults({ data: [], total: 0, page: 0, totalPages: 0 });
    setSearchExecuted(false);
  };

  const handleRefresh = () => {
    fetchStats();
    if (activeTab === 'movie-list') fetchMovieList();
    if (activeTab === 'movie-detail') fetchMovieDetail();
    if (activeTab === 'box-office') fetchBoxOffice();
    if (activeTab === 'box-office-dto') fetchBoxOfficeDto();
    if (activeTab === 'movie-detail-dto') fetchMovieDetailDto();
    if (activeTab === 'movie-list-dto') fetchMovieListDto();
    if (activeTab === 'topRated') fetchTopRated();
    if (activeTab === 'popular-movies') fetchPopularMovies();
    if (activeTab === 'coming-soon') fetchComingSoon();
    if (activeTab === 'now-playing') fetchNowPlaying();
    if (activeTab === 'ended') fetchEnded();
  };

  const renderStats = () => (
    <div>
      <h2>📊 데이터 통계</h2>
      {stats && (
        <div className="stats-grid">
          <div className="stat-card">
            <h3>MovieList</h3>
            <p>{stats.movieListCount?.toLocaleString() || 0}개</p>
          </div>
          <div className="stat-card">
            <h3>MovieDetail</h3>
            <p>{stats.movieDetailCount?.toLocaleString() || 0}개</p>
          </div>
          <div className="stat-card">
            <h3>BoxOffice</h3>
            <p>{stats.boxOfficeCount?.toLocaleString() || 0}개</p>
          </div>
        </div>
      )}
      
      {currentUser && currentUser.role === 'ADMIN' && (
        <div>
          <h3>🔧 관리 기능</h3>
          <div className="button-grid">
            <button onClick={handleFetchBoxOfficeData} className="admin-button">
              📊 박스오피스 데이터 가져오기
            </button>
            <button onClick={fetchTmdbRatings} className="admin-button">
              ⭐ TMDB 평점 가져오기
            </button>
            <button onClick={handleReplaceWithPopularMovies} className="admin-button">
              🎬 인기 영화 100개로 교체
            </button>
            <button onClick={handleUpdateCharacterNames} className="admin-button">
              🇰🇷 캐릭터명 한국어 업데이트
            </button>
            <button onClick={handleFetchPosterUrlsFromTmdb} className="admin-button">
              🎭 TMDB 포스터 URL 가져오기
            </button>
            <button onClick={handleFetchPosterUrlsFromNaver} className="admin-button">
              🎭 네이버 포스터 URL 가져오기
            </button>
          </div>
        </div>
      )}
    </div>
  );

  const renderMovieList = () => (
    <div>
      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Movie CD</th>
            <th>Movie Name</th>
            <th>Movie Name EN</th>
            <th>Open Date</th>
            <th>Genre</th>
            <th>Nation</th>
          </tr>
        </thead>
        <tbody>
          {movieListData.data && movieListData.data.length > 0 ? (
            movieListData.data.map((item, index) => (
              <tr key={index}>
                <td>{item.id}</td>
                <td>{item.movieCd}</td>
                <td>{item.movieNm}</td>
                <td>{item.movieNmEn || '-'}</td>
                <td>{item.openDt || '-'}</td>
                <td>{item.genreNm || '-'}</td>
                <td>{item.nationNm || '-'}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="7" style={{textAlign: 'center', padding: '20px'}}>
                {loading ? '데이터를 불러오는 중...' : '데이터가 없습니다.'}
              </td>
            </tr>
          )}
        </tbody>
      </table>
      {movieListData.data && movieListData.data.length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => fetchMovieList(movieListData.page - 1)}
            disabled={movieListData.page === 0}
          >
            이전
          </button>
          <span>페이지 {movieListData.page + 1} / {movieListData.totalPages}</span>
          <button 
            onClick={() => fetchMovieList(movieListData.page + 1)}
            disabled={movieListData.page >= movieListData.totalPages - 1}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );

  const renderMovieDetail = () => (
    <div>
      <table className="data-table">
        <thead>
          <tr>
            <th>Movie CD</th>
            <th>Movie Name</th>
            <th>Movie Name EN</th>
            <th>Open Date</th>
            <th>Genre</th>
            <th>Nation</th>
            <th>Watch Grade</th>
            <th>Show Time</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {movieDetailData.data && movieDetailData.data.length > 0 ? (
            movieDetailData.data.map((item, index) => (
              <tr key={index}>
                <td>{item.movieCd}</td>
                <td>{item.movieNm}</td>
                <td>{item.movieNmEn || '-'}</td>
                <td>{item.openDt || '-'}</td>
                <td>{item.genreNm || '-'}</td>
                <td>{item.nationNm || '-'}</td>
                <td>{item.watchGradeNm || '-'}</td>
                <td>{item.showTm || '-'}</td>
                <td>{item.status || '-'}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="9" style={{textAlign: 'center', padding: '20px'}}>
                {loading ? '데이터를 불러오는 중...' : '데이터가 없습니다.'}
              </td>
            </tr>
          )}
        </tbody>
      </table>
      {movieDetailData.data && movieDetailData.data.length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => fetchMovieDetail(movieDetailData.page - 1)}
            disabled={movieDetailData.page === 0}
          >
            이전
          </button>
          <span>페이지 {movieDetailData.page + 1} / {movieDetailData.totalPages}</span>
          <button 
            onClick={() => fetchMovieDetail(movieDetailData.page + 1)}
            disabled={movieDetailData.page >= movieDetailData.totalPages - 1}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );

  const renderBoxOffice = () => (
    <div>
      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Movie CD</th>
            <th>Movie Name</th>
            <th>Rank</th>
            <th>Audience Count</th>
            <th>Sales Amount</th>
            <th>Target Date</th>
            <th>Rank Type</th>
            <th>Movie Detail ID</th>
          </tr>
        </thead>
        <tbody>
          {boxOfficeData.data && boxOfficeData.data.length > 0 ? (
            boxOfficeData.data.map((item, index) => (
              <tr key={index}>
                <td>{item.id}</td>
                <td>{item.movieCd}</td>
                <td>{item.movieNm}</td>
                <td>{item.rank}</td>
                <td>{item.audiCnt?.toLocaleString() || '-'}</td>
                <td>{item.salesAmt?.toLocaleString() || '-'}</td>
                <td>{item.targetDate || '-'}</td>
                <td>{item.rankType || '-'}</td>
                <td className={item.movieDetail ? 'success-value' : 'null-value'}>
                  {item.movieDetail ? item.movieDetail.movieCd : 'NULL'}
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="9" style={{textAlign: 'center', padding: '20px'}}>
                {loading ? '데이터를 불러오는 중...' : '데이터가 없습니다.'}
              </td>
            </tr>
          )}
        </tbody>
      </table>
      {boxOfficeData.data && boxOfficeData.data.length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => fetchBoxOffice(boxOfficeData.page - 1)}
            disabled={boxOfficeData.page === 0}
          >
            이전
          </button>
          <span>페이지 {boxOfficeData.page + 1} / {boxOfficeData.totalPages}</span>
          <button 
            onClick={() => fetchBoxOffice(boxOfficeData.page + 1)}
            disabled={boxOfficeData.page >= boxOfficeData.totalPages - 1}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );

  const renderBoxOfficeDto = () => (
    <div>
      <div className="movie-grid">
        {boxOfficeDtoData.data && boxOfficeDtoData.data.length > 0 ? (
          boxOfficeDtoData.data.map((item, index) => (
            <div key={index} className="movie-card" style={{cursor: 'pointer'}} onClick={() => handleMovieClick(item)}>
              <div className="movie-poster">
                {item.posterUrl ? (
                  <img src={item.posterUrl} alt={item.movieNm} />
                ) : (
                  <div className="no-poster">No Poster</div>
                )}
              </div>
              <div className="movie-info">
                <h3>{item.movieNm}</h3>
                <div className="movie-details">
                  <p><strong>순위:</strong> {item.rank}위</p>
                  <p><strong>예매율:</strong> {item.reservationRate ? `${item.reservationRate}%` : '-'}</p>
                  <p><strong>관객수:</strong> {item.audienceCount ? item.audienceCount.toLocaleString() : '-'}명</p>
                  <p><strong>매출액:</strong> {item.salesAmount ? item.salesAmount.toLocaleString() : '-'}원</p>
                  <p><strong>장르:</strong> {item.genreNm || '-'}</p>
                  <p><strong>감독:</strong> {item.directorName || '-'}</p>
                  <p><strong>상태:</strong> {item.movieStatus || '-'}</p>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
            {loading ? '데이터를 불러오는 중...' : '박스오피스 데이터가 없습니다.'}
          </div>
        )}
      </div>
      {boxOfficeDtoData.data && boxOfficeDtoData.data.length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => fetchBoxOfficeDto(boxOfficeDtoData.page - 1)}
            disabled={boxOfficeDtoData.page === 0}
          >
            이전
          </button>
          <span>페이지 {boxOfficeDtoData.page + 1} / {boxOfficeDtoData.totalPages}</span>
          <button 
            onClick={() => fetchBoxOfficeDto(boxOfficeDtoData.page + 1)}
            disabled={boxOfficeDtoData.page >= boxOfficeDtoData.totalPages - 1}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );

  const renderMovieDetailDto = () => (
    <div>
      <div style={{marginBottom: '20px', textAlign: 'center'}}>
        <button 
          onClick={handleAddMovie}
          style={{
            padding: '10px 20px',
            backgroundColor: '#28a745',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            cursor: 'pointer',
            marginRight: '10px'
          }}
        >
          ➕ 영화 등록
        </button>
      </div>
      
      <div className="movie-grid">
        {/* 검색 결과가 있으면 검색 결과를, 없으면 전체 목록을 표시 */}
        {(searchKeyword && searchResults.data ? searchResults.data : movieDetailDtoData.data) && 
         (searchKeyword && searchResults.data ? searchResults.data : movieDetailDtoData.data).length > 0 ? (
          (searchKeyword && searchResults.data ? searchResults.data : movieDetailDtoData.data).map((item, index) => (
            <div key={index} className="movie-card" style={{cursor: 'pointer'}} onClick={() => handleMovieClick(item)}>
              <div className="movie-poster">
                {item.posterUrl ? (
                  <img src={item.posterUrl} alt={item.movieNm} />
                ) : (
                  <div className="no-poster">No Poster</div>
                )}
              </div>
              <div className="movie-info">
                <h3>{item.movieNm}</h3>
                <p className="movie-title-en">{item.movieNmEn || '-'}</p>
                <div className="movie-details">
                  <p><strong>감독:</strong> {item.directorName || '-'}</p>
                  <p><strong>장르:</strong> {item.genreNm || '-'}</p>
                  <p><strong>개봉일:</strong> {item.openDt || '-'}</p>
                  <p><strong>상영시간:</strong> {item.showTm || '-'}분</p>
                  <p><strong>예매율:</strong> {item.reservationRate ? `${item.reservationRate}%` : '-'}</p>
                  <p><strong>누적관객:</strong> {item.totalAudience ? item.totalAudience.toLocaleString() : '-'}명</p>
                </div>
                <div className="movie-actions" style={{marginTop: '10px', display: 'flex', gap: '5px'}}>
                  <button 
                    onClick={(e) => {e.stopPropagation(); handleEditMovie(item);}}
                    style={{
                      padding: '5px 10px',
                      backgroundColor: '#007bff',
                      color: 'white',
                      border: 'none',
                      borderRadius: '3px',
                      cursor: 'pointer',
                      fontSize: '12px'
                    }}
                  >
                    수정
                  </button>
                  <button 
                    onClick={(e) => {e.stopPropagation(); handleDeleteMovie(item.movieCd);}}
                    style={{
                      padding: '5px 10px',
                      backgroundColor: '#dc3545',
                      color: 'white',
                      border: 'none',
                      borderRadius: '3px',
                      cursor: 'pointer',
                      fontSize: '12px'
                    }}
                  >
                    삭제
                  </button>
                  <button 
                    onClick={(e) => {e.stopPropagation(); handleLikeMovie(item.movieCd);}}
                    style={{
                      padding: '5px 10px',
                      backgroundColor: '#ffc107',
                      color: 'black',
                      border: 'none',
                      borderRadius: '3px',
                      cursor: 'pointer',
                      fontSize: '12px'
                    }}
                  >
                    ❤️ 좋아요
                  </button>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
            {loading || isSearching ? '데이터를 불러오는 중...' : 
             searchKeyword ? `"${searchKeyword}" 검색 결과가 없습니다.` : '데이터가 없습니다.'}
          </div>
        )}
      </div>
      
      {/* 페이지네이션 - 검색 결과가 있으면 검색 결과 페이지네이션, 없으면 전체 목록 페이지네이션 */}
      {(searchKeyword && searchResults.data ? searchResults.data : movieDetailDtoData.data) && 
       (searchKeyword && searchResults.data ? searchResults.data : movieDetailDtoData.data).length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => searchKeyword ? 
              searchMoviesByTitle(searchKeyword, (searchKeyword && searchResults.data ? searchResults.page : movieDetailDtoData.page) - 1) :
              fetchMovieDetailDto(movieDetailDtoData.page - 1)
            }
            disabled={(searchKeyword && searchResults.data ? searchResults.page : movieDetailDtoData.page) === 0}
          >
            이전
          </button>
          <span>페이지 {(searchKeyword && searchResults.data ? searchResults.page : movieDetailDtoData.page) + 1} / {(searchKeyword && searchResults.data ? searchResults.totalPages : movieDetailDtoData.totalPages)}</span>
          <button 
            onClick={() => searchKeyword ? 
              searchMoviesByTitle(searchKeyword, (searchKeyword && searchResults.data ? searchResults.page : movieDetailDtoData.page) + 1) :
              fetchMovieDetailDto(movieDetailDtoData.page + 1)
            }
            disabled={(searchKeyword && searchResults.data ? searchResults.page : movieDetailDtoData.page) >= (searchKeyword && searchResults.data ? searchResults.totalPages : movieDetailDtoData.totalPages) - 1}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );

  const renderMovieListDto = () => (
    <div>
      <div className="movie-grid">
        {movieListDtoData.data && movieListDtoData.data.length > 0 ? (
          movieListDtoData.data.map((item, index) => (
            <div key={index} className="movie-card">
              <div className="movie-poster">
                {item.posterUrl ? (
                  <img src={item.posterUrl} alt={item.movieNm} />
                ) : (
                  <div className="no-poster">No Poster</div>
                )}
              </div>
              <div className="movie-info">
                <h3>{item.movieNm}</h3>
                <p className="movie-title-en">{item.movieNmEn || '-'}</p>
                <div className="movie-details">
                  <p><strong>개봉일:</strong> {item.openDt || '-'}</p>
                  <p><strong>장르:</strong> {item.genreNm || '-'}</p>
                  <p><strong>제작국가:</strong> {item.nationNm || '-'}</p>
                  <p><strong>관람등급:</strong> {item.watchGradeNm || '-'}</p>
                  <p><strong>상태:</strong> {item.status || '-'}</p>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
            {loading ? '데이터를 불러오는 중...' : '데이터가 없습니다.'}
          </div>
        )}
      </div>
      {movieListDtoData.data && movieListDtoData.data.length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => fetchMovieListDto(movieListDtoData.page - 1)}
            disabled={movieListDtoData.page === 0}
          >
            이전
          </button>
          <span>페이지 {movieListDtoData.page + 1} / {movieListDtoData.totalPages}</span>
          <button 
            onClick={() => fetchMovieListDto(movieListDtoData.page + 1)}
            disabled={movieListDtoData.page >= movieListDtoData.totalPages - 1}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );

  // 영화 상세 보기 모달
  const renderMovieDetailModal = () => {
    if (!showMovieDetail || !selectedMovie) return null;

    return (
      <div className="modal-overlay" onClick={() => setShowMovieDetail(false)}>
        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header">
            <h2>{selectedMovie.movieNm}</h2>
            <button 
              className="modal-close"
              onClick={() => setShowMovieDetail(false)}
            >
              ✕
            </button>
          </div>
          <div className="modal-body">
            <div className="movie-detail-grid">
              <div className="movie-detail-poster">
                {selectedMovie.posterUrl ? (
                  <img src={selectedMovie.posterUrl} alt={selectedMovie.movieNm} />
                ) : (
                  <div className="no-poster">No Poster</div>
                )}
              </div>
              <div className="movie-detail-info">
                <h3>{selectedMovie.movieNmEn}</h3>
                <div className="movie-detail-section">
                  <h4>기본 정보</h4>
                  <p><strong>감독:</strong> {selectedMovie.directorName || '-'}</p>
                  <p><strong>장르:</strong> {selectedMovie.genreNm || '-'}</p>
                  <p><strong>개봉일:</strong> {selectedMovie.openDt || '-'}</p>
                  <p><strong>상영시간:</strong> {selectedMovie.showTm || '-'}분</p>
                  <p><strong>제작국가:</strong> {selectedMovie.nationNm || '-'}</p>
                  <p><strong>관람등급:</strong> {selectedMovie.watchGradeNm || '-'}</p>
                  <p><strong>배급사:</strong> {selectedMovie.companyNm || '-'}</p>
                </div>
                <div className="movie-detail-section">
                  <h4>줄거리</h4>
                  <p>{selectedMovie.description || '줄거리가 없습니다.'}</p>
                </div>
                <div className="movie-detail-section">
                  <h4>통계</h4>
                  <p><strong>예매율:</strong> {selectedMovie.reservationRate ? `${selectedMovie.reservationRate}%` : '-'}</p>
                  <p><strong>누적관객:</strong> {selectedMovie.totalAudience ? selectedMovie.totalAudience.toLocaleString() : '-'}명</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  // 영화 등록/수정 폼
  const renderMovieForm = () => {
    if (!showMovieForm) return null;

    return (
      <div className="modal-overlay" onClick={() => setShowMovieForm(false)}>
        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header">
            <h2>{editingMovie ? '영화 수정' : '영화 등록'}</h2>
            <button 
              className="modal-close"
              onClick={() => setShowMovieForm(false)}
            >
              ✕
            </button>
          </div>
          <div className="modal-body">
            <form onSubmit={(e) => {e.preventDefault(); handleSaveMovie();}}>
              <div className="form-group">
                <label>영화 제목 (한글)</label>
                <input
                  type="text"
                  value={movieForm.movieNm}
                  onChange={(e) => setMovieForm({...movieForm, movieNm: e.target.value})}
                  required
                />
              </div>
              <div className="form-group">
                <label>영화 제목 (영문)</label>
                <input
                  type="text"
                  value={movieForm.movieNmEn}
                  onChange={(e) => setMovieForm({...movieForm, movieNmEn: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>줄거리</label>
                <textarea
                  value={movieForm.description}
                  onChange={(e) => setMovieForm({...movieForm, description: e.target.value})}
                  rows="4"
                />
              </div>
              <div className="form-group">
                <label>감독</label>
                <input
                  type="text"
                  value={movieForm.directorName}
                  onChange={(e) => setMovieForm({...movieForm, directorName: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>배우 (쉼표로 구분)</label>
                <input
                  type="text"
                  value={movieForm.actors}
                  onChange={(e) => setMovieForm({...movieForm, actors: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>태그 (쉼표로 구분)</label>
                <input
                  type="text"
                  value={movieForm.tags}
                  onChange={(e) => setMovieForm({...movieForm, tags: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>배급사</label>
                <input
                  type="text"
                  value={movieForm.companyNm}
                  onChange={(e) => setMovieForm({...movieForm, companyNm: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>개봉일</label>
                <input
                  type="date"
                  value={movieForm.openDt}
                  onChange={(e) => setMovieForm({...movieForm, openDt: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>상영시간 (분)</label>
                <input
                  type="number"
                  value={movieForm.showTm}
                  onChange={(e) => setMovieForm({...movieForm, showTm: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>장르</label>
                <input
                  type="text"
                  value={movieForm.genreNm}
                  onChange={(e) => setMovieForm({...movieForm, genreNm: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>제작국가</label>
                <input
                  type="text"
                  value={movieForm.nationNm}
                  onChange={(e) => setMovieForm({...movieForm, nationNm: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>관람등급</label>
                <input
                  type="text"
                  value={movieForm.watchGradeNm}
                  onChange={(e) => setMovieForm({...movieForm, watchGradeNm: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>제작연도</label>
                <input
                  type="text"
                  value={movieForm.prdtYear}
                  onChange={(e) => setMovieForm({...movieForm, prdtYear: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>제작상태</label>
                <input
                  type="text"
                  value={movieForm.prdtStatNm}
                  onChange={(e) => setMovieForm({...movieForm, prdtStatNm: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>영화유형</label>
                <input
                  type="text"
                  value={movieForm.typeNm}
                  onChange={(e) => setMovieForm({...movieForm, typeNm: e.target.value})}
                />
              </div>
              <div className="form-actions">
                <button type="submit" className="btn-primary">
                  {editingMovie ? '수정' : '등록'}
                </button>
                <button type="button" onClick={() => setShowMovieForm(false)} className="btn-secondary">
                  취소
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    );
  };

  // 검색 결과 전용 렌더링 함수
  const renderSearchResults = () => (
    <div className="movie-grid">
      {searchResults.data && searchResults.data.length > 0 ? (
        searchResults.data.map((item, index) => (
          <div key={index} className="movie-card" style={{cursor: 'pointer'}} onClick={() => handleMovieClick(item)}>
            <div className="movie-poster">
              {item.posterUrl ? (
                <img src={item.posterUrl} alt={item.movieNm} />
              ) : (
                <div className="no-poster">No Poster</div>
              )}
            </div>
            <div className="movie-info">
              <h3>{item.movieNm}</h3>
              <div className="movie-details">
                <p><strong>개봉일:</strong> {item.openDt || '-'}</p>
                <p><strong>장르:</strong> {item.genreNm || '-'}</p>
                <p><strong>제작국가:</strong> {item.nationNm || '-'}</p>
                <p><strong>상태:</strong> {item.status || '-'}</p>
              </div>
            </div>
          </div>
        ))
      ) : (
        <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
          {isSearching ? '데이터를 불러오는 중...' : `"${searchKeyword}" 검색 결과가 없습니다.`}
        </div>
      )}
      {/* 페이지네이션 */}
      {searchResults.data && searchResults.data.length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => searchMoviesByTitle(searchKeyword, searchResults.page - 1)}
            disabled={searchResults.page === 0}
          >
            이전
          </button>
          <span>페이지 {searchResults.page + 1} / {searchResults.totalPages}</span>
          <button 
            onClick={() => searchMoviesByTitle(searchKeyword, searchResults.page + 1)}
            disabled={searchResults.page >= searchResults.totalPages - 1}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );

  return (
    <div className="App">
      {showSocialJoin ? (
        // 소셜 로그인 닉네임 입력 페이지
        <SocialJoin />
      ) : showAuth ? (
        // 로그인/회원가입 화면
        showLogin ? (
          <Login 
            onLoginSuccess={handleLoginSuccess} 
            onSwitchToSignup={switchToSignup} 
          />
        ) : (
          <Signup 
            onSignupSuccess={handleSignupSuccess} 
            onSwitchToLogin={switchToLogin} 
          />
        )
      ) : (
        // 메인 애플리케이션 화면
        <>
          <div className="header">
            <h1>영화 데이터 관리 시스템</h1>
            {currentUser && (
              <div className="user-info">
                <span>안녕하세요, {currentUser.nickname}님!</span>
                <button onClick={handleLogout} className="logout-button">
                  로그아웃
                </button>
              </div>
            )}
            {/* 헤더 검색창 */}
            <div style={{
              display: 'flex', 
              justifyContent: 'center', 
              alignItems: 'center', 
              gap: '10px', 
              marginTop: '15px',
              flexWrap: 'wrap'
            }}>
              <input
                type="text"
                placeholder="영화 제목을 입력하세요..."
                value={searchKeyword}
                onChange={handleSearchChange}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                style={{
                  padding: '10px 15px',
                  border: '2px solid #007bff',
                  borderRadius: '5px',
                  width: '300px',
                  fontSize: '14px',
                  outline: 'none'
                }}
              />
              <button 
                onClick={handleSearch}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#007bff',
                  color: 'white',
                  border: 'none',
                  borderRadius: '5px',
                  cursor: 'pointer',
                  fontSize: '14px',
                  fontWeight: 'bold'
                }}
              >
                🔍 검색
              </button>
              <button 
                onClick={handleClearSearch}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#6c757d',
                  color: 'white',
                  border: 'none',
                  borderRadius: '5px',
                  cursor: 'pointer',
                  fontSize: '14px',
                  fontWeight: 'bold'
                }}
              >
                초기화
              </button>
            </div>
            
            {/* 검색 상태 표시 */}
            {isSearching && (
              <div style={{
                color: '#007bff', 
                textAlign: 'center',
                fontWeight: 'bold',
                fontSize: '14px',
                marginTop: '10px'
              }}>
                🔍 검색 중...
              </div>
            )}
            {/* 검색 결과 개수 메시지 완전 삭제 */}
          </div>
          
          <div className="container">
            <div className="sidebar">
              <div className="tab-buttons">
                <button 
                  className={activeTab === 'stats' ? 'active' : ''} 
                  onClick={() => handleTabChange('stats')}
                >
                  📊 통계
                </button>
                <button 
                  className={activeTab === 'movie-list' ? 'active' : ''} 
                  onClick={() => handleTabChange('movie-list')}
                >
                  🎬 영화 목록
                </button>
                <button 
                  className={activeTab === 'movie-detail' ? 'active' : ''} 
                  onClick={() => handleTabChange('movie-detail')}
                >
                  🎭 영화 상세
                </button>
                <button 
                  className={activeTab === 'box-office' ? 'active' : ''} 
                  onClick={() => handleTabChange('box-office')}
                >
                  💰 박스오피스
                </button>
                <button 
                  className={activeTab === 'box-office-dto' ? 'active' : ''} 
                  onClick={() => handleTabChange('box-office-dto')}
                >
                  📈 박스오피스 DTO
                </button>
                <button 
                  className={activeTab === 'movie-detail-dto' ? 'active' : ''} 
                  onClick={() => handleTabChange('movie-detail-dto')}
                >
                  🎪 영화 상세 DTO
                </button>
                <button 
                  className={activeTab === 'movie-list-dto' ? 'active' : ''} 
                  onClick={() => handleTabChange('movie-list-dto')}
                >
                  📋 영화 목록 DTO
                </button>
                <button 
                  className={activeTab === 'top-rated' ? 'active' : ''} 
                  onClick={() => handleTabChange('top-rated')}
                >
                  ⭐ 평점 높은 영화
                </button>
                <button 
                  className={activeTab === 'popular' ? 'active' : ''} 
                  onClick={() => handleTabChange('popular')}
                >
                  🔥 인기 영화
                </button>
                <button 
                  className={activeTab === 'coming-soon' ? 'active' : ''} 
                  onClick={() => handleTabChange('coming-soon')}
                >
                  🎬 개봉예정작
                </button>
                <button 
                  className={activeTab === 'now-playing' ? 'active' : ''} 
                  onClick={() => handleTabChange('now-playing')}
                >
                  🎭 개봉중
                </button>
                <button 
                  className={activeTab === 'ended' ? 'active' : ''} 
                  onClick={() => handleTabChange('ended')}
                >
                  🎬 상영종료
                </button>
              </div>
            </div>
            
            <div className="main-content">
              {error && (
                <div className="error-message" style={{marginBottom: '20px', padding: '10px', backgroundColor: '#fee', color: '#c33', borderRadius: '5px'}}>
                  {error}
                </div>
              )}
              
              {loading && (
                <div style={{textAlign: 'center', padding: '20px'}}>
                  데이터를 불러오는 중...
                </div>
              )}

              {searchExecuted ? (
                renderSearchResults()
              ) : (
                <>
                  {activeTab === 'stats' && renderStats()}
                  {activeTab === 'movie-list' && renderMovieList()}
                  {activeTab === 'movie-detail' && renderMovieDetail()}
                  {activeTab === 'box-office' && renderBoxOffice()}
                  {activeTab === 'box-office-dto' && renderBoxOfficeDto()}
                  {activeTab === 'movie-detail-dto' && renderMovieDetailDto()}
                  {activeTab === 'movie-list-dto' && renderMovieListDto()}
                  {activeTab === 'top-rated' && (
                    <div>
                      <div style={{marginBottom: '20px'}}>
                        <button onClick={fetchTopRated} style={{marginRight: '10px'}}>평점 높은 영화 조회</button>
                        <button onClick={handleRefresh}>새로고침</button>
                      </div>
                      <div className="movie-grid">
                        {topRatedData.length > 0 ? (
                          topRatedData.map((movie, index) => (
                            <div key={index} className="movie-card">
                              <div className="movie-poster">
                                {movie.posterUrl ? (
                                  <img src={movie.posterUrl} alt={movie.movieNm} />
                                ) : (
                                  <div className="no-poster">No Poster</div>
                                )}
                              </div>
                              <div className="movie-info">
                                <h3>{movie.movieNm}</h3>
                                <p className="movie-title-en">{movie.movieNmEn || '-'}</p>
                                <div className="movie-details">
                                  <p><strong>평균 평점:</strong> ⭐ {movie.averageRating.toFixed(1)}</p>
                                  <p><strong>개봉일:</strong> {movie.openDt || '-'}</p>
                                  <p><strong>장르:</strong> {movie.genreNm || '-'}</p>
                                </div>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
                            평점 높은 영화가 없습니다.
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                  {activeTab === 'popular' && (
                    <div>
                      <div style={{marginBottom: '20px'}}>
                        <button onClick={fetchPopularMovies} style={{marginRight: '10px'}}>인기 영화 조회</button>
                        <button onClick={handleRefresh}>새로고침</button>
                      </div>
                      <div className="movie-grid">
                        {popularMoviesData.length > 0 ? (
                          popularMoviesData.map((movie, index) => (
                            <div key={index} className="movie-card">
                              <div className="movie-poster">
                                {movie.posterUrl ? (
                                  <img src={movie.posterUrl} alt={movie.movieNm} />
                                ) : (
                                  <div className="no-poster">No Poster</div>
                                )}
                              </div>
                              <div className="movie-info">
                                <h3>{movie.movieNm}</h3>
                                <p className="movie-title-en">{movie.movieNmEn || '-'}</p>
                                <div className="movie-details">
                                  <p><strong>개봉일:</strong> {movie.openDt || '-'}</p>
                                  <p><strong>장르:</strong> {movie.genreNm || '-'}</p>
                                  <p><strong>제작국가:</strong> {movie.nationNm || '-'}</p>
                                </div>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
                            인기 영화가 없습니다.
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                  {activeTab === 'coming-soon' && (
                    <div>
                      <div style={{marginBottom: '20px'}}>
                        <button onClick={() => fetchComingSoon()} style={{marginRight: '10px'}}>개봉예정작 조회</button>
                        <button onClick={handleRefresh}>새로고침</button>
                        <button
                          onClick={async () => {
                            try {
                              const response = await axios.get('http://localhost:80/data/api/movie-status-counts');
                              alert('영화 상태별 개수: ' + JSON.stringify(response.data, null, 2));
                            } catch (err) {
                              alert('디버깅 정보 조회 실패: ' + err.message);
                            }
                          }}
                          style={{
                            padding: '10px 20px',
                            backgroundColor: '#28a745',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: 'pointer',
                            fontSize: '14px'
                          }}
                        >
                          🔍 MovieDetail 디버깅
                        </button>
                      </div>
                      <div className="movie-grid">
                        {comingSoonData.data && comingSoonData.data.length > 0 ? (
                          comingSoonData.data.map((movie, index) => (
                            <div key={index} className="movie-card" style={{cursor: 'pointer'}} onClick={() => handleMovieClick(movie)}>
                              <div className="movie-poster">
                                {movie.posterUrl ? (
                                  <img src={movie.posterUrl} alt={movie.movieNm} />
                                ) : (
                                  <div className="no-poster">No Poster</div>
                                )}
                              </div>
                              <div className="movie-info">
                                <h3>{movie.movieNm}</h3>
                                <p className="movie-title-en">{movie.movieNmEn || '-'}</p>
                                <div className="movie-details">
                                  <p><strong>개봉일:</strong> {movie.openDt || '-'}</p>
                                  <p><strong>장르:</strong> {movie.genreNm || '-'}</p>
                                  <p><strong>제작국가:</strong> {movie.nationNm || '-'}</p>
                                  <p><strong>관람등급:</strong> {movie.watchGradeNm || '-'}</p>
                                  <p><strong>상태:</strong> {movie.status || '-'}</p>
                                </div>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
                            {loading ? '데이터를 불러오는 중...' : '개봉예정작이 없습니다.'}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                  {activeTab === 'now-playing' && (
                    <div>
                      <div className="movie-grid">
                        {nowPlayingData.data && nowPlayingData.data.length > 0 ? (
                          nowPlayingData.data.map((movie, index) => (
                            <div key={index} className="movie-card" style={{cursor: 'pointer'}} onClick={() => handleMovieClick(movie)}>
                              <div className="movie-poster">
                                {movie.posterUrl ? (
                                  <img src={movie.posterUrl} alt={movie.movieNm} />
                                ) : (
                                  <div className="no-poster">No Poster</div>
                                )}
                              </div>
                              <div className="movie-info">
                                <h3>{movie.movieNm}</h3>
                                <p className="movie-title-en">{movie.movieNmEn || '-'}</p>
                                <div className="movie-details">
                                  <p><strong>개봉일:</strong> {movie.openDt || '-'}</p>
                                  <p><strong>장르:</strong> {movie.genreNm || '-'}</p>
                                  <p><strong>제작국가:</strong> {movie.nationNm || '-'}</p>
                                  <p><strong>관람등급:</strong> {movie.watchGradeNm || '-'}</p>
                                  <p><strong>상태:</strong> {movie.status || '-'}</p>
                                </div>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
                            {loading ? '데이터를 불러오는 중...' : '개봉중인 영화가 없습니다.'}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                  {activeTab === 'ended' && (
                    <div>
                      <div className="movie-grid">
                        {endedData.data && endedData.data.length > 0 ? (
                          endedData.data.map((movie, index) => (
                            <div key={index} className="movie-card" style={{cursor: 'pointer'}} onClick={() => handleMovieClick(movie)}>
                              <div className="movie-poster">
                                {movie.posterUrl ? (
                                  <img src={movie.posterUrl} alt={movie.movieNm} />
                                ) : (
                                  <div className="no-poster">No Poster</div>
                                )}
                              </div>
                              <div className="movie-info">
                                <h3>{movie.movieNm}</h3>
                                <p className="movie-title-en">{movie.movieNmEn || '-'}</p>
                                <div className="movie-details">
                                  <p><strong>개봉일:</strong> {movie.openDt || '-'}</p>
                                  <p><strong>장르:</strong> {movie.genreNm || '-'}</p>
                                  <p><strong>제작국가:</strong> {movie.nationNm || '-'}</p>
                                  <p><strong>관람등급:</strong> {movie.watchGradeNm || '-'}</p>
                                  <p><strong>상태:</strong> {movie.status || '-'}</p>
                                </div>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div style={{textAlign: 'center', padding: '20px', gridColumn: '1 / -1'}}>
                            {loading ? '데이터를 불러오는 중...' : '상영종료 영화가 없습니다.'}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>

          {renderMovieDetailModal()}
          {renderMovieForm()}
        </>
      )}
    </div>
  );
}

export default App; 