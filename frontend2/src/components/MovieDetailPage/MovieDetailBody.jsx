import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import styles from './MovieDetailBody.module.css';
// assets 이미지 import
import banner1 from '../../assets/banner1.jpg';
import banner2 from '../../assets/banner2.jpg';
import banner3 from '../../assets/banner3.jpg';
import banner4 from '../../assets/banner4.jpg';
import userIcon from '../../assets/user_icon.png';
import userProfile from '../../assets/user_profile.png';
import previousIcon from '../../assets/previous_icon.png';
import nextIcon from '../../assets/next_icon.png';
import MovieHorizontalSlider from '../MainPage/MovieHorizontalSlider';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import commentIcon2 from '../../assets/comment_icon2.png';
import shareIcon from '../../assets/share_icon.png';
import ReplyModal from '../Modal/ReplyModal';
import CommentDetailModal from '../Modal/CommentDetailModal';
import { useUser } from '../../contexts/UserContext';
import CommentModal from '../Modal/CommentModal';
import AllCommentsModal from '../Modal/AllCommentsModal';


const dummySimilar = [
  { id: 1, title: '비슷한 영화 1', posterUrl: banner1 },
  { id: 2, title: '비슷한 영화 2', posterUrl: banner2 },
  { id: 3, title: '비슷한 영화 3', posterUrl: banner3 },
  { id: 4, title: '비슷한 영화 4', posterUrl: banner4 },
  { id: 5, title: '비슷한 영화 5', posterUrl: banner1 },
  { id: 6, title: '비슷한 영화 6', posterUrl: banner2 },
  { id: 7, title: '비슷한 영화 4', posterUrl: banner4 },
  { id: 8, title: '비슷한 영화 5', posterUrl: banner1 },
  { id: 9, title: '비슷한 영화 6', posterUrl: banner2 },
];

function SimilarMovieCard({ movie }) {
  return (
    <div className={styles.similarMovieCard}>
      <img 
        src={movie.posterUrl || movie.posterImageUrl || banner1} 
        alt={movie.title || movie.movieNm} 
        className={styles.similarPoster} 
      />
      <div className={styles.similarTitle}>{movie.title || movie.movieNm}</div>
    </div>
  );
}
function StillcutCard({ still }) {
  return (
    <div className={styles.stillcutCard}>
      <img src={still.imageUrl} alt="스틸컷" className={styles.stillcutImg} />
    </div>
  );
}



export default function MovieDetailBody({ actors, directors, stillcuts, movieCd, comments, commentLoading, commentError, fetchComments }) {

  const [castPage, setCastPage] = useState(0);
  const [replyModalOpen, setReplyModalOpen] = useState(false);
  const [selectedReviewId, setSelectedReviewId] = useState(null);
  const [commentDetailModalOpen, setCommentDetailModalOpen] = useState(false);
  const [selectedComment, setSelectedComment] = useState(null);
  const { user } = useUser();
  // 수정 모달 상태
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  // 전체 코멘트 모달 상태
  const [allCommentsModalOpen, setAllCommentsModalOpen] = useState(false);
  // 코멘트별 별점 상태
  const [commentRatings, setCommentRatings] = useState({});
  
  // 비슷한 장르 영화 상태 추가
  const [similarMovies, setSimilarMovies] = useState([]);
  const [similarMoviesLoading, setSimilarMoviesLoading] = useState(false);
  const [similarMoviesError, setSimilarMoviesError] = useState(null);

  // AllCommentsModal에서 코멘트 클릭 시 상세 모달로 전환
  const handleAllCommentsCommentClick = (comment) => {
    setSelectedReviewId(comment.id);
    setSelectedComment(comment);
    setAllCommentsModalOpen(false);
    setCommentDetailModalOpen(true);
  };

  // 상세 모달에서 이전(닫기) 버튼 클릭 시 전체 코멘트 모달 다시 열기
  const handleDetailModalClose = () => {
    setCommentDetailModalOpen(false);
    setAllCommentsModalOpen(true);
  };

  // 전체 코멘트 개수
  const totalCommentCount = comments.length; // 필요시 props로 전달받거나 별도 fetch 필요

  // 코멘트별 별점 조회 함수
  const fetchCommentRating = async (commentUserId) => {
    if (!movieCd || !commentUserId) return null;

    try {
      const response = await fetch(`http://localhost:80/api/ratings/${movieCd}`, {
        credentials: 'include',
      });

      if (response.ok) {
        const data = await response.json();
        if (data.success && data.data) {
          return data.data.score;
        }
      }
    } catch (error) {
      console.error('별점 조회 실패:', error);
    }
    return null;
  };

  // 전체 코멘트(무한스크롤/정렬) fetch 함수
  // 실제 API에 맞게 수정 필요
  const fetchAllComments = async ({ page, sort, limit }) => {
    // 예시: /api/reviews?movieCd=xxx&page=1&sort=like&limit=4
    const params = new URLSearchParams({
      movieCd,
      page,
      sort,
      limit,
    });
    const res = await fetch(`/api/reviews?${params.toString()}`, {
      credentials: 'include',
    });
    if (!res.ok) return { comments: [] };
    const data = await res.json();
    console.log(data.data);
    // data: { comments: [], totalCount: number }
    return data;
  };

  // 코멘트 목록이 변경될 때 각 코멘트의 별점 조회
  useEffect(() => {
    const fetchAllCommentRatings = async () => {
      const ratings = {};

      for (const comment of comments) {
        if (comment.userId) {
          const rating = await fetchCommentRating(comment.userId);
          if (rating !== null) {
            ratings[comment.id] = rating;
          }
        }
      }

      setCommentRatings(ratings);
    };

    if (comments.length > 0) {
      fetchAllCommentRatings();
    }
  }, [comments, movieCd]);

  // 작성시간을 상대적 시간으로 변환하는 함수
  const formatRelativeTime = (dateString) => {
    if (!dateString) return '';

    const now = new Date();
    const commentDate = new Date(dateString);

    // 날짜 차이 계산 (밀리초 단위)
    const diffTime = now.getTime() - commentDate.getTime();
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return '오늘';
    } else if (diffDays === 1) {
      return '어제';
    } else if (diffDays < 7) {
      return `${diffDays}일 전`;
    } else {
      // 7일 이상 지난 경우 원래 날짜 형식으로 표시
      return commentDate.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    }
  };

  const directorList = (directors || []).map(d => ({
    id: d.id,
    peopleNm: d.peopleNm,
    photoUrl: d.photoUrl && d.photoUrl.trim() !== '' ? d.photoUrl : userIcon,
    cast: '감독',
    type: 'director', // 반드시 추가!
  }));
  const actorList = (actors || []).map(a => ({
    id: a.id,
    peopleNm: a.peopleNm,
    photoUrl: a.photoUrl && a.photoUrl.trim() !== '' ? a.photoUrl : userIcon,
    cast: '출연',
    type: 'actor', // 반드시 추가!
  }));
  const castList = [...directorList, ...actorList];

  // 슬라이더 세팅
  const castPerPage = 12; // 4x3
  const castTotalPage = Math.ceil(castList.length / castPerPage);
  const castPages = [];
  for (let i = 0; i < castTotalPage; i++) {
    castPages.push(castList.slice(i * castPerPage, (i + 1) * castPerPage));
  }

  const [stillStart, setStillStart] = useState(0);
  const stillVisible = 1; // 한 번에 1장씩
  const stillcutsData = stillcuts || [];
  const stillCardWidth = 1280; // 원하는 카드 width(px)로 맞추세요
  const stillCardGap = 20;    // 카드 사이 gap(px)로 맞추세요

  const handlePrev = () => setStillStart(Math.max(0, stillStart - 1));
  const handleNext = () => setStillStart(Math.min(stillcutsData.length - stillVisible, stillStart + 1));

  // 댓글 상세 모달 핸들러
  const handleCommentCardClick = (reviewId) => {
    const comment = comments.find(c => c.id === reviewId);
    setSelectedReviewId(reviewId);
    setSelectedComment(comment);
    setCommentDetailModalOpen(true);
  };
  // 대댓글(Reply) 모달 핸들러
  const handleReplyIconClick = (e, reviewId) => {
    e.stopPropagation(); // commentCard 클릭 이벤트 버블링 방지
    setSelectedReviewId(reviewId);
    setReplyModalOpen(true);
  };



  // 코멘트 삭제 핸들러
  const handleDelete = (commentId) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      fetch(`/api/reviews/${commentId}`, {
        method: 'DELETE',
        credentials: 'include',
      })
        .then(res => res.json())
        .then(data => {
          if (data.success) {
            fetchComments(); // 삭제 후 목록 갱신
          } else {
            alert('삭제 실패: ' + (data.message || ''));
          }
        })
        .catch(() => alert('삭제 중 오류 발생'));
    }
  };

  // 코멘트 수정 핸들러
  const handleEdit = (comment) => {
    setEditTarget(comment);
    setEditModalOpen(true);
  };

  // 수정 완료 시
  const handleEditSave = () => {
    setEditModalOpen(false);
    fetchComments();
  };

  // 좋아요 클릭 핸들러
  const handleLike = async (commentId, likedByMe) => {
    try {
      let res;
      if (likedByMe) {
        // 좋아요 취소 (DELETE)
        res = await fetch(`http://localhost:80/api/reviews/dto/${commentId}/like`, {
          method: 'DELETE',
          credentials: 'include',
        });
      } else {
        // 좋아요 (POST)
        res = await fetch(`http://localhost:80/api/reviews/dto/${commentId}/like`, {
          method: 'POST',
          credentials: 'include',
        });
      }
      if (res.ok) {
        fetchComments(); // 좋아요 상태 및 카운트 갱신
      } else if (res.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('좋아요 처리 실패');
      }
    } catch (e) {
      alert('네트워크 오류');
    }
  };

  // 비슷한 장르 영화 조회 함수
  const fetchSimilarMovies = async () => {
    if (!movieCd) return;
    
    setSimilarMoviesLoading(true);
    setSimilarMoviesError(null);
    
    try {
      const response = await fetch(`http://localhost:80/data/api/similar-genre-movies?movieCd=${movieCd}&page=0&size=20`, {
        credentials: 'include',
      });
      
      if (response.ok) {
        const data = await response.json();
        if (data.data) {
          //console.log(data.data);
          setSimilarMovies(data.data);
        } else {
          setSimilarMovies([]);
        }
      } else {
        setSimilarMoviesError('비슷한 장르 영화를 불러오는데 실패했습니다.');
        setSimilarMovies([]);
      }
    } catch (error) {
      console.error('비슷한 장르 영화 조회 실패:', error);
      setSimilarMoviesError('네트워크 오류가 발생했습니다.');
      setSimilarMovies([]);
    } finally {
      setSimilarMoviesLoading(false);
    }
  };

  useEffect(() => {
    if (!movieCd) return;
    fetchComments();
    fetchSimilarMovies(); // 비슷한 장르 영화도 함께 조회
  }, [movieCd, fetchComments]);

  // 대댓글 작성 후 코멘트 목록 새로고침
  const handleReplySave = () => {
    fetchComments();
  };

  // 모든 모달을 닫는 함수
  const handleCloseAllModals = () => {
    setCommentDetailModalOpen(false);
    setEditModalOpen(false);
    setReplyModalOpen(false);
    setAllCommentsModalOpen(false);
  };

  return (
    <div className={styles.detailBody}>
      <section>
        <h2>출연/제작</h2>
        <div className={styles.castSliderWrapper}>
          {castPage > 0 && (
            <button className={`${styles.castNavBtn} ${styles.left}`} onClick={() => setCastPage(castPage - 1)}>
              <img src={previousIcon} alt="이전" />
            </button>
          )}
          <div
            className={styles.castSliderTrack}
            style={{ transform: `translateX(-${castPage * 100}%)`, transition: 'transform 0.4s cubic-bezier(0.4, 0, 0.2, 1)' }}
          >
            {castPages.map((pageList, pageIdx) => (
              <div className={`${styles.castGrid} ${pageIdx === 0 ? styles.firstCastGrid : ''}`} key={pageIdx}>
                {pageList.map((person, idx) => {
                  const rowIdx = Math.floor(idx / 4);
                  const isFirstOrSecondRow = rowIdx === 0 || rowIdx === 1;
                  const personLink = person.type === 'director'
                    ? `/person/director/${person.id}`
                    : `/person/actor/${person.id}`;
                  return (
                    <div
                      className={styles.castCard}
                      key={person.id ? `person-${person.type ?? 'unknown'}-${person.id}` : `page-${pageIdx}-idx-${idx}`}
                    >
                      <Link to={personLink} style={{ display: 'block' }}>
                        <img src={person.photoUrl} alt={person.peopleNm} className={styles.castImg} />
                      </Link>
                      <div className={
                        styles.castInfo +
                        (isFirstOrSecondRow ? ' ' + styles.castInfoWithBorder : '')
                      }>
                        <Link to={personLink} style={{ textDecoration: 'none', color: 'inherit' }}>
                          <div className={styles.castName}>{person.peopleNm}</div>
                        </Link>
                        <div className={styles.castRole}>{person.cast}</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
          {castPage < castTotalPage - 1 && (
            <button className={`${styles.castNavBtn} ${styles.right}`} onClick={() => setCastPage(castPage + 1)}>
              <img src={nextIcon} alt="다음" />
            </button>
          )}
        </div>
      </section>
      <section>
        <div className={styles.commentSectionHeader}>
          <h2 className={styles.commentSectionTitle}>코멘트</h2>
          <span className={styles.commentSectionMore} onClick={() => setAllCommentsModalOpen(true)}>더보기</span>
        </div>
        <div className={styles.commentGrid}>
          {/* {commentLoading && <div>로딩 중...</div>} */}
          {commentError && <div style={{ color: 'red' }}>{commentError}</div>}
          {!commentLoading && !commentError && comments.length === 0 && <div>아직 코멘트가 없습니다.</div>}
          {comments.map((comment, idx) => (
            <div
              className={styles.commentCard}
              key={comment.id || idx}
              onClick={() => handleCommentCardClick(comment.id)}
              style={{ cursor: 'pointer' }}
            >
              <div className={styles.commentHeader}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <img
                    className={styles.commentUserProfileImage}
                    src={comment.userProfileImageUrl && comment.userProfileImageUrl.trim() !== '' ? comment.userProfileImageUrl : userIcon}
                    alt="프로필"
                  />
                  <span className={styles.commentUser}>{comment.userNickname || comment.user || '익명'}</span>
                  <span className={styles.commentDate}>{formatRelativeTime(comment.updatedAt || comment.date)}</span>
                </div>
                <span className={styles.commentRating}>
                  ★ {comment.rating ? comment.rating.toFixed(1) : '-'}
                </span>
              </div>
              <hr className={styles.commentDivider} />
              <div className={styles.commentContent}>{comment.content}</div>
              <hr className={styles.commentFooterDivider} />
              <div className={styles.commentFooter}>
                <span>좋아요 {comment.likeCount ?? 0}</span>
                <span>댓글 {comment.commentCount ?? 0}</span>

                {/* 👇 조건부 버튼 */}
                {user && user.id === comment.userId && (
                  <div className={styles.commentActions} onClick={e => e.stopPropagation()}>
                    <button className={styles.replyEditBtn} onClick={() => handleEdit(comment)}>수정</button>
                    <button className={styles.replyDeleteBtn} onClick={() => handleDelete(comment.id)}>삭제</button>
                  </div>
                )}
              </div>
              <div className={styles.commentIconRow}>
                <img
                  src={comment.likedByMe ? likeIconTrue : likeIcon}
                  alt="좋아요"
                  className={styles.commentIcon}
                  onClick={e => {
                    e.stopPropagation();
                    handleLike(comment.id, comment.likedByMe);
                  }}
                  style={{ cursor: 'pointer' }}
                />
                <img
                  src={commentIcon2}
                  alt="댓글"
                  className={styles.commentIcon}
                  onClick={e => handleReplyIconClick(e, comment.id)}
                  style={{ cursor: 'pointer' }}
                />
              </div>
            </div>
          ))}
        </div>
      </section>
      <section>
        <h2>비슷한 장르의 영화</h2>
        {similarMoviesLoading && <div style={{ textAlign: 'center', padding: '20px' }}>로딩 중...</div>}
        {similarMoviesError && <div style={{ color: 'red', textAlign: 'center', padding: '20px' }}>{similarMoviesError}</div>}
        {!similarMoviesLoading && !similarMoviesError && similarMovies.length === 0 && (
          <div style={{ textAlign: 'center', padding: '20px' }}>비슷한 장르의 영화가 없습니다.</div>
        )}
        {!similarMoviesLoading && !similarMoviesError && similarMovies.length > 0 && (
          <MovieHorizontalSlider
            data={similarMovies}
            sectionKey="similar"
            CardComponent={SimilarMovieCard}
          />
        )}
      </section>
      <section>
        <h2>스틸컷</h2>
        <div className={styles.StillsliderWrapper}>
          {stillStart > 0 && (
            <button
              className={`${styles.navBtn} ${styles.left}`}
              onClick={handlePrev}
            >
              <img src={previousIcon} alt="이전" />
            </button>
          )}
          <div
            className={styles.slider}
            style={{
              display: 'flex',
              transition: 'transform 0.4s',
              transform: `translateX(-${stillStart * (stillCardWidth + stillCardGap)}px)`
            }}
          >
            {stillcutsData.map((still, idx) => (
              <div
                className={styles.stillcutCard}
                key={still.id ? `still-${still.id}` : `still-idx-${idx}`}
                style={{
                  flex: `0 0 ${stillCardWidth}px`,
                  marginRight: idx !== stillcutsData.length - 1 ? `${stillCardGap}px` : 0
                }}
              >
                <img src={still.imageUrl} alt="스틸컷" className={styles.stillcutImg} />
              </div>
            ))}
          </div>
          {stillStart + stillVisible < stillcutsData.length && (
            <button
              className={`${styles.navBtn} ${styles.right}`}
              onClick={handleNext}
            >
              <img src={nextIcon} alt="다음" />
            </button>
          )}
        </div>
      </section>

      {/* 댓글 상세 모달 */}
      <CommentDetailModal
        open={commentDetailModalOpen}
        onClose={handleCloseAllModals} // 닫기(×) 버튼
        onBack={handleDetailModalClose} // 이전(←) 버튼
        comment={selectedComment}
        reviewId={selectedReviewId}
        fetchComments={fetchComments}
      />
      {/* 코멘트 수정 모달 */}
      <CommentModal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        movieTitle={editTarget?.movieNm}
        movieCd={editTarget?.movieCd}
        editMode={true}
        initialContent={editTarget?.content || ''}
        initialRating={editTarget?.rating || 0}
        onEditSave={handleEditSave}
        reviewId={editTarget?.id}
      />
      {/* 대댓글 작성 모달(기존) */}
      <ReplyModal
        open={replyModalOpen}
        onClose={() => setReplyModalOpen(false)}
        reviewId={selectedReviewId}
        parentId={null}
        isReply={true}
        onSave={handleReplySave}
      />
      {/* 전체 코멘트 모달 */}
      <AllCommentsModal
        open={allCommentsModalOpen}
        onClose={() => setAllCommentsModalOpen(false)}
        movieId={movieCd} // 또는 실제 id 변수명
        onCommentClick={handleAllCommentsCommentClick}
      />
    </div>
  );
} 