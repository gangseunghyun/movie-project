import React, { useState, useEffect, useRef, useCallback } from 'react';
import styles from './AllReviewsModal.module.css';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import commentIcon2 from '../../assets/comment_icon2.png';
import userIcon from '../../assets/user_icon.png';

const SORT_OPTIONS = [
  { className: 'date', value: 'latest', label: '최신 순' },
  { className: 'like', value: 'like', label: '좋아요 순' },
  { className: 'ratingDesc', value: 'ratingDesc', label: '평점 높은 순' },
  { className: 'ratingAsc', value: 'ratingAsc', label: '평점 낮은 순' },
];

export default function AllReviewsModal({ open, onClose, movieId, onCommentClick, onShowLikedUsers, onLikeClick, onReplyIconClick }) {
  const [comments, setComments] = useState([]);
  const [sort, setSort] = useState('latest');
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const observer = useRef();
  const COMMENTS_PER_PAGE = 10;

  // 무한스크롤 감지 ref
  const lastCommentRef = useCallback(node => {
    if (loading) return;
    if (observer.current) observer.current.disconnect();
    observer.current = new window.IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasMore) {
        setPage(prev => prev + 1);
      }
    });
    if (node) observer.current.observe(node);
  }, [loading, hasMore]);

  // API 호출 함수
  const fetchComments = useCallback(async (pageNum, sortType) => {
    if (!movieId) return;
    const url = `/api/reviews/movie/${movieId}?page=${pageNum}&size=${COMMENTS_PER_PAGE}&sort=${sortType}`;
    try {
      const response = await fetch(url);
      const data = await response.json();
      if (!response.ok) throw new Error('Failed to fetch comments');
      // 각 댓글의 대댓글 개수 조회
      const reviewList = data.data || [];
      const reviewListWithReplyCount = await Promise.all(
        reviewList.map(async comment => {
          try {
            const res = await fetch(`/api/comments/${comment.id}/replies`);
            const replies = await res.json();
            return {
              ...comment,
              replyCount: Array.isArray(replies) ? replies.length : (replies.data?.length ?? 0)
            };
          } catch {
            return {
              ...comment,
              replyCount: 0
            };
          }
        })
      );
      return { ...data, data: reviewListWithReplyCount };
    } catch (error) {
      console.error('Error fetching comments:', error, url);
      throw error;
    }
  }, [movieId]);

  // 코멘트 불러오기
  useEffect(() => {
    if (!open) return;
    setComments([]);
    setPage(0);
    setHasMore(true);
  }, [open, sort]);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    fetchComments(page, sort)
      .then(data => {
        const reviewList = data.data || [];
        if (page === 0) {
          setComments(reviewList);
        } else {
          setComments(prev => [...prev, ...reviewList]);
        }
        setTotalCount(data.total ?? data.count ?? 0);
        setTotalPages(data.totalPages ?? 0);
        setHasMore(data.page < (data.totalPages - 1));
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [page, sort, open, fetchComments]);

  function getRelativeDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();

    // 오늘 날짜(연, 월, 일)만 비교
    const dateYMD = date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
    const nowYMD = now.getFullYear() + '-' + (now.getMonth() + 1) + '-' + now.getDate();

    if (dateYMD === nowYMD) return '오늘';

    // 며칠 전 계산
    const diffTime = now.setHours(0, 0, 0, 0) - date.setHours(0, 0, 0, 0);
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    return `${diffDays}일 전`;
  }

  if (!open) return null;

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContent}>
        <div className={styles.headerRow}>
          <span className={styles.title}>전체 코멘트</span>
          <div className={styles.sortRow}>
            <select
              className={styles.sortSelect}
              value={sort}
              onChange={e => setSort(e.target.value)}
            >
              {SORT_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
            <button className={styles.closeBtn} onClick={onClose}>×</button>
          </div>
        </div>
        <div className={styles.countRow}>{totalCount}개의 코멘트</div>
        <div className={styles.commentList}>
          {comments.map((comment, idx) => {
            const isLast = idx === comments.length - 1;
            return (
              <div
                className={styles.commentCard}
                key={comment.id || idx}
                ref={isLast ? lastCommentRef : null}
                style={{ cursor: 'default' }}
              >
                <div className={styles.commentHeader}>
                  <div className={styles.commentHeaderLeft}>
                    <img
                      src={comment.userProfileImageUrl && comment.userProfileImageUrl.trim() !== '' ? comment.userProfileImageUrl : userIcon}
                      alt="프로필"
                      className={styles.commentUserProfileImage}
                    />
                    <span className={styles.commentUser}>{comment.userNickname || comment.user || '익명'}</span>
                    <span className={styles.commentDate}>{getRelativeDate(comment.updatedAt || comment.createdDate || comment.date)}</span>
                  </div>
                  <span className={styles.commentRating}>★ {comment.rating ? comment.rating.toFixed(1) : '-'}</span>
                </div>
                <div className={styles.commentDivider}></div>
                <div
                  onClick={() => {
                    if (onCommentClick) {
                      onCommentClick(comment);
                    }
                  }}
                  style={{ 
                    cursor: 'pointer',
                    color: '#cecece',
                    minHeight: '48px'
                  }}
                >
                  {comment.content}
                </div>
                <div className={styles.commentDivider}></div>
                <div className={styles.commentFooter}>
                  <span
                    onClick={e => {
                      e.stopPropagation();
                      if (onShowLikedUsers) {
                        onShowLikedUsers(comment.id);
                      }
                    }}
                    style={{ cursor: 'pointer', color: '#ffd600' }}
                  >
                    좋아요 {comment.likeCount ?? 0}
                  </span>
                  <span
                    onClick={e => {
                      e.stopPropagation();
                      if (onCommentClick) {
                        onCommentClick(comment);
                      }
                    }}
                    style={{ cursor: 'pointer' }}
                  >
                    댓글 {comment.commentCount ?? 0}
                  </span>
                </div>
                <div className={styles.commentIconRow}>
                  <img
                    src={comment.likedByMe ? likeIconTrue : likeIcon}
                    alt="좋아요"
                    className={styles.commentIcon}
                    onClick={async e => {
                      e.stopPropagation();
                      if (onLikeClick) {
                        await onLikeClick(comment.id, comment.likedByMe);
                        // 좋아요 처리 후 현재 페이지 다시 로드
                        try {
                          const data = await fetchComments(page, sort);
                          const reviewList = data.data || [];
                          setComments(reviewList);
                          setTotalCount(data.total ?? data.count ?? 0);
                          setTotalPages(data.totalPages ?? 0);
                          setHasMore(data.page < (data.totalPages - 1));
                        } catch (error) {
                          console.error('좋아요 후 목록 새로고침 실패:', error);
                        }
                      }
                    }}
                    style={{ cursor: 'pointer' }}
                  />
                  <img
                    src={commentIcon2}
                    alt="댓글"
                    className={styles.commentIcon}
                    onClick={e => {
                      e.stopPropagation();
                      if (onReplyIconClick) {
                        onReplyIconClick(e, comment.id);
                      }
                    }}
                    style={{ cursor: 'pointer' }}
                  />
                </div>
              </div>
            );
          })}
          {/* {loading && <div className={styles.loading}>로딩 중...</div>} */}
          {!loading && comments.length === 0 && <div className={styles.empty}>아직 코멘트가 없습니다.</div>}
        </div>
      </div>
    </div>
  );
} 