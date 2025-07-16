import React, { useState } from 'react';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import commentIcon2 from '../../assets/comment_icon2.png';
import userIcon from '../../assets/user_icon.png';
import styles from './ReviewCommentsModal.module.css';

const ReviewDetailModal = ({ open, onClose, comment, reviewId, fetchComments }) => {
  const [likeLoading, setLikeLoading] = useState(false);
  const [likedByMe, setLikedByMe] = useState(comment?.likedByMe || false);
  const [likeCount, setLikeCount] = useState(comment?.likeCount || 0);

  if (!open || !comment) return null;

  const handleLike = async () => {
    if (likeLoading) return;
    setLikeLoading(true);
    try {
      let res;
      if (likedByMe) {
        res = await fetch(`http://localhost:80/api/reviews/dto/${comment.id}/like`, {
          method: 'DELETE',
          credentials: 'include',
        });
      } else {
        res = await fetch(`http://localhost:80/api/reviews/dto/${comment.id}/like`, {
          method: 'POST',
          credentials: 'include',
        });
      }
      if (res.ok) {
        setLikedByMe(!likedByMe);
        setLikeCount(prev => likedByMe ? Math.max(prev - 1, 0) : prev + 1);
        if (fetchComments) fetchComments();
      } else {
        alert('좋아요 처리 실패');
      }
    } catch (e) {
      alert('네트워크 오류');
    } finally {
      setLikeLoading(false);
    }
  };

  const handleCommentClick = () => {
    // 댓글 모달/입력창 열기 등 원하는 동작 구현
    alert('댓글 입력창/모달을 여는 로직을 구현하세요!');
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={e => e.stopPropagation()}>
        {/* 헤더 */}
        <div className={styles.header}>
          <div className={styles.headerLeft}>
            <h2 className={styles.title}>댓글</h2>
            <p className={styles.movieTitle}>{comment.movieNm || '영화'}</p>
          </div>
          <button className={styles.closeButton} onClick={onClose}>✕</button>
        </div>
        <div className={styles.divider}></div>
        {/* 리뷰 본문 */}
        <div className={styles.reviewSection}>
          <div className={styles.moviePoster}>
            <img src={comment.posterUrl || userIcon} alt={comment.movieNm || '영화 포스터'} />
          </div>
          <div className={styles.reviewInfo}>
            <h3 className={styles.movieTitle}>{comment.movieNm || '영화'}</h3>
            <div className={styles.reviewContent}>{comment.content}</div>
            {/* 좋아요/댓글 버튼 */}
            <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
              <button
                className={styles.likeButton}
                onClick={handleLike}
                disabled={likeLoading}
                style={{ display: 'flex', alignItems: 'center', background: 'none', border: 'none', cursor: 'pointer' }}
              >
                <img
                  src={likedByMe ? likeIconTrue : likeIcon}
                  alt="좋아요"
                  className={styles.likeIcon}
                  style={{ width: 22, height: 22, marginRight: 4 }}
                />
                <span className={styles.likeCount}>{likeCount}</span>
              </button>
              <button
                className={styles.replyButton}
                onClick={handleCommentClick}
                style={{ display: 'flex', alignItems: 'center', background: 'none', border: 'none', cursor: 'pointer' }}
              >
                <img
                  src={commentIcon2}
                  alt="댓글"
                  style={{ width: 22, height: 22, marginRight: 4 }}
                />
                <span>댓글</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReviewDetailModal; 