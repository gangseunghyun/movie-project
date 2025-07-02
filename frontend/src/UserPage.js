import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  getUserByNickname, 
  getUserPreferredTags, 
  getGenreTags
} from './services/userService';
import axios from 'axios';
import './UserPage.css';

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
    const fetchLikedMovies = async () => {
      if (!user || !currentUser) {
        console.log('fetchLikedMovies: user 또는 currentUser가 없음', { user, currentUser });
        return;
      }
      
      // 본인의 마이페이지인지 확인
      if (currentUser.nickname !== nickname) {
        console.log('fetchLikedMovies: 본인의 마이페이지가 아님', { 
          currentUserNickname: currentUser.nickname, 
          pageNickname: nickname 
        });
        return;
      }
      
      console.log('fetchLikedMovies: 찜한 영화 목록 조회 시작', { userId: user.id });
      setLikedMoviesLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-movies`, { 
          withCredentials: true 
        });
        console.log('fetchLikedMovies: API 응답', response.data);
        if (response.data.success) {
          setLikedMovies(response.data.data);
          console.log('fetchLikedMovies: 찜한 영화 설정 완료', response.data.data);
        }
      } catch (error) {
        console.error('fetchLikedMovies: 찜한 영화 목록 조회 실패:', error);
        console.error('fetchLikedMovies: 에러 응답:', error.response?.data);
      } finally {
        setLikedMoviesLoading(false);
      }
    };
    
    fetchLikedMovies();
  }, [user, currentUser, nickname]);

  // 좋아요한 배우 목록 가져오기
  useEffect(() => {
    const fetchLikedActors = async () => {
      if (!user || !currentUser || currentUser.nickname !== nickname) return;
      
      setLikedActorsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-actors`, { 
          withCredentials: true 
        });
        if (response.data.success) {
          setLikedActors(response.data.data);
        }
      } catch (error) {
        console.error('좋아요한 배우 목록 조회 실패:', error);
      } finally {
        setLikedActorsLoading(false);
      }
    };
    
    fetchLikedActors();
  }, [user, currentUser, nickname]);

  // 좋아요한 감독 목록 가져오기
  useEffect(() => {
    const fetchLikedDirectors = async () => {
      if (!user || !currentUser || currentUser.nickname !== nickname) return;
      
      setLikedDirectorsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-directors`, { 
          withCredentials: true 
        });
        if (response.data.success) {
          setLikedDirectors(response.data.data);
        }
      } catch (error) {
        console.error('좋아요한 감독 목록 조회 실패:', error);
      } finally {
        setLikedDirectorsLoading(false);
      }
    };
    
    fetchLikedDirectors();
  }, [user, currentUser, nickname]);

  useEffect(() => {
    const fetchMyComments = async () => {
      if (!user || !currentUser || currentUser.nickname !== nickname) return;
      setMyCommentsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/my-comments`, { withCredentials: true });
        if (response.data.success) {
          setMyComments(response.data.data);
        }
      } catch (error) {
        console.error('내가 작성한 코멘트 목록 조회 실패:', error);
      } finally {
        setMyCommentsLoading(false);
      }
    };
    fetchMyComments();
  }, [user, currentUser, nickname]);

  // 좋아요한 리뷰(코멘트) 목록 가져오기
  useEffect(() => {
    const fetchLikedComments = async () => {
      if (!user || !currentUser || currentUser.nickname !== nickname) return;
      setLikedCommentsLoading(true);
      try {
        const response = await axios.get(`http://localhost:80/api/users/${user.id}/liked-reviews`, { withCredentials: true });
        if (response.data.success) {
          setLikedComments(response.data.data);
        }
      } catch (error) {
        console.error('좋아요한 리뷰 목록 조회 실패:', error);
      } finally {
        setLikedCommentsLoading(false);
      }
    };
    fetchLikedComments();
  }, [user, currentUser, nickname]);

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
      await axios.put(`http://localhost:80/api/users/${user.id}/preferred-tags`, validTags);
      setPreferredTags(validTags);
      setEditMode(false);
      alert('선호 태그가 저장되었습니다!');
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
          <button className="home-button" onClick={handleGoHome}>홈으로</button>
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
        </div>

        <div className="preferred-tags-section">
          <div className="section-header">
            <h3>선호 태그</h3>
            {!editMode && user && user.id && (
              <button 
                className="edit-button"
                onClick={() => setEditMode(true)}
              >
                수정
              </button>
            )}
          </div>
          
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
          )}
        </div>

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
                    </div>
                    <div className="comment-content">{comment.content}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserPage; 