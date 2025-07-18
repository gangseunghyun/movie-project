import React, { useEffect, useState } from "react";
import styles from "./MyPageFooter.module.css";
import { useUser } from '../../contexts/UserContext';
import { Link, useNavigate } from 'react-router-dom';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import commentIcon2 from '../../assets/comment_icon2.png';
import userIcon from '../../assets/user_icon.png';
import CommentModal from '../Modal/CommentModal';
import LikedCommentsModal from '../Modal/LikedCommentsModal';
import MyCommentsModal from '../Modal/MyCommentsModal';
import ReviewCommentsModal from '../Modal/ReviewCommentsModal';
import LikedUsersModal from '../Modal/LikedUsersModal';
import ReviewModal from '../Modal/ReviewModal';

function formatRelativeTime(dateString) {
  if (!dateString) return '';
  const now = new Date();
  const commentDate = new Date(dateString);
  const diffTime = now.getTime() - commentDate.getTime();
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return '오늘';
  if (diffDays === 1) return '어제';
  if (diffDays < 7) return `${diffDays}일 전`;
  return commentDate.toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' });
}

const MyPageFooter = ({ targetUserId, tempUserInfo, targetUser: propTargetUser }) => {
  const { user } = useUser();
  const navigate = useNavigate();
  const [myComments, setMyComments] = useState([]);
  const [likedComments, setLikedComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [likedLoading, setLikedLoading] = useState(true);
  // 모달 상태 및 선택된 코멘트 관리
  const [selectedComment, setSelectedComment] = useState(null);
  // 수정 모달 상태 및 타겟
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [localTempUserInfo, setLocalTempUserInfo] = useState(tempUserInfo);
  const [likedModalOpen, setLikedModalOpen] = useState(false);
  const [myCommentsModalOpen, setMyCommentsModalOpen] = useState(false);
  
  // 댓글 관련 모달 상태 추가
  const [replyModalOpen, setReplyModalOpen] = useState(false);
  const [selectedReviewId, setSelectedReviewId] = useState(null);
  const [commentsModalOpen, setCommentsModalOpen] = useState(false);
  const [likedUsersModalOpen, setLikedUsersModalOpen] = useState(false);
  const [selectedReviewIdForLikes, setSelectedReviewIdForLikes] = useState(null);

  // 클린봇 차단된 리뷰 표시 상태 관리
  const [showBlocked, setShowBlocked] = useState({});

  // sessionStorage에서 tempUserInfo 확인 (새로고침 시에도 유지)
  useEffect(() => {
    const storedUserInfo = sessionStorage.getItem('tempUserInfo');
    if (storedUserInfo) {
      try {
        const userInfo = JSON.parse(storedUserInfo);
        setLocalTempUserInfo(userInfo);

      } catch (error) {
        console.error('임시 유저 정보 파싱 실패:', error);
      }
    }
  }, []);

  // 표시할 유저 결정 (propTargetUser가 있으면 propTargetUser, localTempUserInfo가 있으면 localTempUserInfo, 없으면 현재 로그인한 user)
  const displayUserId = propTargetUser ? propTargetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id));
  const displayUser = propTargetUser || localTempUserInfo || user;
  const isOwnPage = String(displayUserId) === String(user?.id);


  // 내가 작성한 코멘트 fetch 함수
  const fetchMyComments = () => {
    if (!displayUserId) return;
    setLoading(true);
    fetch(`http://localhost:80/api/users/${displayUserId}/my-comments`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        console.log('내가 작성한 코멘트 데이터:', data);
        const comments = data.data || [];
        
        // 각 코멘트의 최신 평점을 별도로 조회
        const fetchLatestRatings = async () => {
          const updatedComments = await Promise.all(
            comments.map(async (comment) => {
              if (comment.movieCd) {
                try {
                  const ratingResponse = await fetch(`http://localhost:80/api/ratings/${comment.movieCd}`, {
                    credentials: 'include',
                  });
                  if (ratingResponse.ok) {
                    const ratingData = await ratingResponse.json();
                    if (ratingData.success && ratingData.data) {
                      return { ...comment, rating: ratingData.data.score };
                    }
                  }
                } catch (error) {
                  console.error('평점 조회 실패:', error);
                }
              }
              return comment;
            })
          );
          setMyComments(updatedComments);
        };
        
        fetchLatestRatings();
      })
      .catch(() => setMyComments([]))
      .finally(() => setLoading(false));
  };

  // 내가 좋아요한 코멘트 fetch 함수
  const fetchLikedComments = () => {
    if (!displayUserId) return;
    setLikedLoading(true);
    fetch(`http://localhost:80/api/users/${displayUserId}/liked-reviews`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        console.log('내가 좋아요한 코멘트 데이터:', data);
        const comments = data.data || [];
        
        // 좋아요한 코멘트는 다른 사람이 작성한 것이므로 원래 평점을 유지
        // 단, 내가 작성한 코멘트인 경우에만 최신 평점을 조회
        const fetchLatestRatings = async () => {
          const updatedComments = await Promise.all(
            comments.map(async (comment) => {
              // 내가 작성한 코멘트인 경우에만 최신 평점 조회
              if (comment.movieCd && comment.authorId === displayUserId) {
                try {
                  const ratingResponse = await fetch(`http://localhost:80/api/ratings/${comment.movieCd}`, {
                    credentials: 'include',
                  });
                  if (ratingResponse.ok) {
                    const ratingData = await ratingResponse.json();
                    if (ratingData.success && ratingData.data) {
                      return { ...comment, rating: ratingData.data.score };
                    }
                  }
                } catch (error) {
                  console.error('평점 조회 실패:', error);
                }
              }
              // 다른 사람이 작성한 코멘트는 원래 평점 유지
              return comment;
            })
          );
          setLikedComments(updatedComments);
        };
        
        fetchLatestRatings();
      })
      .catch(() => setLikedComments([]))
      .finally(() => setLikedLoading(false));
  };

  useEffect(() => {
    fetchMyComments();
  }, [displayUserId]);

  useEffect(() => {
    fetchLikedComments();
  }, [displayUserId]);

  // 더보기 버튼 클릭 시 (1차: alert, 2차: 모달 구현 가능)
  const handleMoreMyComments = () => {
    setMyCommentsModalOpen(true);
  };
  const handleMoreLikedComments = () => {
    setLikedModalOpen(true);
  };

  // 코멘트 카드 클릭 핸들러
  const handleCommentClick = (comment) => {
    let mergedComment = {
      ...comment,
      userNickname: comment.authorNickname || user?.nickname || '익명'
    };
    console.log('mergedComment:', mergedComment);
    setSelectedComment(mergedComment);
    setCommentsModalOpen(true);
  };

  // 코멘트 수정 핸들러
  const handleEdit = (comment) => {
    setEditTarget(comment);
    setEditModalOpen(true);
  };

  // 코멘트 삭제 핸들러
  const handleDelete = (commentId) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      fetch(`http://localhost:80/api/reviews/${commentId}`, {
        method: 'DELETE',
        credentials: 'include',
      })
        .then(res => res.json())
        .then(data => {
          if (data.success) {
            // 삭제 후 목록 갱신
            setMyComments(prev => prev.filter(c => c.id !== commentId));
          } else {
            alert('삭제 실패: ' + (data.message || ''));
          }
        })
        .catch(() => alert('삭제 중 오류 발생'));
    }
  };

  // 프로필 클릭 핸들러
  const handleProfileClick = (userId) => {
    console.log('프로필 클릭됨, userId:', userId);
    if (userId) {
      // 현재 페이지인지 확인
      if (String(userId) === String(user?.id)) {
        alert('이미 내 페이지입니다.');
        return;
      }
      
      // 모든 모달 닫기
      setMyCommentsModalOpen(false);
      setLikedModalOpen(false);
      setCommentsModalOpen(false);
      setReplyModalOpen(false);
      setLikedUsersModalOpen(false);
      setEditModalOpen(false);
      
      console.log('마이페이지로 이동:', `/mypage/${userId}`);
      navigate(`/mypage/${userId}`);
      // 페이지 이동 후 스크롤을 맨 위로
      setTimeout(() => {
        window.scrollTo(0, 0);
      }, 100);
    } else {
      console.log('userId가 없음');
    }
  };

  // 클린봇 차단된 리뷰 표시 토글 함수
  const toggleBlockedContent = (reviewId) => {
    setShowBlocked(prev => ({
      ...prev,
      [reviewId]: !prev[reviewId]
    }));
  };

  // 클린봇 차단된 리뷰 렌더링 함수
  const renderReviewContent = (review) => {
    if (review.blockedByCleanbot) {
      return (
        <div className={styles.commentContent} style={{ color: '#888', fontStyle: 'italic' }}>
          {showBlocked[review.id] ? (
            <>
              <span style={{ color: '#ff2f6e', fontWeight: 600 }}>[클린봇 감지]</span> {review.content}
            </>
          ) : (
            <>
              이 리뷰는 클린봇이 감지한 악성 리뷰입니다.{' '}
              <button 
                style={{ 
                  color: '#3b82f6', 
                  background: 'none', 
                  border: 'none', 
                  cursor: 'pointer', 
                  textDecoration: 'underline' 
                }} 
                onClick={(e) => {
                  e.stopPropagation();
                  toggleBlockedContent(review.id);
                }}
              >
                보기
              </button>
            </>
          )}
        </div>
      );
    } else {
      return (
        <div className={styles.commentContent}>{review.content}</div>
      );
    }
  };

  // 좋아요한 유저 목록 모달 열기 함수
  const handleShowLikedUsers = (reviewId) => {
    setSelectedReviewIdForLikes(reviewId);
    setLikedUsersModalOpen(true);
  };

  // 댓글 모달 열기 함수
  const handleShowComments = (comment) => {
    setSelectedComment(comment);
    setCommentsModalOpen(true);
  };

  // 댓글 아이콘 클릭 핸들러
  const handleReplyIconClick = (e, reviewId) => {
    e.stopPropagation();
    setSelectedReviewId(reviewId);
    setReplyModalOpen(true);
  };

  // 댓글 작성 후 코멘트 목록 새로고침
  const handleReplySave = () => {
    console.log('handleReplySave 호출됨 - 댓글 목록 새로고침');
    fetchMyComments();
    fetchLikedComments();
    
    // 댓글 작성 후 ReviewCommentsModal 다시 열기
    if (selectedComment) {
      setTimeout(() => {
        setCommentsModalOpen(true);
      }, 500); // 0.5초 후 ReviewCommentsModal 열기
    }
  };

  // 좋아요 핸들러
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
        fetchMyComments(); // 좋아요 상태 및 카운트 갱신
        fetchLikedComments();
      } else if (res.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('좋아요 처리 실패');
      }
    } catch (e) {
      alert('네트워크 오류');
    }
  };

  // 댓글 개수 변경 콜백
  const handleReviewCommentCountChange = (reviewId, newCount) => {
    setMyComments(prev =>
      prev.map(c =>
        c.id === reviewId ? { ...c, commentCount: newCount } : c
      )
    );
    setLikedComments(prev =>
      prev.map(c =>
        c.id === reviewId ? { ...c, commentCount: newCount } : c
      )
    );
  };

  // MovieDetailBody.jsx의 코멘트 카드 레이아웃 복사
  const renderCommentCard = (comment, showActions = false) => (
    <div
      className={styles.commentCard}
      key={comment.id}
      onClick={() => handleCommentClick(comment)}
    >
      <div 
        className={styles.commentHeader}
        onClick={e => e.stopPropagation()}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <img
            src={comment.authorProfileImageUrl && comment.authorProfileImageUrl.trim() !== '' ? comment.authorProfileImageUrl : userIcon}
            alt="프로필"
            className={styles.profileImage}
            style={{ 
              width: 24, 
              height: 24, 
              borderRadius: '50%', 
              cursor: 'pointer'
            }}
            onClick={e => {
              e.stopPropagation();
              console.log('프로필 이미지 클릭됨');
              handleProfileClick(comment.userId || comment.authorId);
            }}
          />
          <span className={styles.commentUser}>{comment.authorNickname || displayUser?.nickname || '익명'}</span>
          <span className={styles.commentDate}>{formatRelativeTime(comment.updatedAt || comment.date)}</span>
        </div>
        <span className={styles.commentRating}>
          ★ {comment.rating ? comment.rating.toFixed(1) : '-'}
        </span>
      </div>
      <hr className={styles.commentDivider} />
      <div 
        className={styles.commentContentWrap}
        onClick={e => {
          e.stopPropagation();
          setSelectedComment(comment);
          setCommentsModalOpen(true);
        }}
        style={{ cursor: 'pointer' }}
      >
        <div className={styles.commentMovieInfo}>
          <span className={styles.commentMovieTitle}>{comment.movieNm}</span>
          <img 
            src={comment.posterUrl} 
            alt="영화 포스터" 
            className={styles.commentMoviePoster}
            onClick={e => {
              e.stopPropagation();
              setSelectedComment(comment);
              setCommentsModalOpen(true);
            }}
            style={{ cursor: 'pointer' }}
          />
        </div>
        <div>
          {renderReviewContent(comment)}
        </div>
      </div>
      <hr className={styles.commentFooterDivider} />
      <div 
        className={styles.commentFooter}
        onClick={e => e.stopPropagation()}
        style={{ cursor: 'default' }}
      >
        <div className={styles.commentFooterLeft}>
          <div className={styles.commentCountContainer}>
            <span 
              className={styles.commentCount}
              onClick={e => {
                e.stopPropagation();
                handleShowLikedUsers(comment.id);
              }}
            >
              좋아요 {comment.likeCount ?? 0}
            </span>
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
          </div>
          <div className={styles.commentCountContainer}>
            <span 
              className={styles.commentCount}
              onClick={e => {
                e.stopPropagation();
                setSelectedComment(comment);
                setCommentsModalOpen(true);
              }}
            >
              댓글 {comment.commentCount ?? 0}
            </span>
            <img
              src={commentIcon2}
              alt="댓글"
              className={styles.commentIcon}
              onClick={e => handleReplyIconClick(e, comment.id)}
              style={{ cursor: 'pointer' }}
            />
          </div>
        </div>
        {showActions && (
          <div className={styles.commentActions} onClick={e => e.stopPropagation()}>
            <button className={styles.replyEditBtn} onClick={() => handleEdit(comment)}>수정</button>
            <button className={styles.replyDeleteBtn} onClick={() => handleDelete(comment.id)}>삭제</button>
          </div>
        )}
      </div>
    </div>
  );

  return (
    <footer className={styles.footer}>
      <div className={styles.commentSectionHeader}>
        <h2 className={styles.commentSectionTitle}>
          {isOwnPage ? '내가 작성한 코멘트' : `${displayUser?.nickname || '익명'}님이 작성한 코멘트`}
        </h2>
        <span className={styles.commentSectionMore} onClick={handleMoreMyComments}>더보기</span>
      </div>
      <div className={styles.commentGrid}>
        {loading ? <div>로딩 중...</div> : (
          myComments.length === 0 ? <div className={styles.emptyMessage}>아직 코멘트가 없습니다.</div> :
            myComments.slice(0, 8).map(comment => renderCommentCard(comment, isOwnPage))
        )}
      </div>
      <hr className={styles.divider} />
      <div className={styles.commentSectionHeader} style={{ marginTop: '32px' }}>
        <h2 className={styles.commentSectionTitle}>
          {isOwnPage ? '내가 좋아요한 코멘트' : `${displayUser?.nickname || '익명'}님이 좋아요한 코멘트`}
        </h2>
        <span className={styles.commentSectionMore} onClick={handleMoreLikedComments}>더보기</span>
      </div>
      <div className={styles.commentGrid}>
        {likedLoading ? <div>로딩 중...</div> : (
          likedComments.length === 0 ? <div className={styles.emptyMessage}>아직 좋아요한 코멘트가 없습니다.</div> :
            likedComments.slice(0, 8).map(comment => renderCommentCard(comment, false))
        )}
      </div>
      {/* 좋아요한 코멘트 전체보기 모달 */}
      <LikedCommentsModal
        open={likedModalOpen}
        onClose={() => setLikedModalOpen(false)}
        likedComments={likedComments}
        onCommentClick={comment => {
          setSelectedComment(comment);
          setLikedModalOpen(false);
          setCommentsModalOpen(true);
        }}
        onShowLikedUsers={handleShowLikedUsers}
        onLikeClick={handleLike}
        onReplyIconClick={handleReplyIconClick}
        onProfileClick={handleProfileClick}
      />
      {/* 내가 작성한 코멘트 전체보기 모달 */}
      <MyCommentsModal
        open={myCommentsModalOpen}
        onClose={() => setMyCommentsModalOpen(false)}
        myComments={myComments}
        title={isOwnPage ? '내가 작성한 코멘트 전체보기' : `${displayUser?.nickname || '익명'}님이 작성한 코멘트 전체보기`}
        onCommentClick={comment => {
          setSelectedComment(comment);
          setMyCommentsModalOpen(false);
          setCommentsModalOpen(true);
        }}
        onShowLikedUsers={handleShowLikedUsers}
        onLikeClick={handleLike}
        onReplyIconClick={handleReplyIconClick}
        onProfileClick={handleProfileClick}
      />

      {/* 코멘트 수정 모달 */}
      <ReviewModal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        movieTitle={editTarget?.movieNm}
        movieCd={editTarget?.movieCd}
        editMode={true}
        initialContent={editTarget?.content || ''}
        initialRating={editTarget?.rating || 0}
        onEditSave={() => {
          setEditModalOpen(false);
          // 수정 후 목록 갱신 (내가 작성한 코멘트와 좋아요한 코멘트 모두 갱신)
          fetchMyComments();
          fetchLikedComments();
        }}
        reviewId={editTarget?.id}
      />
      {/* 댓글 작성 모달 */}
      <CommentModal
        open={replyModalOpen}
        onClose={() => setReplyModalOpen(false)}
        reviewId={selectedReviewId}
        onSave={handleReplySave}
        movieTitle={selectedComment?.movieNm || ''}
        reviewContent={selectedComment?.content || ''}
        userId={user?.id}
      />
      {/* 댓글 목록 모달 */}
      <ReviewCommentsModal
        isOpen={commentsModalOpen}
        onClose={() => setCommentsModalOpen(false)}
        review={selectedComment}
        onCommentCountChange={handleReviewCommentCountChange}
        handleLikeReview={handleLike}
        handleReplyIconClick={handleReplyIconClick}
        user={user}
      />
      {/* 좋아요한 유저 목록 모달 */}
      <LikedUsersModal 
        isOpen={likedUsersModalOpen} 
        onClose={() => setLikedUsersModalOpen(false)} 
        reviewId={selectedReviewIdForLikes} 
      />
    </footer>
  );
};

export default MyPageFooter; 