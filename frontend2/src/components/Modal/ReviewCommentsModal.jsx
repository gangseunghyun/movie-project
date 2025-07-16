import React, { useState, useEffect } from 'react';
import styles from './ReviewCommentsModal.module.css';
import userIcon from '../../assets/user_icon.png';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import commentIcon2 from '../../assets/comment_icon2.png';
import banner1 from '../../assets/banner1.jpg';

export default function ReviewCommentsModal({ isOpen, onClose, review, onCommentCountChange, handleLikeReview, handleReplyIconClick, user }) {
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

  const [likeLoading, setLikeLoading] = useState(false);
  const [likedByMe, setLikedByMe] = useState(review?.likedByMe || false);
  const [likeCount, setLikeCount] = useState(review?.likeCount || 0);

  useEffect(() => {
    if (isOpen && review) {
      console.log('Review 객체:', review); // 디버깅용
      fetchComments();
      fetchCurrentUser();
    }
  }, [isOpen, review]);

  useEffect(() => {
    setLikedByMe(review?.likedByMe || false);
    setLikeCount(review?.likeCount || 0);
  }, [review]);

  // comments 상태가 변경될 때마다 댓글 개수 업데이트를 강제로 트리거
  useEffect(() => {
    // comments 상태가 변경되면 강제로 리렌더링을 트리거
    console.log('댓글 목록 업데이트됨, 총 개수:', getTotalCommentCount(comments));
  }, [comments]);

  useEffect(() => {
    if (onCommentCountChange && review) {
      onCommentCountChange(review.id, getTotalCommentCount(comments));
    }
  }, [comments]);

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
    const diffMinutes = Math.floor(diffTime / (1000 * 60));
    const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffMinutes < 1) return '방금 전';
    if (diffMinutes < 60) return `${diffMinutes}분 전`;
    if (diffHours < 24) return `${diffHours}시간 전`;
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

  // 좋아요/댓글 버튼 로직을 영화상세 카드와 동일하게 적용
  const handleLike = async () => {
    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }
    if (handleLikeReview && review) {
      await handleLikeReview(review.id, likedByMe);
      // 상태 즉시 반영
      setLikedByMe(prev => !prev);
      setLikeCount(prev => prev + (likedByMe ? -1 : 1));
    }
  };
  const handleCommentClick = () => {
    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }
    if (handleReplyIconClick && review) {
      handleReplyIconClick(null, review.id);
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
        // 삭제 성공 시 즉시 로컬 상태에서 해당 댓글 제거
        setComments(prevComments => {
          const removeCommentRecursively = (comments, targetId) => {
            return comments.filter(comment => {
              if (comment.id === targetId) {
                return false; // 삭제할 댓글 제거
              }
              if (comment.replies && comment.replies.length > 0) {
                comment.replies = removeCommentRecursively(comment.replies, targetId);
              }
              return true;
            });
          };
          return removeCommentRecursively([...prevComments], commentId);
        });
        
        // 백그라운드에서 댓글 목록 새로고침 (동기화용)
        setTimeout(() => {
          fetchComments();
        }, 100);
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

  // 댓글 좋아요 핸들러 추가
  const handleCommentLike = async (commentId, likedByMe) => {
    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    try {
      let res;
      if (likedByMe) {
        // 댓글 좋아요 취소 (DELETE)
        res = await fetch(`http://localhost:80/api/comments/${commentId}/like?userId=${currentUser.id}`, {
          method: 'DELETE',
          credentials: 'include',
        });
      } else {
        // 댓글 좋아요 (POST)
        res = await fetch(`http://localhost:80/api/comments/${commentId}/like`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({
            userId: currentUser.id
          }),
        });
      }
      
      if (res.ok) {
        fetchComments(); // 댓글 목록 새로고침
      } else if (res.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('좋아요 처리 실패');
      }
    } catch (e) {
      alert('네트워크 오류');
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
            renderCommentText(comment, parentNickname)
          )}
          <div className={styles.actionButtons}>
            {/* 댓글의 좋아요 버튼 수정 */}
            <div 
              className={`${styles.likeButton} ${!currentUser ? styles.disabled : ''}`} 
              onClick={() => handleCommentLike(comment.id, comment.likedByMe)}
            >
              <img
                src={comment.likedByMe ? likeIconTrue : likeIcon}
                alt="좋아요"
                className={styles.likeIcon}
              />
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
        items.push(...renderCommentTree(child, comment.userNickname || comment.user || '익명', depth + 1))
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

  // 최상위 댓글 개수 계산 (대댓글 제외)
  const getTotalCommentCount = (comments) => {
    return comments.length; // 최상위 댓글만 카운트
  };

  // 클린봇 차단된 댓글 렌더링 함수
  const renderCommentText = (comment, parentNickname) => {
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
          {parentNickname && (
            <span className={styles.replyTo}>@{parentNickname}</span>
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
          {/* 좋아요/댓글 개수 한 줄 텍스트 */}
          <div className={styles.countsLine} style={{ margin: '8px 0 0 0', fontSize: '15px', color: '#555', fontWeight: 500 }}>
            좋아요 {likeCount}  댓글 {getTotalCommentCount(comments)}
          </div>

          {/* 좋아요/댓글 버튼 추가 */}
          <div style={{ display: 'flex', gap: 16, margin: '16px 0 12px 0' }}>
            <button
              className={styles.likeButton}
              onClick={e => {
                e.stopPropagation();
                if (!user) {
                  alert('로그인이 필요합니다.');
                  return;
                }
                handleLike();
              }}
              style={{ display: 'flex', alignItems: 'center', background: 'none', border: 'none', cursor: 'pointer' }}
            >
              <img
                src={likedByMe ? likeIconTrue : likeIcon}
                alt="좋아요"
                className={styles.likeIcon}
                style={{ width: 22, height: 22, marginRight: 4 }}
              />
            </button>
            <button
              className={styles.replyButton}
              onClick={(e) => {
                console.log('댓글 아이콘 클릭됨!');
                e.stopPropagation();
                
                if (!user) {
                  alert('로그인이 필요합니다.');
                  return;
                }
                
                // 부모 컴포넌트의 댓글 작성 모달 열기
                if (handleReplyIconClick && review) {
                  handleReplyIconClick(e, review.id);
                }
              }}
              style={{ display: 'flex', alignItems: 'center', background: 'none', border: 'none', cursor: 'pointer' }}
            >
              <img
                src={commentIcon2}
                alt="댓글"
                style={{ width: 22, height: 22, marginRight: 4 }}
              />
            </button>
          </div>

          <div className={styles.commentsHeader}>
            <span className={styles.commentsCount}>댓글 {getTotalCommentCount(comments)}개</span>
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