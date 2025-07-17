import React, { useState, useEffect, useRef, useCallback } from 'react';
import styles from './LikedCommentsModal.module.css';
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

export default function LikedCommentsModal({ 
  open, 
  onClose, 
  likedComments = [], 
  onCommentClick, 
  onShowLikedUsers, 
  onLikeClick, 
  onReplyIconClick,
  onProfileClick,
  title = '좋아요한 코멘트 전체보기' 
}) {
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

  // 정렬 함수
  const sortComments = useCallback((comments, sortType) => {
    const sortedComments = [...comments];
    switch (sortType) {
      case 'latest':
        return sortedComments.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
      case 'like':
        return sortedComments.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0));
      case 'ratingDesc':
        return sortedComments.sort((a, b) => (b.rating || 0) - (a.rating || 0));
      case 'ratingAsc':
        return sortedComments.sort((a, b) => (a.rating || 0) - (b.rating || 0));
      default:
        return sortedComments;
    }
  }, []);

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
    
    // 정렬된 코멘트 설정
    const sortedComments = sortComments(likedComments, sort);
    setComments(sortedComments);
    setTotalCount(sortedComments.length);
    setHasMore(false); // 이미 모든 데이터가 있으므로 무한스크롤 불필요
    setLoading(false);
  }, [likedComments, sort, open, sortComments]);

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
          <span className={styles.title}>{title}</span>
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
                      src={comment.authorProfileImageUrl && comment.authorProfileImageUrl.trim() !== '' ? comment.authorProfileImageUrl : userIcon}
                      alt="프로필"
                      className={styles.commentUserProfileImage}
                      onClick={e => {
                        e.stopPropagation();
                        if (onProfileClick) {
                          onProfileClick(comment.userId || comment.authorId);
                        }
                      }}
                      style={{ cursor: 'pointer' }}
                    />
                    <span 
                      className={styles.commentUser}
                      onClick={e => {
                        e.stopPropagation();
                        if (onProfileClick) {
                          onProfileClick(comment.userId || comment.authorId);
                        }
                      }}
                      style={{ cursor: 'pointer' }}
                    >
                      {comment.authorNickname || comment.userNickname || '익명'}
                    </span>
                    <span className={styles.commentDate}>{getRelativeDate(comment.updatedAt || comment.createdAt || comment.date)}</span>
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
                    onClick={e => {
                      e.stopPropagation();
                      if (onLikeClick) {
                        onLikeClick(comment.id, comment.likedByMe);
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
          {!loading && comments.length === 0 && <div className={styles.empty}>아직 좋아요한 코멘트가 없습니다.</div>}
        </div>
      </div>
    </div>
  );
} 