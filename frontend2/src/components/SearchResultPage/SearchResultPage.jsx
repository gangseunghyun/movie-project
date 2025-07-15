import React, { useState, useEffect } from 'react';
import styles from './SearchResultPage.module.css';
import searchIcon from '../../assets/search_icon.png';
import { useNavigate, useSearchParams } from 'react-router-dom';
import MovieCard from '../MainPage/MovieCard';
import MovieHorizontalSlider from '../MainPage/MovieHorizontalSlider';
import axios from 'axios';
import { Link } from 'react-router-dom';
import userIcon from '../../assets/user_icon.png';
function ActorCard({ actor }) {
  const personLink = actor.role === '감독'
    ? `/person/director/${actor.id}`
    : `/person/actor/${actor.id}`;
  return (
    <Link to={personLink} className={styles.actorCardLink}>
      <div className={styles.actorCard}>
        <div
          className={styles.actorImg}
          style={{
            backgroundImage: actor.photoUrl ? `url(${actor.photoUrl})` : 'none',
            backgroundSize: 'cover',
            backgroundPosition: 'center'
          }}
        ></div>
        <div className={styles.actorName}>{actor.name}</div>
        <div className={styles.actorRole}>{actor.role}</div>
      </div>
    </Link>
  );
}

function UserCard({ user, onUserClick }) {
  const [followersCount, setFollowersCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);

  // 팔로워/팔로잉 수 가져오기
  useEffect(() => {
    if (!user.id) return;

    const fetchFollowCounts = async () => {
      try {
        // 팔로워 수 가져오기
        const followersResponse = await fetch(`http://localhost:80/api/users/${user.id}/followers`, {
          credentials: 'include',
        });
        if (followersResponse.ok) {
          const followersData = await followersResponse.json();
          setFollowersCount(followersData.data.length);
          console.log('팔로워 수:', followersData.data.length);
        } else {
          console.error('팔로워 정보를 가져오는데 실패했습니다.');
        }

        // 팔로잉 수 가져오기
        const followingResponse = await fetch(`http://localhost:80/api/users/${user.id}/following`, {
          credentials: 'include',
        });
        if (followingResponse.ok) {
          const followingData = await followingResponse.json();
          setFollowingCount(followingData.data.length);
        } else {
          console.error('팔로잉 정보를 가져오는데 실패했습니다.');
        }
      } catch (error) {
        console.error('팔로워/팔로잉 정보를 가져오는 중 오류가 발생했습니다:', error);
      }
    };

    fetchFollowCounts();
  }, [user.id]);

  return (
    <div className={styles.userCard} onClick={() => onUserClick(user.nickname)}>
      <div
        className={styles.userImg}
        style={{
          backgroundImage: `url(${user.profileImageUrl ? user.profileImageUrl : userIcon})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center'
        }}
      ></div>
      <div className={styles.userName}>{user.nickname}</div>
      <div className={styles.userFollowStats}>
        팔로워 {followersCount}명 · 팔로잉 {followingCount}명
      </div>
    </div>
  );
}

function ActorHorizontalSlider({ data }) {
  const [start, setStart] = useState(0);
  const visible = 7;
  const prev = () => setStart(Math.max(0, start - visible));
  const next = () => setStart(Math.min(data.length - visible, start + visible));
  return (
    <div className={styles.sliderWrapper}>
      {start > 0 && <button className={`${styles.navBtn} ${styles.left}`} onClick={prev}>{'<'}</button>}
      <div className={styles.slider}>
        {data.slice(start, start + visible).map((actor, idx) => (
          <ActorCard key={actor.name + actor.role} actor={actor} />
        ))}
      </div>
      {start + visible < data.length && <button className={`${styles.navBtn} ${styles.right}`} onClick={next}>{'>'}</button>}
    </div>
  );
}



export default function SearchResultPage() {
  const [searchParams] = useSearchParams();
  const initialQuery = searchParams.get('query') || '';
  const [input, setInput] = useState(initialQuery);
  const [search, setSearch] = useState(initialQuery);
  const [movies, setMovies] = useState([]);
  const [people, setPeople] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setInput(initialQuery);
    setSearch(initialQuery);
  }, [initialQuery]);

  useEffect(() => {
    if (!search) return;
    window.scrollTo(0, 0);
    setLoading(true);
    setError(null);

    // 영화 검색, 인물 검색, 유저 검색을 병렬로 실행
    Promise.all([
      fetch(`http://localhost:80/data/api/movie-detail-dto/search?keyword=${encodeURIComponent(search)}&page=0&size=20`),
      fetch(`http://localhost:80/data/api/search-person?keyword=${encodeURIComponent(search)}`),
      axios.get(`http://localhost:80/api/users/search?nickname=${encodeURIComponent(search)}`, {
        withCredentials: true
      })
    ])
      .then(responses => {
        if (!responses[0].ok) throw new Error('영화 검색 실패');
        if (!responses[1].ok) throw new Error('인물 검색 실패');
        return Promise.all([responses[0].json(), responses[1].json(), responses[2].data]);
      })
      .then(async ([movieData, personData, userData]) => {
        const movies = movieData.data || [];
        setMovies(movies);
      
        
        // 인물 데이터 처리
        const peopleList = [];
        const peopleMap = {};

        // 배우 데이터 추가
        (personData.actors || []).forEach(actor => {
          if (actor.name && !peopleMap[actor.name]) {
            peopleMap[actor.name] = true;
            peopleList.push({
              name: actor.name,
              role: '배우',
              photoUrl: actor.photoUrl,
              id: actor.id
            });
          }
        });

        // 감독 데이터 추가
        (personData.directors || []).forEach(director => {
          if (director.name && !peopleMap[director.name]) {
            peopleMap[director.name] = true;
            peopleList.push({
              name: director.name,
              role: '감독',
              photoUrl: director.photoUrl,
              id: director.id
            });
          }
        });

        setPeople(peopleList);
        
        // 유저 데이터 처리
        // const usersList = userData.map(nickname => ({
        //   nickname: nickname,
        //   profileImageUrl: null // API에서 프로필 이미지 URL이 제공되지 않으므로 null로 설정
        // }));
        setUsers(userData);
        
         console.log('영화:', movies);
        // console.log('인물:', peopleList);
        //console.log('유저:', userData);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [search]);

  const navigate = useNavigate();
  const handleSearch = () => {
    if (input.trim()) {
      // 최근검색어 저장
      axios.post('http://localhost:80/api/search-history', null, {
        params: { keyword: input, searchResultCount: 0 },
        withCredentials: true
      }).catch(() => { });
      setSearch(input);
      navigate(`/search?query=${encodeURIComponent(input)}`);
    }
  };
  const handleKeyDown = (e) => {
    if (e.key === 'Enter') handleSearch();
  };

  const handleUserClick = (nickname) => {
    // 유저 정보 가져오기
    axios.get(`http://localhost:80/api/users/nickname/${encodeURIComponent(nickname)}`, {
      withCredentials: true
    })
    .then(response => {
      const userData = response.data;
      
      // 유저 정보를 state에 저장하고 마이페이지로 이동
      const userInfo = {
        id: userData.id,
        nickname: userData.nickname,
        profileImageUrl: userData.profileImageUrl
      };
      
      // sessionStorage에 임시로 저장 (새로고침 시에도 유지)
      sessionStorage.setItem('tempUserInfo', JSON.stringify(userInfo));
      
      // 유저의 마이페이지로 이동
      navigate(`/mypage/${userData.id}`);
    })
    .catch(error => {
      console.error('유저 정보 가져오기 실패:', error);
      // 에러 발생 시에도 닉네임으로 이동 시도
      navigate(`/mypage/${nickname}`);
    });
  };

  return (
    <div className={styles.main}>
      <div className={styles.searchBarWrap}>
        <input className={styles.searchInput} value={input} onChange={e => setInput(e.target.value)} onKeyDown={handleKeyDown} />
        <img src={searchIcon} alt="검색" className={styles.searchIcon} onClick={handleSearch} />
      </div>
      
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>영화</h2>
        {/* {loading && <div>로딩중...</div>} */}
        {error && <div style={{ color: 'red' }}>{error}</div>}
        {(!loading && movies.length === 0) && <div className={styles.emptyMessage}>검색 결과가 없습니다.</div>}
        {movies.length > 0 && (
          <MovieHorizontalSlider 
            data={movies} 
            sectionKey="search" 
            CardComponent={MovieCard}
          />
        )}
      </div>
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>인물</h2>
        {people.length === 0 ? (
          <div className={styles.emptyMessage}>검색 결과가 없습니다.</div>
        ) : people.length > 7 ? (
          <ActorHorizontalSlider data={people} />
        ) : (
          <div className={styles.cardList}>
            {people.map(actor => (
              <ActorCard key={actor.name + actor.role} actor={actor} />
            ))}
          </div>
        )}
      </div>
      
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>유저</h2>
        {users.length === 0 ? (
          <div className={styles.emptyMessage}>검색 결과가 없습니다.</div>
        ) : (
          <div className={styles.cardList}>
            {users.map((user, idx) => (
              <UserCard key={user.nickname + idx} user={user} onUserClick={handleUserClick} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}