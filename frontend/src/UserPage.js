import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  getUserByNickname, 
  getUserPreferredTags, 
  getGenreTags
} from './services/userService';
import axios from 'axios';
import './UserPage.css';
import UserReservations from './UserReservations';
import ReservationReceipt from './ReservationReceipt';


const UserPage = ({ onMovieClick }) => {
  const { nickname } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [preferredTags, setPreferredTags] = useState([]);
  const [genreTags, setGenreTags] = useState([]);
  const [editMode, setEditMode] = useState(false);
  const [selectedTags, setSelectedTags] = useState([]);
  const [saving, setSaving] = useState(false);
  
  // 찜한 영화 관련 상태 추가
  const [likedMovies, setLikedMovies] = useState([]);
  const [likedMoviesLoading, setLikedMoviesLoading] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  
  // 좋아요한 인물 관련 상태 추가
  const [likedActors, setLikedActors] = useState([]);
  const [likedDirectors, setLikedDirectors] = useState([]);
  const [likedActorsLoading, setLikedActorsLoading] = useState(false);
  const [likedDirectorsLoading, setLikedDirectorsLoading] = useState(false);

  // 내가 작성한 코멘트(리뷰) 상태 추가
  const [myComments, setMyComments] = useState([]);
  const [myCommentsLoading, setMyCommentsLoading] = useState(false);

  // 내가 좋아요한 코멘트 상태 추가
  const [likedComments, setLikedComments] = useState([]);
  const [likedCommentsLoading, setLikedCommentsLoading] = useState(false);



  // 예매 관련 상태
  const [selectedReservationId, setSelectedReservationId] = useState(null);
  const [showReservations, setShowReservations] = useState(false);

  // 팔로우/팔로워 관련 상태
  const [isFollowing, setIsFollowing] = useState(false);
  const [followLoading, setFollowLoading] = useState(false);
  const [followers, setFollowers] = useState([]);
  const [following, setFollowing] = useState([]);
  const [showFollowModal, setShowFollowModal] = useState(false);
  const [followModalType, setFollowModalType] = useState('followers'); // 'followers' or 'following'

  // openUserReservations 이벤트 수신해서 예매목록 모달 자동 오픈
  useEffect(() => {
    const handleOpenReservations = () => setShowReservations(true);
    window.addEventListener('openUserReservations', handleOpenReservations);
    return () => window.removeEventListener('openUserReservations', handleOpenReservations);
  }, []);

  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getUserByNickname(nickname);
        setUser(data);
        if (data && data.id) {
          const [tags, genres] = await Promise.all([
            getUserPreferredTags(data.id),
            getGenreTags()
          ]);
          setGenreTags(genres);
          // preferredTags, selectedTags는 genreTags에 존재하는 값만
          const validTags = tags.filter(tag => genres.includes(tag));
          setPreferredTags(validTags);
          setSelectedTags(validTags);
        }
      } catch (err) {
        setError('유저 정보를 불러올 수 없습니다.');
      }
      setLoading(false);
    };
    fetchUser();
  }, [nickname]);

  // 현재 로그인한 사용자 정보 가져오기
  useEffect(() => {
    const fetchCurrentUser = async () => {
      try {
        const response = await axios.get('http://localhost:80/api/current-user', { withCredentials: true });
        if (response.data.success) {
          setCurrentUser(response.data.user);
        }
      } catch (error) {
        console.log('로그인되지 않은 사용자');
      }
    };
    fetchCurrentUser();
  }, []);

  // 찜한 영화 목록 가져오기
  useEffect(() => {
    setLikedMovies([]);
    let isCurrent = true;
    const fetchLikedMovies = async () => {
      if (!user || !user.id) return;
      setLikedMoviesLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-movies`, { withCredentials: true });
        if (response.data.success && isCurrent) {
          setLikedMovies(response.data.data);
        }
      } catch (error) {
        console.error('fetchLikedMovies: 찜한 영화 목록 조회 실패:', error);
        console.error('fetchLikedMovies: 에러 응답:', error.response?.data);
      } finally {
        if (isCurrent) setLikedMoviesLoading(false);
      }
    };
    fetchLikedMovies();
    return () => { isCurrent = false; };
  }, [user?.id, nickname]);

  // 좋아요한 배우 목록 가져오기
  useEffect(() => {
    setLikedActors([]);
    let isCurrent = true;
    const fetchLikedActors = async () => {
      if (!user || !user.id) return;
      setLikedActorsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-actors`, { withCredentials: true });
        if (response.data.success && isCurrent) {
          setLikedActors(response.data.data);
        }
      } catch (error) {
        console.error('좋아요한 배우 목록 조회 실패:', error);
      } finally {
        if (isCurrent) setLikedActorsLoading(false);
      }
    };
    fetchLikedActors();
    return () => { isCurrent = false; };
  }, [user?.id, nickname]);

  // 좋아요한 감독 목록 가져오기
  useEffect(() => {
    setLikedDirectors([]);
    let isCurrent = true;
    const fetchLikedDirectors = async () => {
      if (!user || !user.id) return;
      setLikedDirectorsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-directors`, { withCredentials: true });
        if (response.data.success && isCurrent) {
          setLikedDirectors(response.data.data);
        }
      } catch (error) {
        console.error('좋아요한 감독 목록 조회 실패:', error);
      } finally {
        if (isCurrent) setLikedDirectorsLoading(false);
      }
    };
    fetchLikedDirectors();
    return () => { isCurrent = false; };
  }, [user?.id, nickname]);

  useEffect(() => {
    setMyComments([]);
    let isCurrent = true;
    const fetchMyComments = async () => {
      if (!user || !user.id) return;
      setMyCommentsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/my-comments`, { withCredentials: true });
        if (response.data.success && isCurrent) {
          setMyComments(response.data.data);
        }
      } catch (error) {
        console.error('내가 작성한 코멘트 목록 조회 실패:', error);
      } finally {
        if (isCurrent) setMyCommentsLoading(false);
      }
    };
    fetchMyComments();
    return () => { isCurrent = false; };
  }, [user?.id, nickname]);

  // 좋아요한 리뷰(코멘트) 목록 가져오기
  useEffect(() => {
    setLikedComments([]);
    let isCurrent = true;
    const fetchLikedComments = async () => {
      if (!user || !user.id) return;
      setLikedCommentsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-reviews`, { withCredentials: true });
        if (response.data.success && isCurrent) {
          setLikedComments(response.data.data);
        }
      } catch (error) {
        console.error('좋아요한 리뷰 목록 조회 실패:', error);
      } finally {
        if (isCurrent) setLikedCommentsLoading(false);
      }
    };
    fetchLikedComments();
    return () => { isCurrent = false; };
  }, [user?.id, nickname]);

  // 팔로우 상태 확인
  useEffect(() => {
    const checkFollowing = async () => {
      if (!user || !currentUser || user.id === currentUser.id) return;
      try {
        const res = await axios.get(`http://localhost:80/api/users/${currentUser.id}/following`, { withCredentials: true });
        if (res.data && res.data.data) {
          setIsFollowing(res.data.data.some(u => u.id === user.id));
        }
      } catch (e) {
        setIsFollowing(false);
      }
    };
    checkFollowing();
  }, [user, currentUser]);

  // 팔로우 상태 확인 함수 (외부에서 호출 가능)
  const checkFollowing = async () => {
    if (!user || !currentUser || user.id === currentUser.id) return;
    try {
      const res = await axios.get(`http://localhost:80/api/users/${currentUser.id}/following`, { withCredentials: true });
      if (res.data && res.data.data) {
        setIsFollowing(res.data.data.some(u => u.id === user.id));
      }
    } catch (e) {
      setIsFollowing(false);
    }
  };

  // 팔로워/팔로잉 목록 불러오기
  useEffect(() => {
    if (!user) return;
    const fetchFollowData = async () => {
      try {
        const [followersRes, followingRes] = await Promise.all([
          axios.get(`http://localhost:80/api/users/${user.id}/followers`, { withCredentials: true }),
          axios.get(`http://localhost:80/api/users/${user.id}/following`, { withCredentials: true })
        ]);
        setFollowers(followersRes.data.data || []);
        setFollowing(followingRes.data.data || []);
      } catch (e) {
        setFollowers([]);
        setFollowing([]);
      }
    };
    fetchFollowData();
  }, [user]);

  const handleTagChange = (tag) => {
    setSelectedTags(prev => {
      if (prev.includes(tag)) {
        return prev.filter(name => name !== tag);
      } else {
        if (prev.length >= 4) {
          alert('최대 4개까지만 선택할 수 있습니다.');
          return prev;
        }
        return [...prev, tag];
      }
    });
  };

  const handleSaveTags = async () => {
    setSaving(true);
    try {
      // genreTags에 존재하는 selectedTags만 저장
      const validTags = selectedTags.filter(tag => genreTags.includes(tag));
      console.log('[선호태그 저장] 저장할 태그:', validTags);
      console.log('[선호태그 저장] 사용자 ID:', user.id);
      
      await axios.put(`http://localhost:80/api/users/${user.id}/preferred-tags`, validTags);
      console.log('[선호태그 저장] 백엔드 저장 완료');
      
      setPreferredTags(validTags);
      setEditMode(false);
      alert('선호 태그가 저장되었습니다!');
      
      // 메인페이지의 추천 결과 새로고침을 위한 이벤트 발생
      window.dispatchEvent(new CustomEvent('preferredTagsUpdated', { 
        detail: { userId: user.id, tags: validTags } 
      }));
      
      // 백엔드 캐시 강제 삭제 (확실성을 위해)
      try {
        await axios.post(`http://localhost:80/api/users/${user.id}/clear-recommendation-cache`, {}, { withCredentials: true });
        console.log('추천 캐시 강제 삭제 완료');
      } catch (cacheError) {
        console.error('캐시 삭제 실패:', cacheError);
      }
    } catch (e) {
      alert('저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setSelectedTags(preferredTags);
    setEditMode(false);
  };

  const handleGoHome = () => {
    navigate('/');
  };

  const handleFollow = async () => {
    setFollowLoading(true);
    try {
      console.log('팔로우 버튼 클릭됨', { user, currentUser });
      const res = await axios.post(`http://localhost:80/api/users/${user.id}/follow`, {}, { withCredentials: true });
      console.log('팔로우 POST 응답:', res.data);
      // 응답값에서 바로 최신 목록 반영
      setFollowers(res.data.followers || []);
      setFollowing(res.data.following || []);
      await checkFollowing();
      console.log('팔로우 후 setState 직전:', {
        followers: res.data.followers,
        following: res.data.following
      });
      setTimeout(() => {
        console.log('팔로우 후 setState 적용 후:', {
          isFollowing,
          followers,
          following
        });
      }, 500);
    } catch (e) {
      console.error('팔로우 에러:', e);
      alert('팔로우에 실패했습니다.');
    } finally {
      setFollowLoading(false);
    }
  };

  const handleUnfollow = async () => {
    setFollowLoading(true);
    try {
      console.log('언팔로우 버튼 클릭됨', { user, currentUser });
      const res = await axios.delete(`http://localhost:80/api/users/${user.id}/unfollow`, { withCredentials: true });
      console.log('언팔로우 DELETE 응답:', res.data);
      // 응답값에서 바로 최신 목록 반영
      setFollowers(res.data.followers || []);
      setFollowing(res.data.following || []);
      await checkFollowing();
      console.log('언팔로우 후 setState 직전:', {
        followers: res.data.followers,
        following: res.data.following
      });
      setTimeout(() => {
        console.log('언팔로우 후 setState 적용 후:', {
          isFollowing,
          followers,
          following
        });
      }, 500);
    } catch (e) {
      console.error('언팔로우 에러:', e);
      alert('언팔로우에 실패했습니다.');
    } finally {
      setFollowLoading(false);
    }
  };

  if (loading) return (
    <div className="user-page-container">
      <div className="loading-spinner">로딩 중...</div>
    </div>
  );
  
  if (error) return (
    <div className="user-page-container">
      <div className="error-message">{error}</div>
    </div>
  );
  
  if (!user) return (
    <div className="user-page-container">
      <div className="error-message">유저 정보가 없습니다.</div>
    </div>
  );

  return (
    <div className="user-page-container">
      <div className="user-profile-card">
        <div className="profile-header" style={{ justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
            <div className="profile-avatar">
              <span>{user.nickname.charAt(0).toUpperCase()}</span>
            </div>
            <h2 className="profile-title">마이페이지</h2>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {/* 본인 페이지가 아니면 팔로우/언팔로우 버튼 노출 */}
            {currentUser && user.id !== currentUser.id && (
              isFollowing ? (
                <button className="unfollow-button" onClick={handleUnfollow} disabled={followLoading}>
                  {followLoading ? '처리 중...' : '언팔로우'}
                </button>
              ) : (
                <button className="follow-button" onClick={handleFollow} disabled={followLoading}>
                  {followLoading ? '처리 중...' : '팔로우'}
                </button>
              )
            )}
            <button className="home-button" onClick={handleGoHome}>홈으로</button>
          </div>
        </div>
        
        <div className="profile-info">
          <div className="info-item">
            <label>닉네임</label>
            <span>{user.nickname}</span>
          </div>
          <div className="info-item">
            <label>이메일</label>
            <span>{user.email}</span>
          </div>
          <div className="follow-stats" style={{ display: 'flex', gap: '20px', marginTop: 8 }}>
            <span style={{ cursor: 'pointer', color: '#6c63ff', fontWeight: 600 }} onClick={() => { setFollowModalType('following'); setShowFollowModal(true); }}>
              팔로잉 {following.length}
            </span>
            <span style={{ cursor: 'pointer', color: '#6c63ff', fontWeight: 600 }} onClick={() => { setFollowModalType('followers'); setShowFollowModal(true); }}>
              팔로워 {followers.length}
            </span>
          </div>
        </div>

        <div className="preferred-tags-section">
          <div className="section-header">
            <h3>선호 태그</h3>
            {/* 본인만 수정 버튼 노출 */}
            {!editMode && user && user.id && currentUser && user.id === currentUser.id && (
              <button 
                className="edit-button"
                onClick={() => setEditMode(true)}
              >
                수정
              </button>
            )}
          </div>
          
          {/* 본인만 태그 수정 UI 노출 */}
          {!editMode ? (
            <div className="tags-display">
              {preferredTags.length === 0 ? (
                <div className="no-tags">
                  <span>설정된 선호 태그가 없습니다.</span>
                  <p>수정 버튼을 눌러서 선호하는 장르 태그를 설정해보세요!</p>
                </div>
              ) : (
                <div className="tag-categories">
                  <div className="tag-category">
                    <h4>장르</h4>
                    <div className="tag-tags">
                      {preferredTags.map(tag => (
                        <span key={tag} className="tag-tag">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            currentUser && user.id === currentUser.id && (
              <div className="tags-edit">
                <div style={{ marginBottom: 10, color: '#a18cd1', fontWeight: 'bold', fontSize: '15px' }}>
                  선택: {selectedTags.filter(tag => genreTags.includes(tag)).length}/4
                </div>
                <div className="tag-selection">
                  {genreTags.map(tag => (
                    <label key={tag} className="tag-checkbox">
                      <input
                        type="checkbox"
                        checked={selectedTags.includes(tag)}
                        onChange={() => handleTagChange(tag)}
                      />
                      <span className="checkbox-label">{tag}</span>
                    </label>
                  ))}
                </div>
                <div className="edit-actions">
                  <button 
                    className="save-button"
                    onClick={handleSaveTags}
                    disabled={saving}
                  >
                    {saving ? '저장 중...' : '저장'}
                  </button>
                  <button 
                    className="cancel-button"
                    onClick={handleCancel}
                    disabled={saving}
                  >
                    취소
                  </button>
                </div>
              </div>
            )
          )}
        </div>

        {/* '내 예매목록 보기' 버튼을 선호태그 바로 아래에 위치 */}
        <button
          style={{
            background: '#1976d2',
            color: '#fff',
            border: 'none',
            borderRadius: 8,
            padding: '12px 24px',
            fontWeight: 600,
            fontSize: 18,
            cursor: 'pointer',
            margin: '0 0 24px 0',
            display: 'block',
          }}
          onClick={() => setShowReservations(true)}
        >
          내 예매목록 보기
        </button>

        {/* 예매 목록 모달 */}
        {showReservations && (
          <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            width: '100vw',
            height: '100vh',
            background: 'rgba(0,0,0,0.5)',
            zIndex: 2000,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          onClick={e => {
            if (e.target === e.currentTarget) {
              setShowReservations(false);
              setSelectedReservationId(null);
            }
          }}
          >
            <div style={{
              background: '#f7f7fa',
              borderRadius: 16,
              padding: 32,
              minWidth: 800,
              maxWidth: 1000,
              maxHeight: '80vh',
              overflowY: 'auto',
              boxShadow: '0 4px 24px rgba(0,0,0,0.18)',
              position: 'relative',
            }}>
              <button
                onClick={() => {
                  setShowReservations(false);
                  setSelectedReservationId(null);
                }}
                style={{
                  position: 'absolute',
                  top: 16,
                  right: 16,
                  background: 'transparent',
                  border: 'none',
                  fontSize: 24,
                  fontWeight: 700,
                  color: '#888',
                  cursor: 'pointer',
                  zIndex: 10,
                }}
                aria-label="닫기"
              >
                ×
              </button>
              {!selectedReservationId ? (
                <UserReservations 
                  currentUser={user}
                  onSelectReservation={(reservationId) => {
                    setSelectedReservationId(reservationId);
                  }}
                />
              ) : (
                <ReservationReceipt 
                  reservationId={selectedReservationId} 
                  onBack={() => setSelectedReservationId(null)} 
                  currentUser={user} 
                />
              )}
            </div>
          </div>
        )}

        {/* 찜한 영화 섹션 */}
        <div className="liked-movies-section">
          <div className="section-header">
            <h3>내가 찜한 영화</h3>
            <span className="movie-count">({likedMovies.length}개)</span>
          </div>
          
          {(() => {
            console.log('찜한 영화 섹션 렌더링:', { 
              likedMoviesLoading, 
              likedMoviesLength: likedMovies.length, 
              likedMovies,
              currentUser,
              nickname
            });
            
            if (likedMoviesLoading) {
              return <div className="loading-movies">찜한 영화를 불러오는 중...</div>;
            } else if (likedMovies.length === 0) {
              return (
                <div className="no-liked-movies">
                  <span>아직 찜한 영화가 없습니다.</span>
                  <p>영화 목록에서 마음에 드는 영화를 찜해보세요!</p>
                </div>
              );
            } else {
              return (
                <div className="liked-movies-grid">
                  {likedMovies.map(movie => (
                    <div
                      key={movie.movieCd}
                      className="liked-movie-card"
                      onClick={() => onMovieClick && onMovieClick(movie)}
                      style={{ cursor: 'pointer' }}
                    >
                      <div className="movie-poster">
                        {movie.posterUrl ? (
                          <img src={movie.posterUrl} alt={movie.movieNm} />
                        ) : (
                          <div className="no-poster">No Poster</div>
                        )}
                      </div>
                      <div className="movie-info">
                        <h4 className="movie-title">{movie.movieNm}</h4>
                        <p className="movie-genre">{movie.genreNm}</p>
                        <p className="movie-year">{movie.openDt}</p>
                        {movie.averageRating && (
                          <div className="movie-rating">
                            <span className="rating-star">⭐</span>
                            <span className="rating-score">{movie.averageRating.toFixed(1)}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              );
            }
          })()}
        </div>

        {/* 좋아요한 배우 섹션 */}
        <div className="liked-actors-section">
          <div className="section-header">
            <h3>내가 좋아요한 배우</h3>
            <span className="actor-count">({likedActors.length}명)</span>
          </div>
          
          {likedActorsLoading ? (
            <div className="loading-actors">좋아요한 배우를 불러오는 중...</div>
          ) : likedActors.length === 0 ? (
            <div className="no-liked-actors">
              <span>아직 좋아요한 배우가 없습니다.</span>
              <p>배우 상세페이지에서 마음에 드는 배우를 좋아요해보세요!</p>
            </div>
          ) : (
            <div className="liked-actors-grid">
              {likedActors.map(actor => (
                <div
                  key={actor.id}
                  className="liked-actor-card"
                  onClick={() => navigate(`/actor/${actor.id}`)}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="actor-photo">
                    {actor.photoUrl ? (
                      <img src={actor.photoUrl} alt={actor.name} />
                    ) : (
                      <div className="no-photo">No Photo</div>
                    )}
                  </div>
                  <div className="actor-info">
                    <h4 className="actor-name">{actor.name}</h4>
                    <div className="actor-likes">
                      <span className="like-count">♥ {actor.likeCount}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 좋아요한 감독 섹션 */}
        <div className="liked-directors-section">
          <div className="section-header">
            <h3>내가 좋아요한 감독</h3>
            <span className="director-count">({likedDirectors.length}명)</span>
          </div>
          
          {likedDirectorsLoading ? (
            <div className="loading-directors">좋아요한 감독을 불러오는 중...</div>
          ) : likedDirectors.length === 0 ? (
            <div className="no-liked-directors">
              <span>아직 좋아요한 감독이 없습니다.</span>
              <p>감독 상세페이지에서 마음에 드는 감독을 좋아요해보세요!</p>
            </div>
          ) : (
            <div className="liked-directors-grid">
              {likedDirectors.map(director => (
                <div
                  key={director.id}
                  className="liked-director-card"
                  onClick={() => navigate(`/director/${director.id}`)}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="director-photo">
                    {director.photoUrl ? (
                      <img src={director.photoUrl} alt={director.name} />
                    ) : (
                      <div className="no-photo">No Photo</div>
                    )}
                  </div>
                  <div className="director-info">
                    <h4 className="director-name">{director.name}</h4>
                    <div className="director-likes">
                      <span className="like-count">♥ {director.likeCount}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 내가 작성한 코멘트(리뷰) 섹션 */}
        <div className="my-comments-section">
          <div className="section-header">
            <h3>내가 작성한 코멘트</h3>
            <span className="comment-count">({myComments.length}개)</span>
          </div>
          {myCommentsLoading ? (
            <div className="loading-comments">코멘트를 불러오는 중...</div>
          ) : myComments.length === 0 ? (
            <div className="no-my-comments">
              <span>아직 작성한 코멘트가 없습니다.</span>
              <p>영화 상세페이지에서 코멘트를 남겨보세요!</p>
            </div>
          ) : (
            <div className="my-comments-grid">
              {myComments.map(comment => (
                <div
                  key={comment.id}
                  className="my-comment-card"
                  onClick={() => onMovieClick && onMovieClick({
                    movieCd: comment.movieCd,
                    movieNm: comment.movieNm,
                    posterUrl: comment.posterUrl,
                    genreNm: comment.genreNm,
                    openDt: comment.openDt
                  })}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="comment-movie-poster">
                    {comment.posterUrl ? (
                      <img src={comment.posterUrl} alt={comment.movieNm} />
                    ) : (
                      <div className="no-poster">No Poster</div>
                    )}
                  </div>
                                      <div className="comment-info">
                      <h4 className="comment-movie-title">{comment.movieNm}</h4>
                      <div className="comment-meta">
                        <span className="comment-date">{new Date(comment.createdAt).toLocaleDateString()}</span>
                        {comment.rating && (
                          <span className="comment-rating">★ {comment.rating}</span>
                        )}
                        <span className="comment-likes">♥ {comment.likeCount}</span>
                        <span className="comment-replies">💬 {comment.commentCount}</span>
                      </div>
                      <div className="comment-content">{comment.content}</div>
                    </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 내가 좋아요한 코멘트 섹션 */}
        <div className="liked-comments-section">
          <div className="section-header">
            <h3>내가 좋아요한 코멘트</h3>
            <span className="comment-count">({likedComments.length}개)</span>
          </div>
          {likedCommentsLoading ? (
            <div className="loading-comments">코멘트를 불러오는 중...</div>
          ) : likedComments.length === 0 ? (
            <div className="no-liked-comments">
              <span>아직 좋아요한 코멘트가 없습니다.</span>
              <p>영화 상세페이지에서 마음에 드는 코멘트를 좋아요해보세요!</p>
            </div>
          ) : (
            <div className="liked-comments-grid">
              {likedComments.map(comment => (
                <div
                  key={comment.id}
                  className="liked-comment-card"
                  onClick={() => onMovieClick && onMovieClick({
                    movieCd: comment.movieCd,
                    movieNm: comment.movieNm,
                    posterUrl: comment.posterUrl,
                    genreNm: comment.genreNm,
                    openDt: comment.openDt
                  })}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="comment-movie-poster">
                    {comment.posterUrl ? (
                      <img src={comment.posterUrl} alt={comment.movieNm} />
                    ) : (
                      <div className="no-poster">No Poster</div>
                    )}
                  </div>
                  <div className="comment-info">
                    <h4 className="comment-movie-title">{comment.movieNm}</h4>
                    <div className="comment-author">
                      <span className="author-nickname">by {comment.authorNickname}</span>
                    </div>
                    <div className="comment-meta">
                      <span className="comment-date">{new Date(comment.createdAt).toLocaleDateString()}</span>
                      {comment.rating && (
                        <span className="comment-rating">★ {comment.rating}</span>
                      )}
                      <span className="comment-likes">♥ {comment.likeCount}</span>
                      <span className="comment-replies">💬 {comment.commentCount}</span>
                    </div>
                    <div className="comment-content">{comment.content}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>


      {/* 팔로워/팔로잉 모달 */}
      {showFollowModal && (
        <div className="review-modal-overlay" style={{ zIndex: 2000 }}>
          <div className="review-modal" style={{ minWidth: 320, maxWidth: 400, padding: 24 }}>
            <button className="close-btn" onClick={() => setShowFollowModal(false)}>&times;</button>
            <h3 style={{ marginBottom: 16 }}>{followModalType === 'followers' ? '팔로워' : '팔로잉'} 목록</h3>
            <ul style={{ listStyle: 'none', padding: 0, maxHeight: 300, overflowY: 'auto' }}>
              {(followModalType === 'followers' ? followers : following).length === 0 ? (
                <li style={{ color: '#aaa' }}>아직 없습니다.</li>
              ) : (
                (followModalType === 'followers' ? followers : following).map(u => (
                  <li key={u.id} style={{ marginBottom: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ width: 32, height: 32, borderRadius: '50%', background: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, color: '#6c63ff' }}>{u.nickname?.charAt(0)?.toUpperCase()}</div>
                    <span
                      style={{ cursor: 'pointer', color: '#6c63ff', textDecoration: 'underline' }}
                      onClick={() => {
                        setShowFollowModal(false);
                        navigate(`/user/${u.nickname}`);
                      }}
                    >
                      {u.nickname}
                    </span>
                  </li>
                ))
              )}
            </ul>
          </div>
          <style>{`
            .review-modal-overlay { position: fixed; left: 0; top: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.2); z-index: 1000; display: flex; align-items: center; justify-content: center; }
            .review-modal { background: white; border-radius: 16px; padding: 32px; min-width: 320px; max-width: 400px; box-shadow: 0 4px 32px rgba(0,0,0,0.15); position: relative; }
            .close-btn { position: absolute; right: 16px; top: 16px; background: #eee; border: none; border-radius: 50%; width: 32px; height: 32px; font-size: 22px; cursor: pointer; }
          `}</style>
        </div>
      )}
    </div>
  );
};

export default UserPage; 