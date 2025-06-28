import React, { useState, useEffect } from 'react';
import axios from 'axios';
import Login from './Login';
import Signup from './Signup';
import './App.css';

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
  
  // 로그인/회원가입 상태
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [showLogin, setShowLogin] = useState(true);
  const [showAuth, setShowAuth] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);
  
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

  // 검색 관련 상태
  const [searchQuery, setSearchQuery] = useState('');

  // API 기본 URL
  const API_BASE_URL = '/api';

  // 로그인 상태 확인
  useEffect(() => {
    checkLoginStatus();
  }, []);

  // URL에서 탭 읽어오기
  useEffect(() => {
    const path = window.location.pathname;
    const searchParams = new URLSearchParams(window.location.search);
    const tabFromUrl = path.substring(1); // '/' 제거
    const pageFromUrl = searchParams.get('page');
    
    if (tabFromUrl && tabFromUrl !== '') {
      setActiveTab(tabFromUrl);
      
      // 페이지 번호가 있으면 해당 페이지 데이터 로드
      if (pageFromUrl) {
        const page = parseInt(pageFromUrl) - 1; // URL은 1부터, 내부는 0부터
        if (!isNaN(page) && page >= 0) {
          // 탭에 따라 해당 데이터 로드
          if (tabFromUrl === 'movie-list') fetchMovieList(page);
          else if (tabFromUrl === 'movie-detail') fetchMovieDetail(page);
          else if (tabFromUrl === 'box-office') fetchBoxOffice(page);
          else if (tabFromUrl === 'box-office-dto') fetchBoxOfficeDto(page);
          else if (tabFromUrl === 'movie-detail-dto') fetchMovieDetailDto(page);
          else if (tabFromUrl === 'movie-list-dto') fetchMovieListDto(page);
          else if (tabFromUrl === 'coming-soon') fetchComingSoon(page);
          else if (tabFromUrl === 'now-playing') fetchNowPlaying(page);
          else if (tabFromUrl === 'ended') fetchEnded(page);
        }
      }
    }
  }, []);

  const checkLoginStatus = async () => {
    try {
      const response = await axios.get('http://localhost:80/api/current-user', {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      console.log('=== 로그인 상태 확인 ===');
      console.log('API 응답:', response.data);
      if (response.data.success) {
        setIsLoggedIn(true);
        setCurrentUser(response.data.user);
        setIsAdmin(response.data.user.isAdmin);
        console.log('사용자 정보:', response.data.user);
        console.log('isAdmin 값:', response.data.user.isAdmin);
        console.log('role 값:', response.data.user.role);
        setShowAuth(false);
      }
    } catch (err) {
      console.log('로그인되지 않은 상태');
    }
  };

  const handleLoginSuccess = (user) => {
    setIsLoggedIn(true);
    setCurrentUser(user);
    setIsAdmin(user.isAdmin);
    setShowAuth(false);
  };

  const handleSignupSuccess = (data) => {
    alert('회원가입이 완료되었습니다! 로그인해주세요.');
    setShowLogin(true);
  };

  const handleLogout = async () => {
    try {
      await axios.post('http://localhost:80/api/logout', {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      setIsLoggedIn(false);
      setCurrentUser(null);
      setIsAdmin(false);
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
    } catch (err) {
      setError('통계 데이터를 불러오는데 실패했습니다.');
    }
  };

  const fetchMovieList = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-list?page=${page}&size=20`);
      setMovieListData(response.data);
    } catch (err) {
      console.error('MovieList API Error:', err);
      setError('MovieList 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
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
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-detail?page=${page}&size=20`);
      console.log('MovieDetail API Response:', response.data);
      setMovieDetailData(response.data);
    } catch (err) {
      console.error('MovieDetail API Error:', err);
      setError('MovieDetail 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
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

  const fetchBoxOffice = async (page = 0) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`http://localhost:80/data/api/box-office?page=${page}&size=20`);
      console.log('BoxOffice API Response:', response.data);
      setBoxOfficeData(response.data);
    } catch (err) {
      console.error('BoxOffice API Error:', err);
      setError('BoxOffice 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
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
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get('http://localhost:80/data/api/ratings/top-rated?limit=10');
      console.log('Top Rated API Response:', response.data);
      setTopRatedData(response.data);
    } catch (err) {
      console.error('Top Rated API Error:', err);
      setError('Top Rated 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const fetchPopularMovies = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get('http://localhost:80/data/api/popular-movies?limit=100');
      console.log('Popular Movies API Response:', response.data);
      setPopularMoviesData(response.data);
    } catch (err) {
      console.error('Popular Movies API Error:', err);
      setError('Popular Movies 데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
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
      const response = await axios.post('http://localhost:80/api/admin/ratings/fetch-tmdb', {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      console.log('TMDB 평점 가져오기 응답:', response.data);
      alert('TMDB 평점 데이터를 성공적으로 가져왔습니다.');
      handleRefresh();
    } catch (error) {
      console.error('TMDB 평점 가져오기 실패:', error);
      alert('TMDB 평점 가져오기에 실패했습니다: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleFetchBoxOfficeData = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/boxoffice/daily', {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      console.log('박스오피스 데이터 가져오기 응답:', response.data);
      alert('박스오피스 데이터를 성공적으로 가져왔습니다.');
      handleRefresh();
    } catch (error) {
      console.error('박스오피스 데이터 가져오기 실패:', error);
      alert('박스오피스 데이터 가져오기에 실패했습니다: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleReplaceWithPopularMovies = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/movies/replace-with-popular', {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      console.log('인기 영화로 교체 응답:', response.data);
      alert('인기 영화 100개로 성공적으로 교체되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('인기 영화로 교체 실패:', error);
      alert('인기 영화로 교체에 실패했습니다: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleUpdateCharacterNames = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/movies/update-character-names', {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      console.log('캐릭터명 업데이트 응답:', response.data);
      alert('캐릭터명이 성공적으로 업데이트되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('캐릭터명 업데이트 실패:', error);
      alert('캐릭터명 업데이트에 실패했습니다: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleFetchPosterUrlsFromTmdb = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/posters/fetch-tmdb', {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      console.log('TMDB 포스터 URL 가져오기 응답:', response.data);
      alert('TMDB 포스터 URL을 성공적으로 가져왔습니다.');
      handleRefresh();
    } catch (error) {
      console.error('TMDB 포스터 URL 가져오기 실패:', error);
      alert('TMDB 포스터 URL 가져오기에 실패했습니다: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleFetchPosterUrlsFromNaver = async () => {
    try {
      const response = await axios.post('http://localhost:80/api/admin/posters/fetch-naver', {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      console.log('네이버 포스터 URL 가져오기 응답:', response.data);
      alert('네이버 포스터 URL을 성공적으로 가져왔습니다.');
      handleRefresh();
    } catch (error) {
      console.error('네이버 포스터 URL 가져오기 실패:', error);
      alert('네이버 포스터 URL 가져오기에 실패했습니다: ' + (error.response?.data?.message || error.message));
    }
  };

  // 영화 관리 기능들
  const handleMovieClick = (movie) => {
    setSelectedMovie(movie);
    setShowMovieDetail(true);
    setShowMovieForm(false);
  };

  const handleEditMovie = (movie) => {
    if (!isAdmin) {
      alert('관리자만 영화를 수정할 수 있습니다.');
      return;
    }
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
    if (!isAdmin) {
      alert('관리자만 영화를 등록할 수 있습니다.');
      return;
    }
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
        await axios.delete(`http://localhost:80/api/movies/${movieCd}`, {
          withCredentials: true,
          headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0'
          }
        });
        alert('영화가 삭제되었습니다.');
        handleRefresh();
      } catch (error) {
        console.error('영화 삭제 실패:', error);
        alert('영화 삭제에 실패했습니다.');
      }
    }
  };

  const handleSaveMovie = async () => {
    console.log('=== handleSaveMovie 함수 호출됨 ===');
    console.log('현재 movieForm 데이터:', movieForm);
    
    try {
      // 프론트엔드 데이터 검증
      const validationErrors = validateMovieForm(movieForm);
      if (validationErrors.length > 0) {
        console.log('검증 오류:', validationErrors);
        alert('입력 데이터 오류:\n' + validationErrors.join('\n'));
        return;
      }

      console.log('검증 통과, API 호출 시작...');

      // 백엔드 DTO 구조에 맞게 데이터 변환
      const movieData = {
        movieNm: movieForm.movieNm,
        movieNmEn: movieForm.movieNmEn,
        description: movieForm.description,
        companyNm: movieForm.companyNm,
        openDt: movieForm.openDt ? movieForm.openDt : null,
        showTm: movieForm.showTm ? parseInt(movieForm.showTm) : 0,
        genreNm: movieForm.genreNm,
        nationNm: movieForm.nationNm,
        watchGradeNm: movieForm.watchGradeNm,
        prdtYear: movieForm.prdtYear,
        prdtStatNm: movieForm.prdtStatNm,
        typeNm: movieForm.typeNm,
        totalAudience: movieForm.totalAudience,
        reservationRate: movieForm.reservationRate,
        averageRating: movieForm.averageRating,
        // 감독 정보 변환
        directors: movieForm.directorName ? [{
          peopleNm: movieForm.directorName,
          peopleNmEn: ''
        }] : [],
        // 배우 정보 변환
        actors: movieForm.actors ? movieForm.actors.split(',').map(actor => ({
          peopleNm: actor.trim(),
          peopleNmEn: '',
          cast: '',
          castEn: ''
        })) : []
      };

      console.log('변환된 movieData:', movieData);

      if (editingMovie) {
        const updateUrl = `http://localhost:80/api/movies/${editingMovie.movieCd}`;
        console.log('영화 수정 API 호출 URL:', updateUrl);
        const response = await axios.put(updateUrl, movieData, {
          withCredentials: true,
          headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0',
            'Content-Type': 'application/json'
          }
        });
        console.log('영화 수정 응답:', response.data);
        alert('영화가 수정되었습니다.');
      } else {
        const createUrl = `http://localhost:80/api/movies`;
        console.log('영화 등록 API 호출 URL:', createUrl);
        const response = await axios.post(createUrl, movieData, {
          withCredentials: true,
          headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0',
            'Content-Type': 'application/json'
          }
        });
        console.log('영화 등록 응답:', response.data);
        alert('영화가 등록되었습니다.');
      }
      setShowMovieForm(false);
      handleRefresh();
    } catch (error) {
      console.error('영화 저장 실패:', error);
      console.error('에러 상세 정보:', {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        config: {
          url: error.config?.url,
          method: error.config?.method,
          withCredentials: error.config?.withCredentials
        }
      });
      
      if (error.response && error.response.data && error.response.data.message) {
        alert('영화 저장에 실패했습니다:\n' + error.response.data.message);
      } else if (error.code === 'ERR_NETWORK') {
        alert('네트워크 오류: Spring Boot 서버가 실행되지 않았거나 포트가 다릅니다.\n\n확인사항:\n1. Spring Boot 서버가 80번 포트에서 실행 중인지 확인\n2. 브라우저에서 http://localhost:80 접속 가능한지 확인');
      } else {
        alert('영화 저장에 실패했습니다: ' + error.message);
      }
    }
  };

  // 영화 폼 데이터 검증
  const validateMovieForm = (form) => {
    const errors = [];
    
    // 필수 필드 검증
    if (!form.movieNm || form.movieNm.trim() === '') {
      errors.push('• 영화 제목(한글)은 필수입니다.');
    }
    
    // 숫자 필드 검증
    if (form.showTm && (isNaN(form.showTm) || parseInt(form.showTm) < 0)) {
      errors.push('• 상영시간은 0 이상의 숫자여야 합니다.');
    }
    
    if (form.totalAudience && (isNaN(form.totalAudience) || parseInt(form.totalAudience) < 0)) {
      errors.push('• 누적 관객수는 0 이상의 숫자여야 합니다.');
    }
    
    if (form.reservationRate && (isNaN(form.reservationRate) || parseFloat(form.reservationRate) < 0 || parseFloat(form.reservationRate) > 100)) {
      errors.push('• 예매율은 0~100 사이의 숫자여야 합니다.');
    }
    
    if (form.averageRating && (isNaN(form.averageRating) || parseFloat(form.averageRating) < 0 || parseFloat(form.averageRating) > 10)) {
      errors.push('• 평균 평점은 0~10 사이의 숫자여야 합니다.');
    }
    
    // 제작연도 검증
    if (form.prdtYear && form.prdtYear.trim() !== '') {
      const year = parseInt(form.prdtYear);
      if (isNaN(year) || year < 1900 || year > new Date().getFullYear() + 10) {
        errors.push('• 제작연도는 1900년부터 현재로부터 10년 이내여야 합니다.');
      }
    }
    
    // 날짜 형식 검증
    if (form.openDt && form.openDt.trim() !== '') {
      const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
      if (!dateRegex.test(form.openDt)) {
        errors.push('• 개봉일은 YYYY-MM-DD 형식이어야 합니다. (예: 2024-12-25)');
      } else {
        const selectedDate = new Date(form.openDt);
        const currentDate = new Date();
        const futureDate = new Date();
        futureDate.setFullYear(currentDate.getFullYear() + 10);
        
        if (selectedDate > futureDate) {
          errors.push('• 개봉일은 현재로부터 10년 이내여야 합니다.');
        }
      }
    }
    
    // 문자열 길이 검증
    if (form.movieNm && form.movieNm.length > 200) {
      errors.push('• 영화 제목(한글)은 200자 이하여야 합니다.');
    }
    
    if (form.movieNmEn && form.movieNmEn.length > 200) {
      errors.push('• 영화 제목(영문)은 200자 이하여야 합니다.');
    }
    
    if (form.description && form.description.length > 4000) {
      errors.push('• 영화 설명은 4000자 이하여야 합니다.');
    }
    
    if (form.genreNm && form.genreNm.length > 100) {
      errors.push('• 장르는 100자 이하여야 합니다.');
    }
    
    if (form.nationNm && form.nationNm.length > 50) {
      errors.push('• 제작국가는 50자 이하여야 합니다.');
    }
    
    if (form.companyNm && form.companyNm.length > 100) {
      errors.push('• 배급사는 100자 이하여야 합니다.');
    }
    
    return errors;
  };

  const handleLikeMovie = async (movieCd) => {
    try {
      await axios.post(`http://localhost:80/api/movies/${movieCd}/like`, {}, {
        withCredentials: true,
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      });
      alert('좋아요가 추가되었습니다.');
      handleRefresh();
    } catch (error) {
      console.error('좋아요 실패:', error);
      alert('좋아요에 실패했습니다.');
    }
  };

  // 검색 관련 함수들
  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      alert('검색어를 입력해주세요.');
      return;
    }
    
    setLoading(true);
    try {
      const response = await axios.get(`http://localhost:80/data/api/movie-detail-dto/search?keyword=${encodeURIComponent(searchQuery.trim())}&page=0&size=20`);
      setMovieDetailDtoData(response.data);
      // URL 업데이트
      const newUrl = `http://localhost:3000/movie-detail-dto?search=${encodeURIComponent(searchQuery.trim())}&page=1`;
      window.history.pushState({ tab: 'movie-detail-dto', search: searchQuery.trim(), page: 1 }, '', newUrl);
    } catch (error) {
      console.error('검색 실패:', error);
      alert('검색에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleClearSearch = async () => {
    setSearchQuery('');
    setLoading(true);
    try {
      await fetchMovieDetailDto(0);
      // URL 업데이트
      const newUrl = `http://localhost:3000/movie-detail-dto?page=1`;
      window.history.pushState({ tab: 'movie-detail-dto', page: 1 }, '', newUrl);
    } catch (error) {
      console.error('검색 초기화 실패:', error);
    } finally {
      setLoading(false);
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
    setError(null);
    
    // URL 변경
    const newUrl = `http://localhost:3000/${tab}`;
    window.history.pushState({ tab }, '', newUrl);
    
    // 페이지 제목도 변경
    const tabNames = {
      'stats': '',
      'movie-list': '영화 목록',
      'movie-detail': '영화 상세',
      'box-office': '박스오피스',
      'box-office-dto': '박스오피스 DTO',
      'movie-detail-dto': '영화 상세 DTO',
      'movie-list-dto': '영화 목록 DTO',
      'topRated': '평점 높은 영화',
      'popular-movies': '인기 영화',
      'coming-soon': '개봉예정작',
      'now-playing': '개봉중인 영화',
      'ended': '상영종료된 영화'
    };
    
    document.title = `영화 관리 시스템 - ${tabNames[tab] || tab}`;
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
      
      <div className="admin-actions">
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
            <div key={index} className="movie-card" onClick={() => handleMovieClick(item)}>
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
      {isAdmin && (
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
      )}
      
      {/* 검색 기능 추가 */}
      <div style={{marginBottom: '20px', textAlign: 'center'}}>
        <input
          type="text"
          placeholder="영화 제목으로 검색..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
          style={{
            padding: '8px 12px',
            border: '1px solid #ddd',
            borderRadius: '4px',
            marginRight: '10px',
            width: '300px',
            fontSize: '14px'
          }}
        />
        <button
          onClick={handleSearch}
          style={{
            padding: '8px 16px',
            backgroundColor: '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '14px',
            marginRight: '10px'
          }}
        >
          🔍 검색
        </button>
        <button
          onClick={handleClearSearch}
          style={{
            padding: '8px 16px',
            backgroundColor: '#6c757d',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '14px'
          }}
        >
          초기화
        </button>
      </div>

      <div className="movie-grid">
        {movieDetailDtoData.data && movieDetailDtoData.data.length > 0 ? (
          movieDetailDtoData.data.map((item, index) => (
            <div key={index} className="movie-card" onClick={() => handleMovieClick(item)}>
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
                  {isAdmin && (
                    <>
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
                    </>
                  )}
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
            {loading ? '데이터를 불러오는 중...' : '데이터가 없습니다.'}
          </div>
        )}
      </div>
      {movieDetailDtoData.data && movieDetailDtoData.data.length > 0 && (
        <div className="pagination">
          <button 
            onClick={() => {
              const newPage = movieDetailDtoData.page - 1;
              fetchMovieDetailDto(newPage);
              // URL 업데이트 (1부터 시작하는 페이지 번호)
              const newUrl = `http://localhost:3000/movie-detail-dto?page=${newPage + 1}`;
              window.history.pushState({ tab: 'movie-detail-dto', page: newPage + 1 }, '', newUrl);
            }}
            disabled={movieDetailDtoData.page === 0}
          >
            이전
          </button>
          <span>페이지 {movieDetailDtoData.page + 1} / {movieDetailDtoData.totalPages}</span>
          <button 
            onClick={() => {
              const newPage = movieDetailDtoData.page + 1;
              fetchMovieDetailDto(newPage);
              // URL 업데이트 (1부터 시작하는 페이지 번호)
              const newUrl = `http://localhost:3000/movie-detail-dto?page=${newPage + 1}`;
              window.history.pushState({ tab: 'movie-detail-dto', page: newPage + 1 }, '', newUrl);
            }}
            disabled={movieDetailDtoData.page >= movieDetailDtoData.totalPages - 1}
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
            onClick={() => {
              const newPage = movieListDtoData.page - 1;
              fetchMovieListDto(newPage);
              // URL 업데이트 (1부터 시작하는 페이지 번호)
              const newUrl = `http://localhost:3000/movie-list-dto?page=${newPage + 1}`;
              window.history.pushState({ tab: 'movie-list-dto', page: newPage + 1 }, '', newUrl);
            }}
            disabled={movieListDtoData.page === 0}
          >
            이전
          </button>
          <span>페이지 {movieListDtoData.page + 1} / {movieListDtoData.totalPages}</span>
          <button 
            onClick={() => {
              const newPage = movieListDtoData.page + 1;
              fetchMovieListDto(newPage);
              // URL 업데이트 (1부터 시작하는 페이지 번호)
              const newUrl = `http://localhost:3000/movie-list-dto?page=${newPage + 1}`;
              window.history.pushState({ tab: 'movie-list-dto', page: newPage + 1 }, '', newUrl);
            }}
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
    if (!showMovieForm || !isAdmin) return null;

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

  return (
    <div className="App">
      {showAuth ? (
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
                          const response = await axios.get('/data/api/movie-status-counts');
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
                        <div key={index} className="movie-card" onClick={() => handleMovieClick(movie)}>
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
                              {movie.directorName && (
                                <p><strong>감독:</strong> {movie.directorName}</p>
                              )}
                              {movie.showTm > 0 && (
                                <p><strong>상영시간:</strong> {movie.showTm}분</p>
                              )}
                              {movie.companyNm && (
                                <p><strong>제작사:</strong> {movie.companyNm}</p>
                              )}
                              {movie.averageRating > 0 && (
                                <p><strong>평점:</strong> ⭐ {movie.averageRating.toFixed(1)}</p>
                              )}
                              {movie.description && (
                                <div className="movie-description">
                                  <p><strong>줄거리:</strong></p>
                                  <p>{movie.description.length > 100 ? 
                                    movie.description.substring(0, 100) + '...' : 
                                    movie.description}</p>
                                </div>
                              )}
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
                  {comingSoonData.data && comingSoonData.data.length > 0 && (
                    <div className="pagination">
                      <button 
                        onClick={() => fetchComingSoon(comingSoonData.page - 1)}
                        disabled={comingSoonData.page === 0}
                      >
                        이전
                      </button>
                      <span>페이지 {comingSoonData.page + 1} / {comingSoonData.totalPages}</span>
                      <button 
                        onClick={() => fetchComingSoon(comingSoonData.page + 1)}
                        disabled={comingSoonData.page >= comingSoonData.totalPages - 1}
                      >
                        다음
                      </button>
                    </div>
                  )}
                </div>
              )}
              {activeTab === 'now-playing' && (
                <div>
                  <div className="movie-grid">
                    {nowPlayingData.data && nowPlayingData.data.length > 0 ? (
                      nowPlayingData.data.map((movie, index) => (
                        <div key={index} className="movie-card" onClick={() => handleMovieClick(movie)}>
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
                  {nowPlayingData.data && nowPlayingData.data.length > 0 && (
                    <div className="pagination">
                      <button 
                        onClick={() => fetchNowPlaying(nowPlayingData.page - 1)}
                        disabled={nowPlayingData.page === 0}
                      >
                        이전
                      </button>
                      <span>페이지 {nowPlayingData.page + 1} / {nowPlayingData.totalPages}</span>
                      <button 
                        onClick={() => fetchNowPlaying(nowPlayingData.page + 1)}
                        disabled={nowPlayingData.page >= nowPlayingData.totalPages - 1}
                      >
                        다음
                      </button>
                    </div>
                  )}
                </div>
              )}
              {activeTab === 'ended' && (
                <div>
                  <div className="movie-grid">
                    {endedData.data && endedData.data.length > 0 ? (
                      endedData.data.map((movie, index) => (
                        <div key={index} className="movie-card" onClick={() => handleMovieClick(movie)}>
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
                        {loading ? '데이터를 불러오는 중...' : '상영종료된 영화가 없습니다.'}
                      </div>
                    )}
                  </div>
                  {endedData.data && endedData.data.length > 0 && (
                    <div className="pagination">
                      <button 
                        onClick={() => fetchEnded(endedData.page - 1)}
                        disabled={endedData.page === 0}
                      >
                        이전
                      </button>
                      <span>페이지 {endedData.page + 1} / {endedData.totalPages}</span>
                      <button 
                        onClick={() => fetchEnded(endedData.page + 1)}
                        disabled={endedData.page >= endedData.totalPages - 1}
                      >
                        다음
                      </button>
                    </div>
                  )}
                </div>
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