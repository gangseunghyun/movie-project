// MovieDetailPage.jsx (상위 컴포넌트)
import React, { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import MovieDetailHeader from './MovieDetailHeader';
import MovieDetailBody from './MovieDetailBody';
import styles from './MovieDetailPage.module.css';

export default function MovieDetailPage() {
  const { movieCd } = useParams();
  const [movieDetail, setMovieDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // 코멘트 상태 및 fetch 함수 상위에서 관리
  const [comments, setComments] = useState([]);
  const [commentLoading, setCommentLoading] = useState(false);
  const [commentError, setCommentError] = useState(null);

  // 영화 상세 정보를 API에서 가져오는 함수
  const fetchMovieDetail = useCallback(async () => {
    if (!movieCd) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`/api/movie-detail-dto?movieCd=${movieCd}`, {
        credentials: 'include',
      });
      
      const result = await response.json();
      
              if (result.data && result.data.length > 0) {
          const movieData = result.data[0];
          console.log('API에서 받은 영화 데이터:', movieData);
          console.log('배우 데이터:', movieData.actors);
          console.log('첫 번째 배우 상세:', movieData.actors?.[0]);
        
        // API 데이터를 기존 형식에 맞게 변환
        const transformedData = {
          movieCd: movieData.movieCd,
          movieNm: movieData.movieNm || '-',
          movieNmEn: movieData.movieNmEn || '-',
          openDt: movieData.openDt || '-',
          genreNm: movieData.genreNm || '-',
          nationNm: movieData.nationNm || '-',
          rank: movieData.rank || movieData.reservationRank || '-',
          reservationRate: formatReservationRate(movieData.reservationRate),
          watchGradeNm: movieData.watchGradeNm || '-',
          posterUrl: movieData.posterUrl,
          status: movieData.status || '-',
          description: movieData.description || '-',
          showTm: movieData.showTm || '-',
          companyNm: movieData.companyNm || '-',
          averageRating: movieData.averageRating || '-',
          audienceCount: formatAudienceCount(movieData.audiAcc || movieData.totalAudience),
          directors: movieData.directors || [],
          actors: movieData.actors || [],
          stillcuts: movieData.stillcuts || [],
          likedByMe: movieData.likedByMe || false
        };
        
        setMovieDetail(transformedData);
      } else {
        setError('영화 정보를 찾을 수 없습니다.');
      }
    } catch (err) {
      console.error('영화 상세 정보 조회 실패:', err);
      setError('영화 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [movieCd]);

  const fetchComments = useCallback(() => {
    if (!movieCd) return;
    setCommentLoading(true);
    setCommentError(null);
    fetch(`/api/reviews/movie/${movieCd}`, {
      credentials: 'include',
    })
      .then(res => res.json())
      .then(data => {
        if (data.success) {
          setComments(data.data || []);
          //console.log(data.data);
        } else {
          setCommentError(data.message || '코멘트 불러오기 실패');
        }
      })
      .catch(() => setCommentError('코멘트 불러오기 실패'))
      .finally(() => setCommentLoading(false));
  }, [movieCd]);

  function formatAudienceCount(count) {
    if (typeof count === 'string') count = parseInt(count, 10);
    if (isNaN(count)) return '-';
    if (count >= 10000) {
      return (count / 10000).toFixed(1).replace(/\.0$/, '') + '만명';
    }
    return count.toLocaleString() + '명';
  }

  function formatReservationRate(rate) {
    if (rate === undefined || rate === null || rate === '-') return '-';
    const str = String(rate).trim();
    if (str.endsWith('%')) return str;
    const num = Number(str);
    if (isNaN(num)) return '-';
    return num.toFixed(1) + '%';
  }

  useEffect(() => {

    if (!movieCd) return;
    
    // 페이지 진입 시 스크롤 맨 위로
    window.scrollTo(0, 0);
    
    // API에서 영화 상세 정보 조회
    fetchMovieDetail();
  }, [movieCd, fetchMovieDetail]);

  // movieCd가 바뀔 때마다 코멘트 새로고침
  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  if (loading) return <div className={styles.loading}>로딩중...</div>;
  if (error) return <div className={styles.error}>{error}</div>;
  if (!movieDetail) return null;

  return (
    <div className={styles.outer}>
      <MovieDetailHeader 
        movieDetail={movieDetail} 
        onCommentSaved={fetchComments} 
        onRefreshMovieDetail={fetchMovieDetail}
      />
      <MovieDetailBody
        actors={movieDetail.actors}
        directors={movieDetail.directors}
        stillcuts={movieDetail.stillcuts}
        movieCd={movieDetail.movieCd}
        movieId={movieDetail.movieId || movieDetail.id}
        comments={comments}
        setComments={setComments}
        commentLoading={commentLoading}
        commentError={commentError}
        fetchComments={fetchComments}
      />
    </div>
  );
}