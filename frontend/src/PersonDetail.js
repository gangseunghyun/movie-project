import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';

const PersonDetail = ({ type }) => {
  const { id } = useParams(); // type은 props로 받고, id만 useParams에서 가져옴
  const [person, setPerson] = useState(null);
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  if (loading) return <div style={{padding:40}}>로딩 중...</div>;
  if (error) return <div style={{padding:40, color:'red'}}>{error}</div>;
  if (!person) return null;

  return (
    <div style={{maxWidth:900, margin:'40px auto', background:'#fff', borderRadius:12, boxShadow:'0 2px 12px rgba(0,0,0,0.07)', padding:32}}>
      <div style={{display:'flex', alignItems:'center', gap:32}}>
        <img src={person.photoUrl || '/placeholder-actor.png'} alt={person.peopleNm} style={{width:120, height:120, borderRadius:'50%', objectFit:'cover', background:'#eee'}} />
        <div>
          <h1 style={{margin:0, fontSize:32}}>{person.name || person.peopleNm}</h1>
          <div style={{color:'#888', fontSize:20, marginTop:8}}>{type === 'director' ? '감독' : '배우'}</div>
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