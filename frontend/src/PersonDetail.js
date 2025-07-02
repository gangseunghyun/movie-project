import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

const PersonDetail = ({ type }) => {
  const { id } = useParams(); // type은 props로 받고, id만 useParams에서 가져옴
  const navigate = useNavigate();
  const [person, setPerson] = useState(null);
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [likedByMe, setLikedByMe] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [likeLoading, setLikeLoading] = useState(false);

  // 좋아요 상태 불러오기 함수로 분리
  const fetchLikeStatus = async () => {
    try {
      const res = await axios.get(`http://localhost:80/api/person/${type}/${id}/like-status`, { withCredentials: true });
      setLikedByMe(res.data.likedByMe);
      setLikeCount(res.data.likeCount);
    } catch (err) {
      setLikedByMe(false);
    }
  };

  useEffect(() => {
    if (!type || !id) return; // type이나 id가 없으면 API 호출하지 않음
    setLoading(true);
    axios.get(`http://localhost:80/api/person/${type}/${id}`)
      .then(res => {
        setPerson(res.data.person);
        setMovies(res.data.movies);
      })
      .catch(() => setError('정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [type, id]);

  // 상세페이지 진입 시 항상 서버에서 좋아요 상태 받아오기
  useEffect(() => {
    if (!type || !id) return;
    fetchLikeStatus();
    // eslint-disable-next-line
  }, [type, id]);

  // 좋아요 토글 함수
  const handleLikeToggle = async () => {
    if (likeLoading) return;
    setLikeLoading(true);
    try {
      if (likedByMe) {
        await axios.delete(`http://localhost:80/api/person/${type}/${id}/like`, { withCredentials: true });
      } else {
        await axios.post(`http://localhost:80/api/person/${type}/${id}/like`, {}, { withCredentials: true });
      }
      // 버튼 클릭 후에도 항상 서버에서 최신 상태를 다시 받아옴
      await fetchLikeStatus();
    } catch (err) {
      if (err.response?.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('좋아요 처리에 실패했습니다.');
      }
    } finally {
      setLikeLoading(false);
    }
  };

  if (loading) return <div style={{padding:40}}>로딩 중...</div>;
  if (error) return <div style={{padding:40, color:'red'}}>{error}</div>;
  if (!person) return null;

  return (
    <div style={{maxWidth:900, margin:'40px auto', background:'#fff', borderRadius:12, boxShadow:'0 2px 12px rgba(0,0,0,0.07)', padding:32}}>
      {/* 홈으로 버튼 */}
      <div style={{marginBottom: 24, display: 'flex', justifyContent: 'flex-start'}}>
        <button
          onClick={() => navigate('/')}
          style={{
            padding: '10px 20px',
            backgroundColor: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontSize: '14px',
            fontWeight: '600',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            transition: 'all 0.3s ease',
            boxShadow: '0 2px 8px rgba(102, 126, 234, 0.2)'
          }}
          onMouseEnter={(e) => {
            e.target.style.backgroundColor = '#5a6fd8';
            e.target.style.transform = 'translateY(-2px)';
            e.target.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.3)';
          }}
          onMouseLeave={(e) => {
            e.target.style.backgroundColor = '#667eea';
            e.target.style.transform = 'translateY(0)';
            e.target.style.boxShadow = '0 2px 8px rgba(102, 126, 234, 0.2)';
          }}
        >
          <span style={{fontSize: '16px'}}>🏠</span>
          <span>홈으로</span>
        </button>
      </div>
      <div style={{display:'flex', alignItems:'center', gap:32}}>
        <img src={person.photoUrl || '/placeholder-actor.png'} alt={person.peopleNm} style={{width:120, height:120, borderRadius:'50%', objectFit:'cover', background:'#eee'}} />
        <div style={{flex: 1}}>
          <h1 style={{margin:0, fontSize:32}}>{person.name || person.peopleNm}</h1>
          <div style={{color:'#888', fontSize:20, marginTop:8}}>{type === 'director' ? '감독' : '배우'}</div>
          {/* 좋아요 버튼 */}
          <div style={{marginTop: 16}}>
            <button
              onClick={handleLikeToggle}
              disabled={likeLoading}
              style={{
                padding: '12px 24px',
                backgroundColor: likedByMe ? '#ff6b6b' : '#f8f9fa',
                color: likedByMe ? 'white' : '#333',
                border: `2px solid ${likedByMe ? '#ff6b6b' : '#dee2e6'}`,
                borderRadius: '25px',
                fontSize: '16px',
                fontWeight: '600',
                cursor: likeLoading ? 'not-allowed' : 'pointer',
                transition: 'all 0.3s ease',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                opacity: likeLoading ? 0.7 : 1
              }}
              onMouseEnter={(e) => {
                if (!likeLoading) {
                  e.target.style.transform = 'translateY(-2px)';
                  e.target.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
                }
              }}
              onMouseLeave={(e) => {
                if (!likeLoading) {
                  e.target.style.transform = 'translateY(0)';
                  e.target.style.boxShadow = 'none';
                }
              }}
            >
              {likeLoading ? (
                <span>처리중...</span>
              ) : (
                <>
                  <span style={{fontSize: '18px'}}>
                    {likedByMe ? '❤️' : '🤍'}
                  </span>
                  <span>
                    {likedByMe ? '좋아요 취소' : '좋아요'}
                  </span>
                  <span style={{
                    backgroundColor: likedByMe ? 'rgba(255,255,255,0.2)' : '#e9ecef',
                    color: likedByMe ? 'white' : '#6c757d',
                    padding: '4px 8px',
                    borderRadius: '12px',
                    fontSize: '12px',
                    fontWeight: 'bold'
                  }}>
                    {likeCount}
                  </span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
      <hr style={{margin:'32px 0'}} />
      <h2 style={{fontSize:22, marginBottom:16}}>영화</h2>
      <div style={{fontWeight:600, marginBottom:8}}>{type === 'director' ? '감독' : '출연'}</div>
      <table style={{width:'100%', borderCollapse:'collapse'}}>
        <thead>
          <tr style={{borderBottom:'1px solid #eee', color:'#888', fontWeight:500}}>
            <th style={{padding:'8px 0', width:80}}></th>
            <th style={{padding:'8px 0', textAlign:'left'}}>제목</th>
            <th style={{padding:'8px 0'}}>역할</th>
            <th style={{padding:'8px 0'}}>평가</th>
          </tr>
        </thead>
        <tbody>
          {movies.map((movie, idx) => (
            <tr key={movie.id || idx} style={{borderBottom:'1px solid #f5f5f5'}}>
              <td style={{padding:'8px 0'}}>
                {movie.posterUrl ? <img src={movie.posterUrl} alt={movie.movieNm} style={{width:56, height:80, objectFit:'cover', borderRadius:8}} /> : null}
              </td>
              <td style={{padding:'8px 0', fontWeight:500}}>{movie.movieNm}</td>
              <td style={{padding:'8px 0', color:'#888'}}>{type === 'director' ? '감독' : (movie.roleTypeKor || '출연')}</td>
              <td style={{padding:'8px 0', color:'#f39c12', fontWeight:600}}>
                {movie.averageRating ? `평균★ ${movie.averageRating.toFixed(1)}` : ''}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default PersonDetail; 