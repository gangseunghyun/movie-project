import React, { useState, useEffect } from 'react';
import styles from './ReviewCommentsModal.module.css';
import userIcon from '../../assets/user_icon.png';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import banner1 from '../../assets/banner1.jpg';

export default function ReviewCommentsModal({ isOpen, onClose, review }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [replyingToCommentId, setReplyingToCommentId] = useState(null);
  const [replyContent, setReplyContent] = useState('');
  const [currentUser, setCurrentUser] = useState(null);
  
  // 클린봇 차단된 댓글 표시 상태 관리
  const [showBlocked, setShowBlocked] = useState({});

  useEffect(() => {
    if (isOpen && review) {
      console.log('Review 객체:', review); // 디버깅용
      fetchComments();
      fetchCurrentUser();
    }
  }, [isOpen, review]);

  const fetchCurrentUser = async () => {
    try {
      const response = await fetch('http://localhost:80/api/current-user', {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        if (data.success && data.user) {
          setCurrentUser(data.user);
        } else {
          setCurrentUser(null);
        }
      } else {
        setCurrentUser(null);
      }
    } catch (error) {
      console.error('사용자 정보 조회 실패:', error);
      setCurrentUser(null);
    }
  };



  const fetchComments = async () => {
    if (!review) return;
    
    setLoading(true);
    setError(null);
    
    try {
      // 트리 구조로 댓글 가져오기 (대댓글 포함)
      const url = currentUser 
        ? `http://localhost:80/api/comments/review/${review.id}/all?userId=${currentUser.id}`
        : `http://localhost:80/api/comments/review/${review.id}/all`;
      
      const response = await fetch(url, {
        credentials: 'include'
      });
      
      if (!response.ok) {
        throw new Error('댓글을 불러오는데 실패했습니다.');
      }
      
      const data = await response.json();
      if (data.success) {
        setComments(data.data || []);
        console.log('댓글 데이터 (트리 구조):', data.data); // 디버깅용
      } else {
        throw new Error(data.message || '댓글을 불러오는데 실패했습니다.');
      }
    } catch (err) {
      console.error('댓글 조회 실패:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) return '어제';
    if (diffDays < 7) return `${diffDays}일 전`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`;
    if (diffDays < 365) return `${Math.floor(diffDays / 30)}개월 전`;
    return `${Math.floor(diffDays / 365)}년 전`;
  };

  const formatReviewContent = (content) => {
    console.log('원본 리뷰 내용:', content); // 디버깅용
    if (!content) return '리뷰 내용이 없습니다.';
    if (content.length > 200) {
      return content.substring(0, 200) + '...';
    }
    return content;
  };

  const handleLike = async (commentId, likedByMe) => {
    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    try {
      if (likedByMe) {
        // 좋아요 취소
        const response = await fetch(`http://localhost:80/api/comments/${commentId}/like?userId=${currentUser.id}`, {
          method: 'DELETE',
          credentials: 'include',
        });
        
        if (response.ok) {
          fetchComments();
        } else {
          alert('좋아요 취소에 실패했습니다.');
        }
      } else {
        // 좋아요 추가
        const response = await fetch(`http://localhost:80/api/comments/${commentId}/like`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({
            userId: currentUser.id
          }),
        });
        
        if (response.ok) {
          fetchComments();
        } else {
          alert('좋아요 추가에 실패했습니다.');
        }
      }
    } catch (error) {
      console.error('좋아요 처리 오류:', error);
      alert('네트워크 오류가 발생했습니다.');
    }
  };

  // 댓글 수정 시작
  const startEdit = (comment) => {
    setEditingCommentId(comment.id);
    setEditContent(comment.content);
  };

  // 댓글 수정 취소
  const cancelEdit = () => {
    setEditingCommentId(null);
    setEditContent('');
  };

  // 댓글 수정 저장
  const saveEdit = async () => {
    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    if (!editContent.trim()) {
      alert('댓글 내용을 입력해주세요.');
      return;
    }

    try {
      const response = await fetch(`http://localhost:80/api/comments/${editingCommentId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          content: editContent,
          userId: currentUser.id
        }),
      });

      if (response.ok) {
        setEditingCommentId(null);
        setEditContent('');
        fetchComments();
      } else {
        alert('댓글 수정에 실패했습니다.');
      }
    } catch (error) {
      console.error('댓글 수정 오류:', error);
      alert('네트워크 오류가 발생했습니다.');
    }
  };

  // 댓글 삭제
  const deleteComment = async (commentId) => {
    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    if (!window.confirm('댓글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      const response = await fetch(`http://localhost:80/api/comments/${commentId}?userId=${currentUser.id}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      if (response.ok) {
        fetchComments();
      } else {
        alert('댓글 삭제에 실패했습니다.');
      }
    } catch (error) {
      console.error('댓글 삭제 오류:', error);
      alert('네트워크 오류가 발생했습니다.');
    }
  };

  // 대댓글 작성 시작
  const startReply = (commentId) => {
    setReplyingToCommentId(commentId);
    setReplyContent('');
  };

  // 대댓글 작성 취소
  const cancelReply = () => {
    setReplyingToCommentId(null);
    setReplyContent('');
  };

  // 대댓글 작성 저장
  const saveReply = async () => {
    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    if (!replyContent.trim()) {
      alert('댓글 내용을 입력해주세요.');
      return;
    }

    try {
      const response = await fetch('http://localhost:80/api/comments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          reviewId: review.id,
          content: replyContent,
          parentId: replyingToCommentId,
          userId: currentUser.id
        }),
      });

      if (response.ok) {
        setReplyingToCommentId(null);
        setReplyContent('');
        fetchComments();
      } else {
        alert('댓글 작성에 실패했습니다.');
      }
    } catch (error) {
      console.error('댓글 작성 오류:', error);
      alert('네트워크 오류가 발생했습니다.');
    }
  };

  // 재귀적으로 모든 댓글/대댓글/대대댓글...을 평면적으로 수직 나열
  const renderCommentTree = (comment, parentNickname, depth = 0) => {
    const items = [
      <div
        key={comment.id}
        className={depth === 0 ? styles.commentItem : styles.replyItem}
        style={depth === 1 ? { paddingLeft: 32, borderLeft: '2px solid #2196f3' } : {}}
      >
        <div className={styles.profileImage}>
          <img
            src={comment.userProfileImageUrl && comment.userProfileImageUrl.trim() !== ''
              ? comment.userProfileImageUrl
              : userIcon}
            alt="프로필"
            onError={e => { e.target.src = userIcon; }}
          />
        </div>
        <div className={styles.commentContent}>
          <div className={styles.commentHeaderLine}>
            <span className={styles.username}>{comment.userNickname || comment.user || '익명'}</span>
            <span className={styles.timestamp}>{formatDate(comment.createdAt || comment.updatedAt)}</span>
          </div>
          {editingCommentId === comment.id ? (
            <div className={styles.editSection}>
              <textarea
                value={editContent}
                onChange={e => setEditContent(e.target.value)}
                className={styles.editTextarea}
                placeholder="댓글을 수정하세요..."
              />
              <div className={styles.editButtons}>
                <button onClick={saveEdit} className={styles.saveButton}>저장</button>
                <button onClick={cancelEdit} className={styles.cancelButton}>취소</button>
              </div>
            </div>
          ) : (
            renderCommentText(comment)
          )}
          <div className={styles.actionButtons}>
            <div 
              className={`${styles.likeButton} ${!currentUser ? styles.disabled : ''}`} 
              onClick={() => currentUser ? handleLike(comment.id, comment.likedByMe) : alert('로그인이 필요합니다.')}
            >
              <img
                src={comment.likedByMe ? likeIconTrue : likeIcon}
                alt="좋아요"
                className={styles.likeIcon}
              />
              <span className={styles.likeCount}>{comment.likeCount || 0}</span>
            </div>
            <button 
              className={`${styles.replyButton} ${!currentUser ? styles.disabled : ''}`} 
              onClick={() => currentUser ? startReply(comment.id) : alert('로그인이 필요합니다.')}
            >
              답글쓰기
            </button>
            {currentUser && comment.userId === currentUser.id && (
              <>
                <button className={styles.editButton} onClick={() => startEdit(comment)}>수정</button>
                <button className={styles.deleteButton} onClick={() => deleteComment(comment.id)}>삭제</button>
              </>
            )}
          </div>
          {replyingToCommentId === comment.id && (
            <div className={styles.replySection}>
              <textarea
                value={replyContent}
                onChange={e => setReplyContent(e.target.value)}
                className={styles.replyTextarea}
                placeholder="답글을 작성하세요..."
              />
              <div className={styles.replyButtons}>
                <button onClick={saveReply} className={styles.saveButton}>답글 작성</button>
                <button onClick={cancelReply} className={styles.cancelButton}>취소</button>
              </div>
            </div>
          )}
        </div>
      </div>
    ];
    if (comment.replies && comment.replies.length > 0) {
      comment.replies.forEach(child =>
        items.push(...renderCommentTree(child, comment.userNickname, depth + 1))
      );
    }
    return items;
  };

  // 클린봇 차단된 댓글 표시 토글 함수
  const toggleBlockedContent = (commentId) => {
    setShowBlocked(prev => ({
      ...prev,
      [commentId]: !prev[commentId]
    }));
  };

  // 클린봇 차단된 댓글 렌더링 함수
  const renderCommentText = (comment) => {
    if (comment.isBlockedByCleanbot) {
      return (
        <div className={styles.commentText} style={{ color: '#888', fontStyle: 'italic' }}>
          {showBlocked[comment.id] ? (
            <>
              <span style={{ color: '#ff2f6e', fontWeight: 600 }}>[클린봇 감지]</span> {comment.content}
            </>
          ) : (
            <>
              이 댓글은 클린봇이 감지한 악성 댓글입니다.{' '}
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
                  toggleBlockedContent(comment.id);
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
        <div className={styles.commentText}>
          {comment.parent && (
            <span className={styles.replyTo}>@{comment.parent.userNickname || '익명'}</span>
          )}
          {comment.content}
        </div>
      );
    }
  };

  if (!isOpen) return null;

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        {/* 헤더 */}
        <div className={styles.header}>
          <div className={styles.headerLeft}>
            <h2 className={styles.title}>댓글</h2>
            <p className={styles.movieTitle}>{review?.movieNm || '영화'}</p>
          </div>
          <button className={styles.closeButton} onClick={onClose}>
            ✕
          </button>
        </div>

        <div className={styles.divider}></div>

        {/* 비로그인 사용자 안내 메시지 */}
        {!currentUser && (
          <div className={styles.loginNotice}>
            <p>댓글 작성과 좋아요 기능을 사용하려면 로그인이 필요합니다.</p>
          </div>
        )}

        {/* 댓글 목록 */}
        <div className={styles.commentsSection}>
          {/* 영화 정보 및 원본 리뷰 */}
          <div className={styles.reviewSection}>
            {/* 영화 포스터 */}
            <div className={styles.moviePoster}>
              <img 
                src={review?.posterUrl || banner1} 
                alt={review?.movieNm || '영화 포스터'}
                onError={(e) => {
                  e.target.src = banner1;
                }}
              />
            </div>
            
            {/* 영화 정보 및 리뷰 내용 */}
            <div className={styles.reviewInfo}>
              <h3 className={styles.movieTitle}>{review?.movieNm || '영화'}</h3>
              <div className={styles.reviewContent}>
                {formatReviewContent(review?.content)}
              </div>
            </div>
          </div>
          
          <div className={styles.commentsHeader}>
            <span className={styles.commentsCount}>댓글 {comments.length}개</span>
          </div>

          {loading ? (
            <div className={styles.loading}>댓글을 불러오는 중...</div>
          ) : error ? (
            <div className={styles.error}>{error}</div>
          ) : comments.length === 0 ? (
            <div className={styles.noComments}>아직 댓글이 없습니다.</div>
          ) : (
            <div className={styles.commentsList}>
              {comments.flatMap(comment => renderCommentTree(comment, null, 0))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
} 