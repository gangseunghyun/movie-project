import React, { useEffect, useState, useRef, useCallback } from 'react';
import styles from './MainPage.module.css';
import BannerSlider from './BannerSlider';
import MovieHorizontalSlider from './MovieHorizontalSlider';
import { useUser } from '../../contexts/UserContext';

export default function MainPage() {
  const { user } = useUser();
  const nickname = user?.nickname || user?.name || '';
  const [topRatedMovies, setTopRatedMovies] = useState([]);
  const [popularMovies, setPopularMovies] = useState([]);
  const [popularRatings, setPopularRatings] = useState({});
  const [comingSoonMovies, setComingSoonMovies] = useState([]);
  const [comingSoonRatings, setComingSoonRatings] = useState({});
  const [boxOfficeData, setBoxOfficeData] = useState([]);
  const [ratings, setRatings] = useState({});
  const [actorMovies, setActorMovies] = useState([]);
  const [actorInfo, setActorInfo] = useState(null);
  const [directorMovies, setDirectorMovies] = useState([]);
  const [directorInfo, setDirectorInfo] = useState(null);
  const [recommendedMovies, setRecommendedMovies] = useState({}); // 기존
  const [socialRecommendedMovies, setSocialRecommendedMovies] = useState([]); // 기존
  const [socialRecommender, setSocialRecommender] = useState(null); // 추가
  const [mainSocialRecommendedMovies, setMainSocialRecommendedMovies] = useState([]); // 추가
  const [mainSocialRecommender, setMainSocialRecommender] = useState(null); // 추가
  const [newGenreRecommendations, setNewGenreRecommendations] = useState([]); // 추가

  // 페이지 로드 시 맨 위로 스크롤
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  useEffect(() => {
    fetch('http://localhost:80/data/api/box-office-dto?page=0&size=20')
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }
        return res.json();
      })
      .then(data => {
        const movies = data.data || [];
        //console.log('박스오피스 데이터:', movies);
        setBoxOfficeData(movies);
      })
      .catch(error => {
        console.error('박스오피스 데이터 로드 실패:', error);
        setBoxOfficeData([]);
      });

    // 평점 높은 영화 fetch
    fetch('http://localhost:80/data/api/ratings/top-rated?limit=5')
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }
        return res.json();
      })
      .then(data => {
        //console.log('평점 높은 영화:', data);
        setTopRatedMovies(data);
      })
      .catch(error => {
        console.error('평점 높은 영화 로드 실패:', error);
        setTopRatedMovies([]);
      });

    // 인기 영화 fetch
    // fetch('http://localhost:80/data/api/popular-movies?limit=10')
    //   .then(res => res.json())
    //   .then(data => {
    //     //console.log('인기 영화:', data);
    //     setPopularMovies(data.data || []);
    //   });

    fetch('http://localhost:80/data/api/movies/coming-soon?page=0&size=20')
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }
        return res.json();
      })
      .then(data => {
        const movies = data.data || [];
        setComingSoonMovies(movies);
        //console.log('개봉예정작 API 응답:', data);

      })
      .catch(error => {
        console.error('개봉예정작 로드 실패:', error);
        setComingSoonMovies([]);
      });

    fetch('http://localhost:80/api/person/recommended-actor')
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }
        return res.json();
      })
      .then(data => {
        if (data.success && data.data) {
          setActorInfo(data.data.actor); // 배우 정보
          const movies = data.data.allMovies || [];

          // 배우 추천 영화에 필요한 정보 추가
          const enrichedMovies = movies.map(movie => ({
            ...movie,
            averageRating: movie.averageRating || 0.0,
            description: movie.description || '',
            showTm: movie.showTm || 0,
            companyNm: movie.companyNm || '',
            directorName: movie.directorName || ''
          }));

          setActorMovies(enrichedMovies);
        }
      })
      .catch(error => {
        console.error('배우 추천 영화 로드 실패:', error);
        setActorMovies([]);
        setActorInfo(null);
      });

    fetch('http://localhost:80/api/person/recommended-director')
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }
        return res.json();
      })
      .then(data => {
        if (data.success && data.data) {
          setDirectorInfo(data.data.director); // 감독 정보
          const movies = data.data.allMovies || [];

          // 감독 추천 영화에 필요한 정보 추가
          const enrichedMovies = movies.map(movie => ({
            ...movie,
            averageRating: movie.averageRating || 0.0,
            description: movie.description || '',
            showTm: movie.showTm || 0,
            companyNm: movie.companyNm || '',
            directorName: movie.directorName || ''
          }));
          console.log('감독 추천 영화:', enrichedMovies);
          setDirectorMovies(enrichedMovies);
        }
      })
      .catch(error => {
        console.error('감독 추천 영화 로드 실패:', error);
        setDirectorMovies([]);
        setDirectorInfo(null);
      });

  }, []);

  // user가 있을 때 추천 영화 fetch
  useEffect(() => {
    if (user && user.id) {
      fetch(`http://localhost:80/api/users/${user.id}/recommended-movies`, {
        credentials: 'include'
      })
        .then(res => {
          if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
          }
          return res.json();
        })
        .then(data => {
          
          setRecommendedMovies(data);
        })
        .catch(error => {
          console.error('사용자 추천 영화 로드 실패:', error);
          setRecommendedMovies({});
        });
      // 소셜 추천 영화 fetch
      fetch(`http://localhost:80/api/users/${user.id}/daily-social-recommendation`, {
        credentials: 'include'
      })
        .then(res => {
          if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
          }
          return res.json();
        })
        .then(data => {
          //console.log("소셜 추천 영화 API 응답:", data.movies);
          setSocialRecommendedMovies(Array.isArray(data.movies) ? data.movies : []);
          setSocialRecommender(data.recommender || null); // 추가
        })
        .catch(error => {
          console.error('소셜 추천 영화 로드 실패:', error);
          setSocialRecommendedMovies([]);
          setSocialRecommender(null);
        });
      // 새로운 장르 추천 fetch
      fetch(`http://localhost:80/api/users/${user.id}/new-genre-recommendation`, {
        credentials: 'include'
      })
        .then(res => {
          if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
          }
          return res.json();
        })
        .then(data => {
          console.log("새로운 장르 추천 API 응답:", data);
          setNewGenreRecommendations(Array.isArray(data.genres) ? data.genres : []);
        })
        .catch(error => {
          console.error('새로운 장르 추천 로드 실패:', error);
          setNewGenreRecommendations([]);
        });
    } else {
      setRecommendedMovies({});
      setSocialRecommendedMovies([]);
      setMainSocialRecommendedMovies([]);
      setMainSocialRecommender(null);
      setNewGenreRecommendations([]);
    }
  }, [user]);


  // 새로운 장르 추천 영화 - 21개 장르 중 20개 랜덤 선택, 장르당 1개씩 (중복 제거)
  const shuffledGenres = [...newGenreRecommendations]
    .sort(() => Math.random() - 0.5) // 장르 배열을 랜덤하게 섞기
    .slice(0, 20); // 상위 20개 장르 선택
    
  // 중복 제거를 위한 Set 사용
  const seenMovieCds = new Set();
  const finalNewGenreMovies = shuffledGenres
    .map(genreObj => {
      if (!Array.isArray(genreObj.movies) || genreObj.movies.length === 0) return null;
      
      // 해당 장르의 영화들 중에서 아직 사용되지 않은 첫 번째 영화 찾기
      const availableMovie = genreObj.movies.find(movie => !seenMovieCds.has(movie.movieCd));
      
      if (availableMovie) {
        seenMovieCds.add(availableMovie.movieCd);
        return availableMovie;
      }
      
      return null;
    })
    .filter(Boolean); // null 값 제거

  const sections = [
    { title: '평점 높은 영화', key: 'toprated', data: topRatedMovies },
    // { title: '인기 영화', key: 'popular', data: popularMovies },
    { title: '박스오피스 순위', key: 'boxoffice', data: boxOfficeData, ratings },
    { title: '개봉 예정작', key: 'upcoming', data: comingSoonMovies },
    // 선호 태그별 추천 영화 섹션 추가
    ...(user && Object.keys(recommendedMovies).length > 0
      ? Object.entries(recommendedMovies).map(([tag, movies]) => ({
        title: `${tag} 영화`,
        key: `like-${tag}`,
        data: Array.isArray(movies) ? movies : [],
      }))
      : []
    ),
    // 소셜 추천 영화 섹션 추가
    ...(user && socialRecommendedMovies.length > 0
      ? [{
          title: (
            <span style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
              {socialRecommender?.profileImageUrl && (
                <img
                  src={socialRecommender.profileImageUrl}
                  alt="프로필"
                  style={{ width: 28, height: 28, borderRadius: '50%', objectFit: 'cover' }}
                />
              )}
              <span style={{fontWeight: 600}}>
                {socialRecommender?.nickname || '친구'}
              </span>
              <span>님이 추천한 영화</span>
            </span>
          ),
          key: 'social-recommend',
          data: socialRecommendedMovies,
        }]
      : []
    ),
    // 새로운 장르 추천 섹션(하나만)
    ...(user && finalNewGenreMovies.length > 0
      ? [{
          title: '새로운 장르 추천',
          key: 'new-genre-recommend',
          data: finalNewGenreMovies,
        }]
      : []
    ),
    { title: directorInfo && directorInfo.name ? `${directorInfo.name} 감독 영화` : '감독 영화', key: 'director', data: directorMovies, directorInfo },
    { title: actorInfo && actorInfo.name ? `${actorInfo.name} 출연 영화` : '배우 출연 영화', key: 'actor', data: actorMovies, actorInfo },
  ];

  const [visibleSectionCount, setVisibleSectionCount] = useState(3);
  const observerRef = useRef(null);
  const loadMoreRef = useRef(null);

  const handleIntersect = useCallback((entries) => {
    if (entries[0].isIntersecting) {
      setVisibleSectionCount((prev) => prev + 3);
    }
  }, []);

  useEffect(() => {
    if (!loadMoreRef.current) return;
    if (observerRef.current) observerRef.current.disconnect();
    observerRef.current = new window.IntersectionObserver(handleIntersect, {
      root: null,
      rootMargin: '0px',
      threshold: 1.0,
    });
    observerRef.current.observe(loadMoreRef.current);
    return () => observerRef.current && observerRef.current.disconnect();
  }, [handleIntersect, loadMoreRef, sections]);

  return (
    <div className={styles.main}>
      <BannerSlider />
      {sections.slice(0, visibleSectionCount).map(section => (
        <div key={section.key} className={styles.section}>
          <h2 className={styles.sectionTitle}>{section.title}</h2>
          <MovieHorizontalSlider
            data={section.data}
            sectionKey={section.key}
            ratings={section.ratings}
            actorInfo={section.key === 'actor' ? section.actorInfo : undefined}
          />
        </div>
      ))}
      {/* 무한스크롤 트리거용 div */}
      <div ref={loadMoreRef} style={{ height: 1 }} />
    </div>
  );
}