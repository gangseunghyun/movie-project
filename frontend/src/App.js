import React, { useState, useEffect } from 'react';
import axios from 'axios';
import Login from './Login';
import Signup from './Signup';
import SocialJoin from './SocialJoin';
import './App.css';
import { safeFetch } from './api';
import { Routes, Route, useNavigate } from 'react-router-dom';
import UserSearch from './UserSearch';
import UserPage from './UserPage';
import { userSearch } from './services/userService';
import MainPage from './MainPage';
import FindId from './FindId';
import FindPassword from './FindPassword';
import ResetPassword from './ResetPassword';
import StarRating from './StarRating';
import RatingDistributionChart from './components/RatingDistributionChart';
import PersonDetail from './PersonDetail';
import BookingModal from './BookingModal';
import ReviewModal from './components/ReviewModal';
import ReviewList from './components/ReviewList';
import UserReservations from './UserReservations';
import ReservationReceipt from './ReservationReceipt';

// axios 기본 설정 - baseURL 제거하고 절대 경로 사용
axios.defaults.withCredentials = true;

// API 기본 URL
const API_BASE_URL = 'http://localhost:80/api';

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showLogin, setShowLogin] = useState(false);
  const [showSignup, setShowSignup] = useState(false);
  const [showSocialJoin, setShowSocialJoin] = useState(false);
  const [socialUserInfo, setSocialUserInfo] = useState(null);
  const [activeTab, setActiveTab] = useState('movieList');
  const [activeMenu, setActiveMenu] = useState('메인 페이지');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [recentKeywords, setRecentKeywords] = useState([]);
  const [popularKeywords, setPopularKeywords] = useState([]);
  const [showMovieDetail, setShowMovieDetail] = useState(false);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [showMovieForm, setShowMovieForm] = useState(false);
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [editingMovie, setEditingMovie] = useState(null);
  const [movieForm, setMovieForm] = useState({
    movieNm: '',
    movieNmEn: '',
    description: '',
    directorName: '',
    actorNames: '',
    genreNm: '',
    openDt: '',
    showTm: '',
    nationNm: '',
    watchGradeNm: '',
    companyNm: '',
    posterUrl: '',
    stillcutUrls: '',
    tags: '',
    prdtYear: '',
    prdtStatNm: '',
    typeNm: ''
  });
  const [stats, setStats] = useState({
    totalMovies: 0,
    nowPlaying: 0,
    comingSoon: 0,
    ended: 0
  });
  const [movieList, setMovieList] = useState([]);
  const [movieDetail, setMovieDetail] = useState([]);
  const [boxOffice, setBoxOffice] = useState([]);
  const [boxOfficeDto, setBoxOfficeDto] = useState([]);
  const [movieListDto, setMovieListDto] = useState([]);
  const [movieDetailDto, setMovieDetailDto] = useState([]);
  const [topRated, setTopRated] = useState([]);
  const [popularMovies, setPopularMovies] = useState([]);
  const [comingSoon, setComingSoon] = useState([]);
  const [nowPlaying, setNowPlaying] = useState([]);
  const [ended, setEnded] = useState([]);
  const [tmdbRatings, setTmdbRatings] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // 별점 관련 상태 추가
  const [userRating, setUserRating] = useState(null);
  const [averageRating, setAverageRating] = useState(null);
  const [ratingCount, setRatingCount] = useState(null);
  const [loadingRating, setLoadingRating] = useState(false);

  // 1. 정렬 옵션 추가
  const [sortOption, setSortOption] = useState('rating');

  // 추가로 필요한 상태들 선언
  const [showAuth, setShowAuth] = useState(false);
  const [searchExecuted, setSearchExecuted] = useState(false);
  const [userResults, setUserResults] = useState([]);
  const [personResults, setPersonResults] = useState({ actors: [], directors: [] });
  
  // 데이터 관련 상태들 (기존 구조 유지)
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
  const [recommendedMoviesData, setRecommendedMoviesData] = useState([]);
  const [activeRecommendedTab, setActiveRecommendedTab] = useState('recommended');
  const [actorRecommendation, setActorRecommendation] = useState(null);
  const [directorRecommendation, setDirectorRecommendation] = useState(null);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewListKey, setReviewListKey] = useState(0); // 리뷰 목록 강제 새로고침용
  const [openTagCards, setOpenTagCards] = useState([]); // 태그 펼침 상태 관리

  // 소셜 친구 추천 상태 추가
  const [socialRecommendation, setSocialRecommendation] = useState(null);
  const [socialRecommendationLoading, setSocialRecommendationLoading] = useState(false);

  // 새로운 장르 추천 상태
  const [newGenreRecommendation, setNewGenreRecommendation] = useState(null);

  // 소셜 추천 fetch 함수 최상단에 선언
  const fetchSocialRecommendation = async () => {
    if (!currentUser || !currentUser.id) return;
    setSocialRecommendationLoading(true);
    try {
      const response = await axios.get(`http://localhost:80/api/users/${currentUser.id}/daily-social-recommendation`, { withCredentials: true });
      if (response.data.success) {
        setSocialRecommendation(response.data);
      }
    } catch (error) {
      console.error('소셜 추천 정보 조회 실패:', error);
    } finally {
      setSocialRecommendationLoading(false);
    }
  };

  useEffect(() => {
    if (!currentUser || !currentUser.id) return;
    fetchSocialRecommendation();
  }, [currentUser?.id]);

  useEffect(() => {
    if (typeof recommendedMoviesData === 'object' && !Array.isArray(recommendedMoviesData)) {
      const tagNames = Object.keys(recommendedMoviesData);
      if (tagNames.length > 0) {
        setActiveRecommendedTab(tagNames[0]);
      }
    }
  }, [recommendedMoviesData]);

  // 로그인 상태 확인
  useEffect(() => {
    console.log('배우 추천 useEffect 실행!');
    checkLoginStatus();
    
    // URL 경로 확인하여 social-join 페이지 표시
    if (window.location.pathname === '/social-join') {
      setShowSocialJoin(true);
      setShowAuth(false);
    }
    
    // 인기 검색어 불러오기 (로그인 상태와 관계없이)
    fetchPopularKeywords();
    
    // 배우 추천 정보 가져오기
    fetchActorRecommendation();
    // 감독 추천 정보 가져오기
    fetchDirectorRecommendation();
    
    // 메인 페이지 데이터 불러오기
    fetchStats();
  }, []);

  // 5분마다 배우/감독 추천 정보 갱신
  useEffect(() => {
    const interval = setInterval(() => {
      fetchActorRecommendation();
      fetchDirectorRecommendation();
    }, 300000); // 5분
    return () => clearInterval(interval);
  }, []);

  // 정렬 옵션이 변경될 때마다 현재 활성 탭에 따라 데이터 다시 가져오기
  useEffect(() => {
    if (activeMenu === '영화 상세') {
      fetchMovieDetail(0);
    } else if (activeMenu === '영화 상세 DTO') {
      fetchMovieDetailDto(0);
    }
  }, [sortOption]);

  // 별점 정보 불러오기 함수들
  const fetchUserRating = async (movieCd) => {
    try {
      const res = await axios.get(`${API_BASE_URL}/ratings/${movieCd}`, { withCredentials: true });
      setUserRating(res.data.data?.score || null);
    } catch (error) {
      setUserRating(null);
    }
  };

  const fetchAverageRating = async (movieCd) => {
    try {
      const res = await axios.get(`${API_BASE_URL}/ratings/movie/${movieCd}/average`);
      setAverageRating(res.data.data?.averageRating || null);
      setRatingCount(res.data.data?.ratingCount || null);
    } catch (error) {
      setAverageRating(null);
      setRatingCount(null);
    }
  };

  const fetchRatingDistribution = async (movieCd) => {
    try {
      console.log('평점 분포 API 호출 시작:', movieCd);
      const res = await axios.get(`${API_BASE_URL}/ratings/movie/${movieCd}/distribution`);
      console.log('평점 분포 API 응답:', res.data);
      setRatingDistribution(res.data.data?.distribution || null);
    } catch (error) {
      console.error('평점 분포 API 실패:', error);
      setRatingDistribution(null);
    }
  };

  // 별점 정보 불러오기 (왓챠 스타일: Review 테이블 기준)
  useEffect(() => {
    if (!selectedMovie) return;
    setLoadingRating(true);
    
    const loadRatingData = async () => {
      await Promise.all([
        fetchUserRating(selectedMovie.movieCd),
        fetchAverageRating(selectedMovie.movieCd),
        fetchRatingDistribution(selectedMovie.movieCd)
      ]);
      setLoadingRating(false);
    };
    
    loadRatingData();
  }, [selectedMovie]);

  // 최근 검색어 불러오기
  const fetchRecentKeywords = async () => {
    if (!currentUser) {
      console.log('사용자가 로그인되지 않아 최근 검색어를 불러오지 않습니다.');
      return;
    }
    try {
      console.log('최근 검색어 불러오기 시작...');
      const res = await axios.get('http://localhost:80/api/search-history', { withCredentials: true });
      console.log('최근 검색어 API 응답:', res.data);
      if (Array.isArray(res.data)) {
        const keywords = res.data.map(item => item.keyword);
        setRecentKeywords(keywords);
        console.log('최근 검색어 설정 완료:', keywords);
      } else {
        console.log('최근 검색어 응답이 배열이 아님:', res.data);
        setRecentKeywords([]);
      }
    } catch (e) {
      console.error('최근 검색어 불러오기 실패:', e);
      setRecentKeywords([]);
    }
  };

  // 배우 추천 정보 가져오기
  const fetchActorRecommendation = async () => {
    console.log('배우 추천 API 호출 시도');
    try {
      const response = await axios.get('http://localhost:80/api/person/recommended-actor');
      console.log('배우 추천 API 응답:', response.data);
      if (response.data.success) {
        setActorRecommendation(response.data.data);
        console.log('배우 추천 데이터 설정 완료:', response.data.data);
      } else {
        console.log('배우 추천 API 응답이 실패:', response.data);
      }
    } catch (error) {
      console.error('배우 추천 정보 조회 실패:', error);
    }
  };

  // 감독 추천 정보 가져오기
  const fetchDirectorRecommendation = async () => {
    console.log('감독 추천 API 호출 시도');
    try {
      const response = await axios.get('http://localhost:80/api/person/recommended-director');
      console.log('감독 추천 API 응답:', response.data);
      if (response.data.success) {
        setDirectorRecommendation(response.data.data);
        console.log('감독 추천 데이터 설정 완료:', response.data.data);
      } else {
        console.log('감독 추천 API 응답이 실패:', response.data);
      }
    } catch (error) {
      console.error('감독 추천 정보 조회 실패:', error);
    }
  };

  // 인기 검색어 불러오기
  const fetchPopularKeywords = async () => {
    try {
      console.log('인기 검색어 불러오기 시작...');
      const res = await axios.get('http://localhost:80/api/popular-keywords');
      console.log('인기 검색어 API 응답:', res.data);
      if (Array.isArray(res.data)) {
        // PopularKeyword 엔티티 구조에 맞게 매핑
        setPopularKeywords(res.data.map(item => ({
          keyword: item.keyword,
          searchCount: item.searchCount
        })));
        console.log('인기 검색어 설정 완료:', res.data);
      } else {
        console.log('인기 검색어 응답이 배열이 아님:', res.data);
        setPopularKeywords([]);
      }
    } catch (e) {
      console.error('인기 검색어 불러오기 실패:', e);
      setPopularKeywords([]);
    }
  };

  // 최근 검색어 삭제 후 상태 업데이트
  const handleDeleteRecentKeyword = async (keyword) => {
    try {
      await axios.delete('http://localhost:80/api/search-history', { 
        params: { keyword }, 
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest'
        }
      });
      // 삭제 후 최근 검색어 다시 불러오기
      fetchRecentKeywords();
    } catch (err) {
      console.error('삭제 실패:', err);
      alert('삭제에 실패했습니다.');
    }
  };

  // 로그인 상태가 바뀌거나, 로그인 시 최근 검색어 불러오기
  useEffect(() => {
    if (currentUser) {
      fetchRecentKeywords();
    } else {
      setRecentKeywords([]);
    }
  }, [currentUser]);

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
    setIsLoggedIn(true);
    setCurrentUser(user);
    setShowAuth(false);
    // 검색 상태 초기화
    setSearchExecuted(false);
    setSearchKeyword('');
    setSearchResults({ data: [], total: 0, page: 0, totalPages: 0 });
  };

  const handleSignupSuccess = (data) => {
    alert('회원가입이 완료되었습니다! 로그인해주세요.');
    setShowLogin(true);
    // 검색 상태 초기화
    setSearchExecuted(false);
    setSearchKeyword('');
    setSearchResults({ data: [], total: 0, page: 0, totalPages: 0 });
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
      // 검색 상태 초기화
      setSearchExecuted(false);
      setSearchKeyword('');
      setSearchResults({ data: [], total: 0, page: 0, totalPages: 0 });
      
      // 인기검색어 캐시 무효화
      try {
        await axios.post('http://localhost:80/api/popular-keywords/clear-cache');
        console.log("인기검색어 캐시 무효화 완료");
      } catch (cacheError) {
        console.error("캐시 무효화 실패:", cacheError);
      }
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
      // 정렬 옵션에 따라 파라미터 설정
      let sortParam = 'date'; // 기본값: 개봉일순
      if (sortOption === 'rating') {
        sortParam = 'rating';
      } else if (sortOption === 'nameAsc') {
        sortParam = 'nameAsc';
      } else if (sortOption === 'nameDesc') {
        sortParam = 'nameDesc';
      }
      
      const response = await axios.get(`http://localhost:80/data/api/movie-detail?page=${page}&size=20&sort=${sortParam}`);
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
      // 정렬 옵션에 따라 파라미터 설정
      let sortParam = 'date'; // 기본값: 개봉일순
      if (sortOption === 'rating') {
        sortParam = 'rating';
      } else if (sortOption === 'nameAsc') {
        sortParam = 'nameAsc';
      } else if (sortOption === 'nameDesc') {
        sortParam = 'nameDesc';
      }
      
      const response = await axios.get(`http://localhost:80/data/api/movie-detail-dto?page=${page}&size=20&sort=${sortParam}`, {
        withCredentials: true
      });
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
      const response = await axios.get(`http://localhost:80/data/api/movie-detail-dto/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=20`, {
        withCredentials: true
      });
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
  const handleSearch = async (customKeyword) => {
    const keyword = String(customKeyword !== undefined ? customKeyword : searchKeyword || '').trim();

    if (!keyword) {
      setSearchResults({ data: [], total: 0, page: 0, totalPages: 0 });
      setUserResults([]);
      setPersonResults({ actors: [], directors: [] });
      setSearchExecuted(false);
      fetchMovieDetailDto(0);
      return;
    }

    setIsSearching(true);
    setError(null);
    setSearchExecuted(true);

    try {
      const movieRes = await safeFetch(`http://localhost:80/data/api/movie-detail-dto/search?keyword=${encodeURIComponent(keyword)}&page=0&size=20`);
      setSearchResults(movieRes);

      const userRes = await userSearch(keyword);
      setUserResults(userRes);

      const personRes = await axios.get(`http://localhost:80/data/api/search-person?keyword=${encodeURIComponent(keyword)}`);
      setPersonResults(personRes.data);

      // 검색어 저장 (로그인한 사용자만)
      if (currentUser) {
        try {
          const searchResultCount = movieRes.data ? movieRes.data.length : 0;
          await axios.post('http://localhost:80/api/search-history', null, {
            params: { 
              keyword: keyword,
              searchResultCount: searchResultCount
            },
            withCredentials: true
          });
          console.log('검색어 저장 완료:', keyword);
          
          // 최근 검색어 다시 불러오기
          fetchRecentKeywords();
        } catch (saveError) {
          console.error('검색어 저장 실패:', saveError);
          // 검색어 저장 실패해도 검색은 계속 진행
        }
      }
    } catch (err) {
      setError('검색 중 오류가 발생했습니다.');
    }
    setIsSearching(false);
  };

  // 검색어 초기화 핸들러
  const handleClearSearch = () => {
    setSearchKeyword('');
    setSearchResults({ data: [], total: 0, page: 0, totalPages: 0 });
    setSearchExecuted(false);
    setUserResults([]);
    setPersonResults({ actors: [], directors: [] });
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
    try {
      setLoading(true);
      const response = await safeFetch(`http://localhost:80/data/api/movies/ended?page=${page}&size=20`);
      if (response.success) {
        setEndedData({
          data: response.data.content,
          total: response.data.totalElements,
          page: response.data.number,
          totalPages: response.data.totalPages
        });
      }
    } catch (error) {
      console.error('상영종료 영화 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchRecommendedMovies = async () => {
    if (!currentUser) {
      console.log('로그인하지 않은 사용자는 추천 영화를 볼 수 없습니다.');
      return;
    }
    try {
      // 별도의 로딩 상태 사용 (전체 페이지 로딩에 영향 주지 않음)
      const response = await axios.get(`http://localhost:80/api/users/${currentUser.id}/recommended-movies`, { withCredentials: true });
      setRecommendedMoviesData(response.data);
      // 첫 번째 탭을 기본으로 설정
      if (typeof response.data === 'object' && !Array.isArray(response.data)) {
        const tagNames = Object.keys(response.data);
        if (tagNames.length > 0) {
          setActiveRecommendedTab(tagNames[0]);
        }
      }
    } catch (error) {
      console.error('추천 영화 조회 실패:', error);
      setRecommendedMoviesData([]);
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
  const handleMovieClick = async (movie) => {
    // 상세 정보가 이미 있으면 바로 모달 오픈
    if (movie.directors && movie.actors && movie.stillcuts) {
      setSelectedMovie(movie);
      setShowMovieDetail(true);
      return;
    }
    // 상세 정보 fetch
    try {
      const res = await axios.get(`http://localhost:80/data/api/movie-detail-dto?movieCd=${movie.movieCd}`);
      if (res.data && res.data.data && res.data.data.length > 0) {
        setSelectedMovie(res.data.data[0]);
        setShowMovieDetail(true);
      } else {
        alert('상세 정보를 불러올 수 없습니다.');
      }
    } catch (e) {
      alert('상세 정보 조회 실패');
    }
  };

  const handleEditMovie = (movie) => {
    setEditingMovie(movie);
    setMovieForm({
      movieNm: movie.movieNm || '',
      movieNmEn: movie.movieNmEn || '',
      description: movie.description || '',
      directorName: movie.directorName || '',
      actorNames: movie.actors || '',
      genreNm: movie.genreNm || '',
      openDt: movie.openDt || '',
      showTm: movie.showTm || '',
      nationNm: movie.nationNm || '',
      watchGradeNm: movie.watchGradeNm || '',
      companyNm: movie.companyNm || '',
      posterUrl: movie.posterUrl || '',
      stillcutUrls: movie.stillcuts ? movie.stillcuts.map(stillcut => stillcut.imageUrl).join(',') : '',
      tags: movie.tags || '',
      prdtYear: movie.prdtYear || '',
      prdtStatNm: movie.prdtStatNm || '',
      typeNm: movie.typeNm || ''
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
      actorNames: '',
      genreNm: '',
      openDt: '',
      showTm: '',
      nationNm: '',
      watchGradeNm: '',
      companyNm: '',
      posterUrl: '',
      stillcutUrls: '',
      tags: '',
      prdtYear: '',
      prdtStatNm: '',
      typeNm: ''
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
      let actorNamesStr = movieForm.actorNames;
      if (Array.isArray(actorNamesStr)) {
        actorNamesStr = actorNamesStr.join(',');
      }
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
        actors: actorNamesStr ? actorNamesStr.split(',').map(actor => ({
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

      // 찜 토글 상태 갱신 함수
  const updateMovieLikeState = (movieCd, liked) => {
    // movieDetailDtoData 업데이트
    setMovieDetailDtoData(prev => ({
      ...prev,
      data: prev.data.map(movie =>
        movie.movieCd === movieCd
          ? {
              ...movie,
              likedByMe: liked,
              likeCount: liked
                ? (movie.likeCount || 0) + 1
                : Math.max((movie.likeCount || 1) - 1, 0)
            }
          : movie
      )
    }));

    // topRatedData 업데이트
    setTopRatedData(prev => 
      prev ? prev.map(movie =>
        movie.movieCd === movieCd
          ? {
              ...movie,
              likedByMe: liked,
              likeCount: liked
                ? (movie.likeCount || 0) + 1
                : Math.max((movie.likeCount || 1) - 1, 0)
            }
          : movie
      ) : prev
    );

    // popularMoviesData 업데이트
    setPopularMoviesData(prev => 
      prev ? prev.map(movie =>
        movie.movieCd === movieCd
          ? {
              ...movie,
              likedByMe: liked,
              likeCount: liked
                ? (movie.likeCount || 0) + 1
                : Math.max((movie.likeCount || 1) - 1, 0)
            }
          : movie
      ) : prev
    );

    // comingSoonData 업데이트
    setComingSoonData(prev => ({
      ...prev,
      data: prev.data ? prev.data.map(movie =>
        movie.movieCd === movieCd
          ? {
              ...movie,
              likedByMe: liked,
              likeCount: liked
                ? (movie.likeCount || 0) + 1
                : Math.max((movie.likeCount || 1) - 1, 0)
            }
          : movie
      ) : prev.data
    }));

    // nowPlayingData 업데이트
    setNowPlayingData(prev => ({
      ...prev,
      data: prev.data ? prev.data.map(movie =>
        movie.movieCd === movieCd
          ? {
              ...movie,
              likedByMe: liked,
              likeCount: liked
                ? (movie.likeCount || 0) + 1
                : Math.max((movie.likeCount || 1) - 1, 0)
            }
          : movie
      ) : prev.data
    }));

    // endedData 업데이트
    setEndedData(prev => ({
      ...prev,
      data: prev.data ? prev.data.map(movie =>
        movie.movieCd === movieCd
          ? {
              ...movie,
              likedByMe: liked,
              likeCount: liked
                ? (movie.likeCount || 0) + 1
                : Math.max((movie.likeCount || 1) - 1, 0)
            }
          : movie
      ) : prev.data
    }));

    // searchResults 업데이트 (검색 결과가 있는 경우)
    setSearchResults(prev => ({
      ...prev,
      data: prev.data ? prev.data.map(movie =>
        movie.movieCd === movieCd
          ? {
              ...movie,
              likedByMe: liked,
              likeCount: liked
                ? (movie.likeCount || 0) + 1
                : Math.max((movie.likeCount || 1) - 1, 0)
            }
          : movie
      ) : prev.data
    }));
  };



  useEffect(() => {
    if (activeMenu === '메인 페이지') fetchStats();
    else if (activeMenu === '영화 목록') fetchMovieList();
    else if (activeMenu === '영화 상세') fetchMovieDetail();
    else if (activeMenu === '박스오피스') fetchBoxOffice();
    else if (activeMenu === '박스오피스 DTO') fetchBoxOfficeDto();
    else if (activeMenu === '영화 상세 DTO') fetchMovieDetailDto();
    else if (activeMenu === '영화 목록 DTO') fetchMovieListDto();
    else if (activeMenu === '평점 높은 영화') fetchTopRated();
    else if (activeMenu === '인기 영화') fetchPopularMovies();
    else if (activeMenu === '개봉예정작') fetchComingSoon();
    else if (activeMenu === '개봉중') fetchNowPlaying();
    else if (activeMenu === '상영종료') fetchEnded();
  }, [activeMenu]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSearchKeyword('');
    setSearchExecuted(false);
  };

  const handleRefresh = () => {
    if (activeMenu === '메인 페이지') fetchStats();
    else if (activeMenu === '영화 목록') fetchMovieList();
    else if (activeMenu === '영화 상세') fetchMovieDetail();
    else if (activeMenu === '박스오피스') fetchBoxOffice();
    else if (activeMenu === '박스오피스 DTO') fetchBoxOfficeDto();
    else if (activeMenu === '영화 상세 DTO') fetchMovieDetailDto();
    else if (activeMenu === '영화 목록 DTO') fetchMovieListDto();
    else if (activeMenu === '평점 높은 영화') fetchTopRated();
    else if (activeMenu === '인기 영화') fetchPopularMovies();
    else if (activeMenu === '개봉예정작') fetchComingSoon();
    else if (activeMenu === '개봉중') fetchNowPlaying();
    else if (activeMenu === '상영종료') fetchEnded();
  };

  const handleMenuChange = (menu) => {
    setActiveMenu(menu);
    setSearchExecuted(false);
    setSearchKeyword('');
    
    if (menu === '메인 페이지') {
      fetchStats();
    } else if (menu === '영화 목록') {
      fetchMovieList(0);
    } else if (menu === '영화 상세') {
      fetchMovieDetail(0);
    } else if (menu === '박스오피스') {
      fetchBoxOffice(0);
    } else if (menu === '박스오피스 DTO') {
      fetchBoxOfficeDto(0);
    } else if (menu === '영화 상세 DTO') {
      fetchMovieDetailDto(0);
    } else if (menu === '영화 목록 DTO') {
      fetchMovieListDto(0);
    } else if (menu === '평점 높은 영화') {
      fetchTopRated();
    } else if (menu === '인기 영화') {
      fetchPopularMovies();
    } else if (menu === '개봉예정작') {
      fetchComingSoon(0);
    } else if (menu === '개봉중') {
      fetchNowPlaying(0);
    } else if (menu === '상영종료') {
      fetchEnded(0);
    } else if (menu === '태그추천영화') {
      fetchRecommendedMovies();
    }
  };

  // 2. 정렬 함수 추가
  const getSortedResults = (data) => {
    if (!data) return [];
    const arr = [...data];
    if (sortOption === 'rating') {
      return arr.sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0));
    }
    if (sortOption === 'date') {
      return arr.sort((a, b) => (b.openDt || '').localeCompare(a.openDt || ''));
    }
    if (sortOption === 'nameDesc') {
      return arr.sort((a, b) => (b.movieNm || '').localeCompare(a.movieNm || ''));
    }
    if (sortOption === 'nameAsc') {
      return arr.sort((a, b) => (a.movieNm || '').localeCompare(b.movieNm || ''));
    }
    return arr;
  };

  const renderStats = () => (
    <div>
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
          <button onClick={generateTags} className="admin-button">
            🏷️ 태그 데이터 생성
          </button>
        </div>
      </div>
      )}

      {/* 추천 영역: 배우/감독 추천은 세로로 쌓고, 그 아래에 소셜 친구 추천을 세로로 따로 배치 */}
      <div style={{ maxWidth: '800px', margin: '30px auto 0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {/* 배우 추천 */}
        {actorRecommendation && (
          <div style={{ background: '#f8f9fa', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', padding: '20px' }}>
            <h3 style={{ marginBottom: '20px', color: '#333' }}>🎭 오늘의 배우 추천</h3>
            <div style={{ display: 'flex', gap: '20px' }}>
              {/* 배우 프로필 */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '150px' }}>
                <div style={{ width: '120px', height: '120px', borderRadius: '50%', overflow: 'hidden', marginBottom: '10px', backgroundColor: '#ddd', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {actorRecommendation.actor.photoUrl ? (
                    <img src={actorRecommendation.actor.photoUrl} alt={actorRecommendation.actor.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <span style={{ fontSize: '40px' }}>🎭</span>
                  )}
                </div>
                <h4 style={{ margin: '0 0 5px 0', textAlign: 'center' }}>{actorRecommendation.actor.name}</h4>
                <p style={{ margin: '0', fontSize: '14px', color: '#666', textAlign: 'center' }}>영화 {actorRecommendation.movieCount}개</p>
                <p style={{ margin: '5px 0 0 0', fontSize: '14px', color: '#666', textAlign: 'center' }}>평균 평점: {actorRecommendation.averageRating.toFixed(1)}⭐</p>
              </div>
              {/* 대표 작품 */}
              <div style={{ flex: 1 }}>
                <h5 style={{ margin: '0 0 15px 0', color: '#333' }}>대표 작품</h5>
                <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
                  {actorRecommendation.topMovies.map((movie, index) => (
                    <div key={movie.movieCd} style={{ width: '120px', cursor: 'pointer', transition: 'transform 0.2s' }} onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.05)'} onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'} onClick={() => handleMovieClick(movie)}>
                      <div style={{ width: '100%', height: '160px', borderRadius: '8px', overflow: 'hidden', backgroundColor: '#ddd', marginBottom: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        {movie.posterUrl ? (
                          <img src={movie.posterUrl} alt={movie.movieNm} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : (
                          <span style={{ fontSize: '24px' }}>🎬</span>
                        )}
                      </div>
                      <div>
                        <p style={{ margin: '0 0 5px 0', fontSize: '12px', fontWeight: 'bold', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{movie.movieNm}</p>
                        <p style={{ margin: '0', fontSize: '11px', color: '#666' }}>{movie.averageRating.toFixed(1)}⭐</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
        {/* 감독 추천 */}
        {directorRecommendation && (
          <div style={{ background: '#f0f8ff', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', padding: '20px' }}>
            <h3 style={{ marginBottom: '20px', color: '#333' }}>🎬 오늘의 감독 추천</h3>
            <div style={{ display: 'flex', gap: '20px' }}>
              {/* 감독 프로필 */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '150px' }}>
                <div style={{ width: '120px', height: '120px', borderRadius: '50%', overflow: 'hidden', marginBottom: '10px', backgroundColor: '#ddd', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {directorRecommendation.director.photoUrl ? (
                    <img src={directorRecommendation.director.photoUrl} alt={directorRecommendation.director.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <span style={{ fontSize: '40px' }}>🎬</span>
                  )}
                </div>
                <h4 style={{ margin: '0 0 5px 0', textAlign: 'center' }}>{directorRecommendation.director.name}</h4>
                <p style={{ margin: '0', fontSize: '14px', color: '#666', textAlign: 'center' }}>영화 {directorRecommendation.movieCount}개</p>
                <p style={{ margin: '5px 0 0 0', fontSize: '14px', color: '#666', textAlign: 'center' }}>평균 평점: {directorRecommendation.averageRating.toFixed(1)}⭐</p>
              </div>
              {/* 대표 작품 */}
              <div style={{ flex: 1 }}>
                <h5 style={{ margin: '0 0 15px 0', color: '#333' }}>대표 작품</h5>
                <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
                  {directorRecommendation.topMovies.map((movie, index) => (
                    <div key={movie.movieCd} style={{ width: '120px', cursor: 'pointer', transition: 'transform 0.2s' }} onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.05)'} onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'} onClick={() => handleMovieClick(movie)}>
                      <div style={{ width: '100%', height: '160px', borderRadius: '8px', overflow: 'hidden', backgroundColor: '#ddd', marginBottom: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        {movie.posterUrl ? (
                          <img src={movie.posterUrl} alt={movie.movieNm} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : (
                          <span style={{ fontSize: '24px' }}>🎬</span>
                        )}
                      </div>
                      <div>
                        <p style={{ margin: '0 0 5px 0', fontSize: '12px', fontWeight: 'bold', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{movie.movieNm}</p>
                        <p style={{ margin: '0', fontSize: '11px', color: '#666' }}>{movie.averageRating.toFixed(1)}⭐</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
        {/* 소셜 친구 추천 */}
        {socialRecommendation && socialRecommendation.recommender && socialRecommendation.movies && socialRecommendation.movies.length > 0 && (
          <div style={{ background: '#fff7f0', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', padding: '20px' }}>
            <h3 style={{ marginBottom: '20px', color: '#333' }}>🍿 {socialRecommendation.recommender.nickname}님이 추천하는 영화</h3>
            <div style={{ display: 'flex', gap: '20px' }}>
              {/* 추천자 프로필 */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '150px' }}>
                <div style={{ width: '100px', height: '100px', borderRadius: '50%', overflow: 'hidden', marginBottom: '10px', backgroundColor: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {socialRecommendation.recommender.profileImageUrl ? (
                    <img src={socialRecommendation.recommender.profileImageUrl} alt={socialRecommendation.recommender.nickname} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <span style={{ fontSize: '36px' }}>👤</span>
                  )}
                </div>
                <h4 style={{ margin: '0 0 5px 0', textAlign: 'center' }}>{socialRecommendation.recommender.nickname}</h4>
                <p style={{ margin: '0', fontSize: '13px', color: '#666', textAlign: 'center' }}>팔로잉 유저</p>
              </div>
              {/* 추천 영화 리스트 */}
              <div style={{ flex: 1 }}>
                <h5 style={{ margin: '0 0 15px 0', color: '#333' }}>추천 영화</h5>
                <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
                  {socialRecommendation.movies.map((movie, index) => (
                    <div key={movie.movieCd || movie.id || index} style={{ width: '120px', cursor: 'pointer', transition: 'transform 0.2s' }} onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.05)'} onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'} onClick={() => handleMovieClick(movie)}>
                      <div style={{ width: '100%', height: '160px', borderRadius: '8px', overflow: 'hidden', backgroundColor: '#ddd', marginBottom: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        {movie.posterUrl ? (
                          <img src={movie.posterUrl} alt={movie.movieNm} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : (
                          <span style={{ fontSize: '24px' }}>🎬</span>
                        )}
                      </div>
                      <div>
                        <p style={{ margin: '0 0 5px 0', fontSize: '12px', fontWeight: 'bold', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{movie.movieNm}</p>
                        <p style={{ margin: '0', fontSize: '11px', color: '#666' }}>{movie.averageRating ? `${movie.averageRating.toFixed(1)}⭐` : ''}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
        {/* 새로운 장르 추천 */}
        {newGenreRecommendation && newGenreRecommendation.success && newGenreRecommendation.genres && newGenreRecommendation.genres.length > 0 && (
          <div style={{ background: '#f0fff0', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', padding: '20px' }}>
            <h3 style={{ marginBottom: '20px', color: '#333' }}>🎯 이런 영화 어때요?</h3>
            <p style={{ marginBottom: '15px', color: '#666', fontSize: '14px' }}>아직 경험해보지 못한 장르의 대표 영화들을 추천해드려요!</p>
            <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
              {(() => {
                const allMovies = [];
                const seen = new Set();
                const genres = newGenreRecommendation.genres;
                let added = 0;
                let idx = 0;
                while (added < 20) {
                  let found = false;
                  for (let g = 0; g < genres.length; g++) {
                    const movie = genres[g].movies[idx];
                    if (movie && !seen.has(movie.movieCd)) {
                      allMovies.push(movie);
                      seen.add(movie.movieCd);
                      added++;
                      found = true;
                      if (added === 20) break;
                    }
                  }
                  if (!found) break;
                  idx++;
                }
                return allMovies.map((movie, index) => (
                  <div key={movie.movieCd || index} style={{ width: '120px', cursor: 'pointer', transition: 'transform 0.2s' }} onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.05)'} onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'} onClick={() => handleMovieClick(movie)}>
                    <div style={{ width: '100%', height: '160px', borderRadius: '8px', overflow: 'hidden', backgroundColor: '#ddd', marginBottom: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      {movie.posterUrl ? (
                        <img src={movie.posterUrl} alt={movie.movieNm} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      ) : (
                        <span style={{ fontSize: '24px' }}>🎬</span>
                      )}
                    </div>
                    <div>
                      <p style={{ margin: '0 0 5px 0', fontSize: '12px', fontWeight: 'bold', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{movie.movieNm}</p>
                      <p style={{ margin: '0', fontSize: '11px', color: '#666' }}>{movie.genreNm || ''}</p>
                      <p style={{ margin: '2px 0 0 0', fontSize: '11px', color: '#666' }}>
                        {typeof movie.averageRating === 'number' && !isNaN(movie.averageRating)
                          ? `${movie.averageRating.toFixed(1)}⭐`
                          : '-'}
                      </p>
                    </div>
                  </div>
                ));
              })()}
            </div>
          </div>
        )}
      </div>
    </div>
  );

  const renderMovieList = () => (
    <div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
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
            getSortedResults(movieListData.data).map((item, index) => (
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
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
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
            getSortedResults(movieDetailData.data).map((item, index) => (
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
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
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
            getSortedResults(boxOfficeData.data).map((item, index) => (
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
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
      <div className="movie-grid">
        {boxOfficeDtoData.data && boxOfficeDtoData.data.length > 0 ? (
          getSortedResults(boxOfficeDtoData.data).map((item, index) => (
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

  const renderMovieDetailDto = ({ currentUser, handleEditMovie, handleDeleteMovie }) => (
    <div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
      <div className="movie-grid">
        {movieDetailDtoData.data && movieDetailDtoData.data.length > 0 ? (
          getSortedResults(movieDetailDtoData.data).map((item, index) => (
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
                {/* 태그 표시 */}
                {item.tags && item.tags.length > 0 && (
                  <div className="movie-tags" style={{ marginTop: '8px' }}>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                      {(openTagCards.includes(item.movieCd) ? item.tags : item.tags.slice(0, 3)).map((tag, tagIndex) => (
                        <span 
                          key={tag.id || tagIndex} 
                          className="tag"
                          style={{
                            backgroundColor: '#e3f2fd',
                            color: '#1976d2',
                            padding: '2px 8px',
                            borderRadius: '12px',
                            fontSize: '11px',
                            cursor: 'pointer',
                            transition: 'all 0.2s ease'
                          }}
                          onMouseEnter={(e) => {
                            e.target.style.backgroundColor = '#bbdefb';
                            e.target.style.transform = 'translateY(-1px)';
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.backgroundColor = '#e3f2fd';
                            e.target.style.transform = 'translateY(0)';
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            setSearchKeyword(tag.name);
                            handleSearch(tag.name);
                          }}
                        >
                          #{tag.name}
                        </span>
                      ))}
                      {item.tags.length > 3 && !openTagCards.includes(item.movieCd) && (
                        <span 
                          style={{ fontSize: '11px', color: '#666', alignSelf: 'center', cursor: 'pointer', background: '#f0f0f0', borderRadius: '12px', padding: '2px 8px' }}
                          onClick={e => { e.stopPropagation(); handleToggleTags(item.movieCd); }}
                        >
                          +{item.tags.length - 3}
                        </span>
                      )}
                      {item.tags.length > 3 && openTagCards.includes(item.movieCd) && (
                        <span 
                          style={{ fontSize: '11px', color: '#666', alignSelf: 'center', cursor: 'pointer', background: '#f0f0f0', borderRadius: '12px', padding: '2px 8px' }}
                          onClick={e => { e.stopPropagation(); handleToggleTags(item.movieCd); }}
                        >
                          접기
                        </span>
                      )}
                    </div>
                  </div>
                )}
                <div className="movie-actions" style={{marginTop: '10px', display: 'flex', gap: '5px'}}>
                  {currentUser && (currentUser.isAdmin || currentUser.role === 'ADMIN') && (
                    <>
                      <button 
                        onClick={e => { e.stopPropagation(); handleEditMovie(item); }}
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
                        onClick={e => { e.stopPropagation(); handleDeleteMovie(item.movieCd); }}
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
                  {/* 찜 버튼 (누르면 토글, 카운트 표시) */}
                  <button
                    onClick={e => {
                      e.stopPropagation();
                      if (item.likedByMe) {
                        handleUnlikeMovie(item.movieCd);
                        if (typeof fetchSocialRecommendation === 'function') fetchSocialRecommendation();
                      } else {
                        handleLikeMovie(item.movieCd);
                        if (typeof fetchSocialRecommendation === 'function') fetchSocialRecommendation();
                      }
                    }}
                    style={{
                      padding: '5px 10px',
                      backgroundColor: item.likedByMe ? '#ffc107' : '#eee',
                      color: item.likedByMe ? 'black' : '#333',
                      border: 'none',
                      borderRadius: '3px',
                      cursor: 'pointer',
                      fontSize: '12px'
                    }}
                  >
                                          {item.likedByMe ? '❤️ 찜 ' : '🤍 찜 '}
                    {item.likeCount}
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
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
      <div className="movie-grid">
        {movieListDtoData.data && movieListDtoData.data.length > 0 ? (
          getSortedResults(movieListDtoData.data).map((item, index) => (
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

  // 예매 모달 핸들러
  const handleBookingClick = () => {
    if (!currentUser) {
      setShowLoginAlert(true);
      return;
    }
    setShowBookingModal(true);
  };

  const handleBookingComplete = (bookingData) => {
    console.log('예매 완료:', bookingData);
    // 예매 완료 후 필요한 처리 (예: 예매 내역 페이지로 이동 등)
  };

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
            {/* 예매하기 버튼 - 맨 위에 배치 */}
            {currentUser && (
              <div style={{ 
                textAlign: 'center', 
                marginBottom: '20px',
                padding: '16px',
                backgroundColor: '#f8f9fa',
                borderRadius: '8px'
              }}>
                <button 
                  className="booking-button"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleBookingClick();
                  }}
                  style={{
                    padding: '16px 32px',
                    backgroundColor: '#667eea',
                    color: 'white',
                    border: 'none',
                    borderRadius: '12px',
                    fontSize: '1.2rem',
                    fontWeight: '700',
                    cursor: 'pointer',
                    transition: 'all 0.3s',
                    boxShadow: '0 4px 12px rgba(102, 126, 234, 0.3)'
                  }}
                  onMouseOver={(e) => {
                    e.target.style.backgroundColor = '#5a6fd8';
                    e.target.style.transform = 'translateY(-3px)';
                    e.target.style.boxShadow = '0 6px 20px rgba(102, 126, 234, 0.4)';
                  }}
                  onMouseOut={(e) => {
                    e.target.style.backgroundColor = '#667eea';
                    e.target.style.transform = 'translateY(0)';
                    e.target.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.3)';
                  }}
                >
                  🎬 예매하기
                </button>
              </div>
            )}
            
            <div className="movie-detail-grid">
              <div className="movie-detail-poster">
                {selectedMovie.posterUrl ? (
                  <img src={selectedMovie.posterUrl} alt={selectedMovie.movieNm} />
                ) : (
                  <div className="no-poster">No Poster</div>
                )}
                {/* 찜 버튼 (포스터 아래, 별점 위) */}
                <div style={{ textAlign: 'center', margin: '16px 0' }}>
                  <button
                    onClick={e => {
                      e.stopPropagation();
                      if (!currentUser) {
                        setShowLoginAlert(true);
                        return;
                      }
                      if (selectedMovie.likedByMe) {
                        handleUnlikeMovie(selectedMovie.movieCd);
                        setSelectedMovie(prev => prev ? { ...prev, likedByMe: false, likeCount: Math.max((prev.likeCount || 1) - 1, 0) } : prev);
                        if (typeof fetchSocialRecommendation === 'function') fetchSocialRecommendation();
                      } else {
                        handleLikeMovie(selectedMovie.movieCd);
                        setSelectedMovie(prev => prev ? { ...prev, likedByMe: true, likeCount: (prev.likeCount || 0) + 1 } : prev);
                        if (typeof fetchSocialRecommendation === 'function') fetchSocialRecommendation();
                      }
                    }}
                    style={{
                      padding: '8px 20px',
                      backgroundColor: selectedMovie.likedByMe ? '#ffc107' : '#eee',
                      color: selectedMovie.likedByMe ? 'black' : '#333',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontSize: '18px',
                      fontWeight: 600,
                      marginBottom: 8
                    }}
                  >
                    {selectedMovie.likedByMe ? '❤️ 찜 ' : '🤍 찜 '}
                    {selectedMovie.likeCount}
                  </button>
                </div>
                {/* ⭐️ 별점 입력 UI */}
                <div style={{ textAlign: 'center', marginTop: 8 }}>
                  <div style={{ marginBottom: 8, color: '#aaa', fontSize: 16 }}>
                    평가하기
                  </div>
                  <StarRating
                    movieCd={selectedMovie.movieCd}
                    userRating={userRating}
                    onChange={score => {
                      if (!currentUser) {
                        setShowLoginAlert(true);
                        return;
                      }
                      handleStarChange(score);
                    }}
                    average={averageRating}
                    count={ratingCount}
                    disabled={loadingRating}
                  />
                  <div style={{ marginTop: 12, color: '#666', fontSize: 15 }}>
                    평균 평점: <b>{typeof averageRating === 'number' && !isNaN(averageRating) ? averageRating.toFixed(1) : '0.0'}</b>점
                    <span style={{ marginLeft: 8, fontSize: 13 }}>
                      ({typeof ratingCount === 'number' && !isNaN(ratingCount) ? ratingCount : 0}명 참여)
                    </span>
                  </div>
                </div>
                {/* ⭐️ 별점 분포 차트 */}
                {ratingDistribution && (
                  <div style={{ marginTop: 12 }}>
                    <RatingDistributionChart distribution={ratingDistribution} />
                  </div>
                )}
                {/* ⭐️ 평균 평점 및 참여 인원 표시 (그래프 아래, 버튼 위) */}
                <div style={{ textAlign: 'center', margin: '16px 0 8px 0', color: '#666', fontSize: 15 }}>
                  평균 평점: <b>{typeof averageRating === 'number' && !isNaN(averageRating) ? averageRating.toFixed(1) : '0.0'}</b>점
                  <span style={{ marginLeft: 8, fontSize: 13 }}>
                    ({typeof ratingCount === 'number' && !isNaN(ratingCount) ? ratingCount : 0}명 참여)
                  </span>
                </div>
                {/* 코멘트 남기기 버튼 */}
                <div style={{ textAlign: 'center', marginTop: 8 }}>
                  <button
                    onClick={() => {
                      if (!currentUser) {
                        setShowLoginAlert(true);
                      } else {
                        setShowReviewModal(true);
                      }
                    }}
                    style={{
                      background: '#ff2f6e',
                      color: 'white',
                      border: 'none',
                      borderRadius: 16,
                      padding: '18px 48px',
                      fontSize: 22,
                      fontWeight: 700,
                      boxShadow: '0 2px 8px rgba(255,47,110,0.12)',
                      cursor: 'pointer',
                      width: '90%',
                      maxWidth: 340,
                      display: 'block',
                      marginLeft: 'auto',
                      marginRight: 'auto',
                      marginTop: 0
                    }}
                  >
                    코멘트 남기기
                  </button>
                </div>
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
                {/* 출연/제작 섹션 - 왓챠피디아 스타일 */}
                <div className="movie-detail-section">
                  <h4>출연/제작</h4>
                  <div className="credit-row">
                    {/* 감독 */}
                    {selectedMovie.directors && selectedMovie.directors.length > 0 && (
                      <div className="credit-card" onClick={() => {
                        console.log('감독 클릭:', selectedMovie.directors[0]);
                        if (selectedMovie.directors[0].id) {
                          setShowMovieDetail(false); // 모달 닫기
                          navigate(`/director/${selectedMovie.directors[0].id}`);
                        } else {
                          console.error('감독 ID가 없습니다:', selectedMovie.directors[0]);
                        }
                      }}>
                        <img src={selectedMovie.directors[0].photoUrl || '/placeholder-actor.png'} alt={selectedMovie.directors[0].peopleNm} />
                        <div>{selectedMovie.directors[0].peopleNm}</div>
                        <div className="credit-role">감독</div>
                      </div>
                    )}
                    {/* 주연 */}
                    {selectedMovie.actors && selectedMovie.actors.filter(a => a.roleType === 'LEAD').map((actor, idx) => (
                      <div className="credit-card" key={"lead-"+idx} onClick={() => {
                        console.log('주연 클릭:', actor);
                        if (actor.id) {
                          setShowMovieDetail(false); // 모달 닫기
                          navigate(`/actor/${actor.id}`);
                        } else {
                          console.error('배우 ID가 없습니다:', actor);
                        }
                      }}>
                        <img src={actor.photoUrl || '/placeholder-actor.png'} alt={actor.peopleNm} />
                        <div>{actor.peopleNm}</div>
                        <div className="credit-role">주연</div>
                      </div>
                    ))}
                    {/* 조연 */}
                    {selectedMovie.actors && selectedMovie.actors.filter(a => a.roleType === 'SUPPORTING').map((actor, idx) => (
                      <div className="credit-card" key={"support-"+idx} onClick={() => {
                        console.log('조연 클릭:', actor);
                        if (actor.id) {
                          setShowMovieDetail(false); // 모달 닫기
                          navigate(`/actor/${actor.id}`);
                        } else {
                          console.error('배우 ID가 없습니다:', actor);
                        }
                      }}>
                        <img src={actor.photoUrl || '/placeholder-actor.png'} alt={actor.peopleNm} />
                        <div>{actor.peopleNm}</div>
                        <div className="credit-role">조연</div>
                      </div>
                    ))}
                  </div>
                </div>
                {/* 태그 섹션 */}
                {selectedMovie.tags && selectedMovie.tags.length > 0 && (
                  <div className="movie-detail-section">
                    <h4>태그</h4>
                    <div className="tag-container" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                      {selectedMovie.tags.map((tag, index) => (
                        <span 
                          key={tag.id || index} 
                          className="tag"
                          style={{
                            backgroundColor: '#f0f0f0',
                            color: '#333',
                            padding: '4px 12px',
                            borderRadius: '16px',
                            fontSize: '14px',
                            cursor: 'pointer',
                            transition: 'all 0.2s ease'
                          }}
                          onMouseEnter={(e) => {
                            e.target.style.backgroundColor = '#e0e0e0';
                            e.target.style.transform = 'translateY(-1px)';
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.backgroundColor = '#f0f0f0';
                            e.target.style.transform = 'translateY(0)';
                          }}
                          onClick={() => {
                            // 태그 클릭 시 해당 태그로 검색
                            setSearchKeyword(tag.name);
                            handleSearch(tag.name);
                            setShowMovieDetail(false);
                          }}
                        >
                          #{tag.name}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                {/* 리뷰 목록 섹션 */}
                <div className="movie-detail-section">
                  <ReviewList 
                    key={reviewListKey}
                    movieCd={selectedMovie.movieCd} 
                    currentUser={currentUser}
                  />
                </div>
                
                {selectedMovie.stillcuts && selectedMovie.stillcuts.length > 0 && (
                  <div className="movie-detail-section">
                    <h4>스틸컷</h4>
                    <div className="stillcut-gallery">
                      {selectedMovie.stillcuts.map((stillcut, index) => (
                        <div key={stillcut.id || index} className="stillcut-item">
                          <img 
                            src={stillcut.imageUrl} 
                            alt={`${selectedMovie.movieNm} 스틸컷 ${index + 1}`}
                            onClick={() => window.open(stillcut.imageUrl, '_blank')}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}
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
                  value={movieForm.actorNames}
                  onChange={(e) => setMovieForm({...movieForm, actorNames: e.target.value})}
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
  const renderSearchResults = () => {
    console.log('렌더링할 searchResults:', searchResults);
    console.log('렌더링할 userResults:', userResults);
    return (
      <div className="search-section">
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
          <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
            <option value="rating">별점순</option>
            <option value="date">개봉일순</option>
            <option value="nameAsc">이름 오름차순</option>
            <option value="nameDesc">이름 내림차순</option>
          </select>
        </div>
        <div className="search-movie-result">
          <h3>영화 결과</h3>
          {searchResults.data && searchResults.data.length > 0 ? (
            <div className="movie-grid">
              {getSortedResults(searchResults.data).map(movie => (
                <div key={movie.movieCd} className="movie-card" onClick={() => handleMovieClick(movie)}>
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
              ))}
            </div>
          ) : (
            <div>검색 결과가 없습니다.</div>
          )}
        </div>
        <div className="search-divider"></div>
        <div className="search-person-result">
          <h3>인물 결과</h3>
          {(personResults.actors && personResults.actors.length > 0) || (personResults.directors && personResults.directors.length > 0) ? (
            <div>
              {personResults.actors && personResults.actors.length > 0 && (
                <div>
                  <h4>배우</h4>
                  <div className="person-grid">
                    {personResults.actors.map(actor => (
                      <div key={actor.id} className="person-card" onClick={() => handleActorClick(actor.id)}>
                        <div className="person-photo">
                          {actor.photoUrl ? (
                            <img src={actor.photoUrl} alt={actor.name} />
                          ) : (
                            <div className="no-photo">No Photo</div>
                          )}
                        </div>
                        <div className="person-info">
                          <h5>{actor.name}</h5>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              {personResults.directors && personResults.directors.length > 0 && (
                <div>
                  <h4>감독</h4>
                  <div className="person-grid">
                    {personResults.directors.map(director => (
                      <div key={director.id} className="person-card" onClick={() => handleDirectorClick(director.id)}>
                        <div className="person-photo">
                          {director.photoUrl ? (
                            <img src={director.photoUrl} alt={director.name} />
                          ) : (
                            <div className="no-photo">No Photo</div>
                          )}
                        </div>
                        <div className="person-info">
                          <h5>{director.name}</h5>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div>검색 결과가 없습니다.</div>
          )}
        </div>
        <div className="search-divider"></div>
        <div className="search-user-result">
          <h3>유저 결과</h3>
          {userResults && userResults.length > 0 ? (
            <ul style={{ listStyle: 'none', padding: 0 }}>
              {userResults.map(nickname => (
                <li key={nickname} style={{ padding: 8, borderBottom: '1px solid #eee', cursor: 'pointer' }} onClick={() => handleUserClick(nickname)}>
                  {nickname}
                </li>
              ))}
            </ul>
          ) : (
            <div>검색 결과가 없습니다.</div>
          )}
        </div>
      </div>
    );
  };

  // 유저 닉네임 클릭 시 마이페이지 이동 (window.location 사용)
  const handleUserClick = (nickname) => {
    window.location.href = `/user/${nickname}`;
  };

  // 배우 클릭 시 배우 상세페이지 이동
  const handleActorClick = (actorId) => {
    window.location.href = `/actor/${actorId}`;
  };

    // 감독 클릭 시 감독 상세페이지 이동
  const handleDirectorClick = (directorId) => {
    window.location.href = `/director/${directorId}`;
  };

  // 태그 데이터 생성 (임시 함수)
  const generateTags = async () => {
    try {
      const response = await axios.post('http://localhost:80/admin/api/tags/generate-from-genres');
      console.log('태그 생성 결과:', response.data);
      alert('태그 생성 완료! 페이지를 새로고침해주세요.');
    } catch (error) {
      console.error('태그 생성 실패:', error);
      alert('태그 생성 실패: ' + error.message);
    }
  };

  // 개봉예정작 렌더링
  const renderComingSoon = () => (
    <div>
      <h2>개봉예정작</h2>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
      <div className="movie-grid">
        {comingSoonData.data && comingSoonData.data.length > 0 ? (
          getSortedResults(comingSoonData.data).map((item, index) => (
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
  );

  // 개봉중 렌더링
  const renderNowPlaying = () => (
    <div>
      <h2>개봉중</h2>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
      <div className="movie-grid">
        {nowPlayingData.data && nowPlayingData.data.length > 0 ? (
          getSortedResults(nowPlayingData.data).map((item, index) => (
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
  );

  // 상영종료 렌더링
  const renderEnded = () => (
    <div>
      <h2>상영종료</h2>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
      <div className="movie-grid">
        {endedData.data && endedData.data.length > 0 ? (
          getSortedResults(endedData.data).map((item, index) => (
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
  );

  // 평점 높은 영화 렌더링
  const renderTopRated = () => (
    <div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16, marginBottom: 16 }}>
        <select value={sortOption} onChange={e => setSortOption(e.target.value)}>
          <option value="rating">별점순</option>
          <option value="date">개봉일순</option>
          <option value="nameAsc">이름 오름차순</option>
          <option value="nameDesc">이름 내림차순</option>
        </select>
      </div>
      <div className="movie-grid">
        {topRatedData && topRatedData.length > 0 ? (
          getSortedResults(topRatedData).map((item, index) => (
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
                  <p><strong>평점:</strong> {item.averageRating ? item.averageRating.toFixed(1) : '-'}</p>
                  <p><strong>개봉일:</strong> {item.openDt || '-'}</p>
                  <p><strong>장르:</strong> {item.genreNm || '-'}</p>
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
    </div>
  );

  // 인기 영화 렌더링
  const renderPopularMovies = () => (
    <div className="movie-grid">
      {popularMoviesData.map((movie) => (
        <div key={movie.movieCd} className="movie-card" onClick={() => handleMovieClick(movie)}>
          <div className="movie-poster">
            {movie.posterUrl ? (
              <img src={movie.posterUrl} alt={movie.movieNm} />
            ) : (
              <div className="no-poster">No Poster</div>
            )}
          </div>
          <div className="movie-info">
            <h3>{movie.movieNm}</h3>
            <p>{movie.genreNm}</p>
            <p>{movie.openDt}</p>
          </div>
        </div>
      ))}
    </div>
  );

  const renderRecommendedMovies = () => {
    if (!currentUser) {
      return (
        <div style={{ textAlign: 'center', padding: '50px', color: '#666' }}>
          <h2>로그인이 필요합니다</h2>
          <p>태그 추천 영화를 보려면 로그인해주세요.</p>
        </div>
      );
    }

    if (loading) {
      return <div style={{ textAlign: 'center', padding: '50px' }}>로딩 중...</div>;
    }

    // recommendedMoviesData가 객체인지 배열인지 확인
    if (
      !recommendedMoviesData ||
      (Array.isArray(recommendedMoviesData) && recommendedMoviesData.length === 0) ||
      (typeof recommendedMoviesData === 'object' && !Array.isArray(recommendedMoviesData) && Object.keys(recommendedMoviesData).length === 0)
    ) {
      return (
        <div style={{ textAlign: 'center', padding: '50px', color: '#666' }}>
          <h2>추천 영화가 없습니다</h2>
          <p>마이페이지에서 선호하는 장르 태그를 설정해보세요!</p>
          <button
            style={{
              background: '#a18cd1',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              padding: '12px 24px',
              fontSize: '16px',
              cursor: 'pointer',
              marginTop: '20px',
              fontWeight: 'bold'
            }}
            onClick={() => window.location.href = `/user/${encodeURIComponent(currentUser.nickname)}`}
          >
            마이페이지로 이동
          </button>
        </div>
      );
    }

    // 태그별 그룹화된 데이터인 경우
    if (typeof recommendedMoviesData === 'object' && !Array.isArray(recommendedMoviesData)) {
      const tagNames = Object.keys(recommendedMoviesData);
      
      return (
        <div>
          <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: '#f8f9fa', borderRadius: '8px' }}>
            <h3 style={{ margin: '0 0 10px 0', color: '#333' }}>🎯 {currentUser.nickname}님을 위한 추천 영화</h3>
            <p style={{ margin: '0', color: '#666', fontSize: '14px' }}>
              마이페이지에서 설정한 선호 장르 태그를 기반으로 추천된 영화입니다.
            </p>
          </div>
          
          {/* 태그별 탭 */}
          <div style={{ marginBottom: '20px' }}>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '15px' }}>
              {tagNames.map((tagName, index) => (
                <button
                  key={tagName}
                  onClick={() => setActiveRecommendedTab(tagName)}
                  style={{
                    padding: '8px 16px',
                    border: 'none',
                    borderRadius: '20px',
                    backgroundColor: activeRecommendedTab === tagName ? '#a18cd1' : '#f0f0f0',
                    color: activeRecommendedTab === tagName ? 'white' : '#333',
                    cursor: 'pointer',
                    fontSize: '14px',
                    fontWeight: activeRecommendedTab === tagName ? 'bold' : 'normal'
                  }}
                >
                  {tagName} ({recommendedMoviesData[tagName].length})
                </button>
              ))}
            </div>
          </div>

          {/* 선택된 탭의 영화들 표시 */}
          <div className="movie-grid">
            {recommendedMoviesData[activeRecommendedTab]?.map((movie) => (
              <div key={movie.movieCd} className="movie-card" onClick={() => handleMovieClick(movie)}>
                <div className="movie-poster">
                  {movie.posterUrl ? (
                    <img src={movie.posterUrl} alt={movie.movieNm} />
                  ) : (
                    <div className="no-poster">No Poster</div>
                  )}
                </div>
                <div className="movie-info">
                  <h3>{movie.movieNm}</h3>
                  <p>{movie.genreNm}</p>
                  <p>{movie.openDt}</p>
                  {movie.totalAudience && (
                    <p style={{ fontSize: '12px', color: '#666' }}>
                      관객수: {movie.totalAudience.toLocaleString()}명
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      );
    }

    // 기존 배열 형태인 경우 (하위 호환성)
    return (
      <div>
        <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: '#f8f9fa', borderRadius: '8px' }}>
          <h3 style={{ margin: '0 0 10px 0', color: '#333' }}>🎯 {currentUser.nickname}님을 위한 추천 영화</h3>
          <p style={{ margin: '0', color: '#666', fontSize: '14px' }}>
            마이페이지에서 설정한 선호 장르 태그를 기반으로 추천된 영화입니다.
          </p>
        </div>
        <div className="movie-grid">
          {recommendedMoviesData.map((movie) => (
            <div key={movie.movieCd} className="movie-card" onClick={() => handleMovieClick(movie)}>
              <div className="movie-poster">
                {movie.posterUrl ? (
                  <img src={movie.posterUrl} alt={movie.movieNm} />
                ) : (
                  <div className="no-poster">No Poster</div>
                )}
              </div>
              <div className="movie-info">
                <h3>{movie.movieNm}</h3>
                <p>{movie.genreNm}</p>
                <p>{movie.openDt}</p>
                {movie.totalAudience && (
                  <p style={{ fontSize: '12px', color: '#666' }}>
                    관객수: {movie.totalAudience.toLocaleString()}명
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  // 최근 검색어 클릭 시 바로 검색
  const handleRecentKeywordClick = (keyword) => {
    setSearchKeyword(keyword);
    handleSearch(keyword);
  };

  // 인기 검색어 클릭 시 바로 검색
  const handlePopularKeywordClick = (keyword) => {
    setSearchKeyword(keyword);
    handleSearch(keyword);
  };

  // 로그인 안내 모달 컴포넌트
  const LoginAlertModal = () => (
    <div className="modal-overlay" onClick={() => setShowLoginAlert(false)}>
      <div className="modal-content" style={{ maxWidth: 340, textAlign: 'center' }} onClick={e => e.stopPropagation()}>
        <h3 style={{ margin: '24px 0 12px 0' }}>로그인이 필요합니다</h3>
        <p style={{ marginBottom: 24 }}>리뷰를 작성하려면 로그인이 필요합니다.</p>
        <button
          style={{
            background: '#a18cd1', color: 'white', border: 'none', borderRadius: 5,
            padding: '10px 24px', fontSize: 16, cursor: 'pointer', marginBottom: 12
          }}
          onClick={() => { setShowLoginAlert(false); window.location.href = '/login'; }}
        >
          로그인 하러가기
        </button>
        <br />
        <button
          style={{ background: 'none', color: '#888', border: 'none', fontSize: 14, cursor: 'pointer' }}
          onClick={() => setShowLoginAlert(false)}
        >
          닫기
        </button>
      </div>
    </div>
  );

  const [showLoginAlert, setShowLoginAlert] = useState(false);

  // App 컴포넌트 최상단에 추가
  const [ratingDistribution, setRatingDistribution] = useState(null);






  const navigate = useNavigate();

  // 태그 펼침 토글 함수
  const handleToggleTags = (movieCd) => {
    setOpenTagCards(prev =>
      prev.includes(movieCd)
        ? prev.filter(id => id !== movieCd)
        : [...prev, movieCd]
    );
  };

  // 모든 모달 닫기 함수
  const closeAllModals = () => {
    setShowBookingModal(false);
    setShowMovieDetail(false);
    setShowMovieForm(false);
    setShowReviewModal(false);
    // 필요시 다른 모달도 닫기
  };

  // 내 예매목록(마이페이지)로 이동 함수
  const goToMyReservations = () => {
    closeAllModals();
    if (currentUser?.nickname) {
      navigate(`/user/${currentUser.nickname}`);
      setTimeout(() => {
        const evt = new CustomEvent('openUserReservations');
        window.dispatchEvent(evt);
      }, 100);
    }
  };

  // current-user 정보 받아온 후 소셜 추천 정보 가져오기
  useEffect(() => {
    if (!currentUser || !currentUser.id) return;
    let isCurrent = true;
    const fetchSocialRecommendation = async () => {
      setSocialRecommendationLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${currentUser.id}/daily-social-recommendation`, { withCredentials: true });
        if (response.data.success && isCurrent) {
          setSocialRecommendation(response.data);
        }
      } catch (error) {
        console.error('소셜 추천 정보 조회 실패:', error);
      } finally {
        if (isCurrent) setSocialRecommendationLoading(false);
      }
    };
    fetchSocialRecommendation();
    return () => { isCurrent = false; };
  }, [currentUser?.id]);

  // 소셜 친구 추천 UI 렌더링 함수
  const renderSocialRecommendation = () => {
    if (socialRecommendationLoading) {
      return <div style={{ marginTop: '30px' }}>소셜 친구 추천 정보를 불러오는 중...</div>;
    }
    if (!socialRecommendation || !socialRecommendation.recommender || !socialRecommendation.movies || socialRecommendation.movies.length === 0) {
      return null;
    }
    const { recommender, movies } = socialRecommendation;
    return (
      <div style={{ marginTop: '30px' }}>
        <h3 style={{ marginBottom: '20px', color: '#333' }}>🍿 {recommender.nickname}님이 추천하는 영화</h3>
        <div style={{ 
          display: 'flex', 
          gap: '20px', 
          padding: '20px', 
          backgroundColor: '#f8f6ff', 
          borderRadius: '12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
        }}>
          {/* 추천자 프로필 */}
          <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            alignItems: 'center',
            minWidth: '150px'
          }}>
            <div style={{
              width: '100px',
              height: '100px',
              borderRadius: '50%',
              overflow: 'hidden',
              marginBottom: '10px',
              backgroundColor: '#eee',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              {recommender.profileImageUrl ? (
                <img 
                  src={recommender.profileImageUrl} 
                  alt={recommender.nickname}
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              ) : (
                <span style={{ fontSize: '36px' }}>👤</span>
              )}
            </div>
            <h4 style={{ margin: '0 0 5px 0', textAlign: 'center' }}>
              {recommender.nickname}
            </h4>
            <p style={{ margin: '0', fontSize: '13px', color: '#666', textAlign: 'center' }}>
              팔로잉 유저
            </p>
          </div>
          {/* 추천 영화 리스트 */}
          <div style={{ flex: 1 }}>
            <h5 style={{ margin: '0 0 15px 0', color: '#333' }}>추천 영화</h5>
            <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
              {movies.map((movie, index) => (
                <div 
                  key={movie.movieCd || movie.id || index}
                  style={{
                    width: '120px',
                    cursor: 'pointer',
                    transition: 'transform 0.2s'
                  }}
                  onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.05)'}
                  onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'}
                  onClick={() => handleMovieClick(movie)}
                >
                  <div style={{
                    width: '100%',
                    height: '160px',
                    borderRadius: '8px',
                    overflow: 'hidden',
                    backgroundColor: '#ddd',
                    marginBottom: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    {movie.posterUrl ? (
                      <img 
                        src={movie.posterUrl} 
                        alt={movie.movieNm}
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                    ) : (
                      <span style={{ fontSize: '24px' }}>🎬</span>
                    )}
                  </div>
                  <div>
                    <p style={{ 
                      margin: '0 0 5px 0', 
                      fontSize: '12px', 
                      fontWeight: 'bold',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}>
                      {movie.movieNm}
                    </p>
                    <p style={{ margin: '0', fontSize: '11px', color: '#666' }}>
                      {movie.averageRating ? `${movie.averageRating.toFixed(1)}⭐` : ''}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <>
      {/* 기존 헤더/네비게이션 등 */}
      <Routes>
        <Route path="/" element={
          <MainPage
            searchKeyword={searchKeyword}
            setSearchKeyword={setSearchKeyword}
            handleSearch={handleSearch}
            handleClearSearch={handleClearSearch}
            searchResults={searchResults}
            loading={loading}
            renderStats={renderStats}
            renderMovieList={renderMovieList}
            renderMovieDetail={renderMovieDetail}
            renderBoxOffice={renderBoxOffice}
            renderBoxOfficeDto={renderBoxOfficeDto}
            renderMovieDetailDto={renderMovieDetailDto}
            renderMovieListDto={renderMovieListDto}
            renderTopRated={renderTopRated}
            renderPopularMovies={renderPopularMovies}
            renderRecommendedMovies={renderRecommendedMovies}
            renderComingSoon={renderComingSoon}
            renderNowPlaying={renderNowPlaying}
            renderEnded={renderEnded}
            renderSearchResults={renderSearchResults}
            searchExecuted={searchExecuted}
            currentUser={currentUser}
            handleLogout={handleLogout}
            activeMenu={activeMenu}
            setActiveMenu={handleMenuChange}
            handleMovieClick={handleMovieClick}
            handleEditMovie={handleEditMovie}
            handleAddMovie={handleAddMovie}
            handleDeleteMovie={handleDeleteMovie}
            handleSaveMovie={handleSaveMovie}
            handleLikeMovie={handleLikeMovie}
            selectedMovie={selectedMovie}
            showMovieDetail={showMovieDetail}
            setShowMovieDetail={setShowMovieDetail}
            showMovieForm={showMovieForm}
            setShowMovieForm={setShowMovieForm}
            editingMovie={editingMovie}
            movieForm={movieForm}
            setMovieForm={setMovieForm}
            renderMovieDetailModal={renderMovieDetailModal}
            renderMovieForm={renderMovieForm}
            recentKeywords={recentKeywords}
            handleRecentKeywordClick={handleRecentKeywordClick}
            handleDeleteRecentKeyword={handleDeleteRecentKeyword}
            popularKeywords={popularKeywords}
            handlePopularKeywordClick={handlePopularKeywordClick}
            newGenreRecommendation={newGenreRecommendation}
            fetchNewGenreRecommendation={fetchNewGenreRecommendation}
          />
        } />
        <Route path="/login" element={<Login onLoginSuccess={handleLoginSuccess} />} />
        <Route path="/signup" element={<Signup onSignupSuccess={handleSignupSuccess} />} />
        <Route path="/social-join" element={<SocialJoin />} />
        <Route path="/user-search" element={<UserSearch />} />
        <Route path="/user/:nickname" element={<UserPage onMovieClick={handleMovieClick} />} />
        <Route path="/find-id" element={<FindId />} />
        <Route path="/find-password" element={<FindPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/director/:id" element={<PersonDetail type="director" />} />
        <Route path="/actor/:id" element={<PersonDetail type="actor" />} />
      </Routes>
      
      {/* 영화 상세 모달 - 모든 라우트에서 항상 렌더링 */}
      {showMovieDetail && selectedMovie && renderMovieDetailModal()}
      
      {/* 영화 등록/수정 폼 모달 - 모든 라우트에서 항상 렌더링 */}
      {showMovieForm && renderMovieForm()}
      
      {/* 로그인 안내 모달 */}
      {showLoginAlert && <LoginAlertModal />}
      
      {/* 예매 모달 */}
      {showBookingModal && selectedMovie && (
        <BookingModal
          movie={selectedMovie}
          onClose={() => setShowBookingModal(false)}
          onBookingComplete={handleBookingComplete}
          goToMyReservations={goToMyReservations}
        />
      )}
      {/* 영화 상세에서 코멘트 남기기 버튼 노출 예시 */}
      {showReviewModal && (
        <ReviewModal
          movieTitle={selectedMovie?.movieNm || '영화 제목'}
          movieCd={selectedMovie?.movieCd}
          onSave={(content, spoiler) => {
            // 영화 상세 페이지에서 이미 입력된 별점 사용
            const reviewData = {
              content,
              rating: userRating, // 영화 상세 페이지의 별점 사용
              spoiler,
              movieCd: selectedMovie.movieCd,
            };
            console.log('리뷰 저장 요청 데이터:', reviewData);
            
            axios.post(`${API_BASE_URL}/reviews`, reviewData, { withCredentials: true })
            .then(res => {
              console.log('리뷰 저장 성공', res.data);
              // 리뷰 목록 새로고침
              setReviewListKey(prev => prev + 1);
              // 별점 정보도 새로고침
              if (selectedMovie) {
                fetchUserRating(selectedMovie.movieCd);
                fetchAverageRating(selectedMovie.movieCd);
                fetchRatingDistribution(selectedMovie.movieCd);
              }
            })
            .catch(err => {
              console.error('리뷰 저장 실패', err);
              console.error('에러 응답:', err.response?.data);
            });
            setShowReviewModal(false);
          }}
          onClose={() => setShowReviewModal(false)}
        />
      )}

    </>
  );
}

export default App; 