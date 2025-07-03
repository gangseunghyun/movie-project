import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:80';

function CommentList({ reviewId, currentUser, isOpen, onClose }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [newComment, setNewComment] = useState('');
  const [editingComment, setEditingComment] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [likeLoading, setLikeLoading] = useState({});

  useEffect(() => {
    if (isOpen && reviewId) {
      fetchComments();
    }
  }, [isOpen, reviewId]);

  const fetchComments = async () => {
    setLoading(true);
    setError('');
    
    try {
      const url = currentUser 
        ? `http://localhost:80/api/comments/review/${reviewId}?userId=${currentUser.id}`
        : `http://localhost:80/api/comments/review/${reviewId}`;
        
      const response = await axios.get(url, { withCredentials: true });
      
      if (response.data.success) {
        setComments(response.data.data);
      } else {
        setError('댓글을 불러오는데 실패했습니다.');
      }
    } catch (err) {
      console.error('댓글 목록 조회 실패:', err);
      setError('댓글을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    
    if (!newComment.trim()) {
      alert('댓글 내용을 입력해주세요.');
      return;
    }

    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    try {
      const response = await axios.post(`http://localhost:80/api/comments`, {
        reviewId: reviewId,
        userId: currentUser.id,
        content: newComment.trim()
      }, { withCredentials: true });

      if (response.data.success) {
        setNewComment('');
        fetchComments();
      } else {
        alert(response.data.message || '댓글 작성에 실패했습니다.');
      }
    } catch (err) {
      console.error('댓글 작성 실패:', err);
      alert(err.response?.data?.message || '댓글 작성에 실패했습니다.');
    }
  };

  const handleEditComment = async (commentId) => {
    if (!editContent.trim()) {
      alert('댓글 내용을 입력해주세요.');
      return;
    }

    try {
      const response = await axios.put(`http://localhost:80/api/comments/${commentId}`, {
        userId: currentUser.id,
        content: editContent.trim()
      }, { withCredentials: true });

      if (response.data.success) {
        setEditingComment(null);
        setEditContent('');
        fetchComments();
      } else {
        alert(response.data.message || '댓글 수정에 실패했습니다.');
      }
    } catch (err) {
      console.error('댓글 수정 실패:', err);
      alert(err.response?.data?.message || '댓글 수정에 실패했습니다.');
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('정말로 이 댓글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      const response = await axios.delete(`http://localhost:80/api/comments/${commentId}?userId=${currentUser.id}`, {
        withCredentials: true
      });

      if (response.data.success) {
        alert('댓글이 삭제되었습니다.');
        fetchComments();
      } else {
        alert(response.data.message || '댓글 삭제에 실패했습니다.');
      }
    } catch (err) {
      console.error('댓글 삭제 실패:', err);
      alert(err.response?.data?.message || '댓글 삭제에 실패했습니다.');
    }
  };

  const handleLikeComment = async (commentId, currentLiked) => {
    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    setLikeLoading(prev => ({ ...prev, [commentId]: true }));

    try {
      if (currentLiked) {
        await axios.delete(`http://localhost:80/api/comments/${commentId}/like?userId=${currentUser.id}`, {
          withCredentials: true
        });
      } else {
        await axios.post(`http://localhost:80/api/comments/${commentId}/like`, {
          userId: currentUser.id
        }, { withCredentials: true });
      }

      fetchComments();
    } catch (err) {
      console.error('댓글 좋아요 처리 실패:', err);
      alert('댓글 좋아요 처리에 실패했습니다.');
    } finally {
      setLikeLoading(prev => ({ ...prev, [commentId]: false }));
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    
    const diffTime = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffTime / (1000 * 60));
    const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffMinutes < 1) {
      return '방금 전';
    } else if (diffMinutes < 60) {
      return `${diffMinutes}분 전`;
    } else if (diffHours < 24) {
      return `${diffHours}시간 전`;
    } else if (diffDays === 1) {
      return '어제';
    } else if (diffDays < 7) {
      return `${diffDays}일 전`;
    } else {
      return date.toLocaleDateString('ko-KR');
    }
  };

  if (!isOpen) return null;

  return (
    <div className="comment-modal-overlay" onClick={onClose}>
      <div className="comment-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="comment-modal-header">
          <h3>댓글 ({comments.length}개)</h3>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        <div className="comment-modal-body">
          {/* 댓글 작성 폼 */}
          {currentUser && (
            <form onSubmit={handleSubmitComment} className="comment-form">
              <textarea
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                placeholder="댓글을 작성해주세요..."
                rows={3}
                maxLength={500}
              />
              <div className="comment-form-footer">
                <span className="char-count">{newComment.length}/500</span>
                <button type="submit" className="submit-btn">댓글 작성</button>
              </div>
            </form>
          )}

          {error && (
            <div className="error-message">{error}</div>
          )}

          {/* 댓글 목록 */}
          {loading ? (
            <div className="loading">댓글을 불러오는 중...</div>
          ) : comments.length === 0 ? (
            <div className="no-comments">아직 댓글이 없습니다.</div>
          ) : (
            <div className="comments-list">
              {comments.map((comment) => (
                <div key={comment.id} className="comment-item">
                  <div className="comment-header">
                    <div className="commenter-info">
                      <div className="commenter-name">{comment.userNickname}</div>
                      <div className="comment-date">{formatDate(comment.createdAt)}</div>
                    </div>
                  </div>
                  
                  <div className="comment-content">
                    {editingComment === comment.id ? (
                      <div className="edit-form">
                        <textarea
                          value={editContent}
                          onChange={(e) => setEditContent(e.target.value)}
                          rows={2}
                          maxLength={500}
                        />
                        <div className="edit-actions">
                          <button 
                            onClick={() => handleEditComment(comment.id)}
                            className="save-btn"
                          >
                            저장
                          </button>
                          <button 
                            onClick={() => {
                              setEditingComment(null);
                              setEditContent('');
                            }}
                            className="cancel-btn"
                          >
                            취소
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div>{comment.content}</div>
                    )}
                  </div>
                  
                  <div className="comment-footer">
                    <div className="comment-actions">
                      <button 
                        className={`action-btn ${comment.likedByMe ? 'liked' : ''}`}
                        onClick={() => handleLikeComment(comment.id, comment.likedByMe)}
                        disabled={likeLoading[comment.id]}
                      >
                        <span style={{ marginRight: 4, color: comment.likedByMe ? '#ff2f6e' : '#666' }}>
                          {comment.likedByMe ? '❤️' : '🤍'}
                        </span>
                        좋아요 {comment.likeCount > 0 && `(${comment.likeCount})`}
                      </button>
                      
                      {currentUser && currentUser.id === comment.userId && (
                        <>
                          <button 
                            className="action-btn edit-btn"
                            onClick={() => {
                              setEditingComment(comment.id);
                              setEditContent(comment.content);
                            }}
                          >
                            <span style={{ marginRight: 4 }}>✏️</span>
                            수정
                          </button>
                          <button 
                            className="action-btn delete-btn"
                            onClick={() => handleDeleteComment(comment.id)}
                          >
                            <span style={{ marginRight: 4 }}>🗑️</span>
                            삭제
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <style>{`
          .comment-modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: rgba(0, 0, 0, 0.5);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 1000;
          }

          .comment-modal-content {
            background: white;
            border-radius: 12px;
            width: 90%;
            max-width: 600px;
            max-height: 80vh;
            overflow-y: auto;
          }

          .comment-modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 20px 24px;
            border-bottom: 1px solid #eee;
          }

          .comment-modal-header h3 {
            margin: 0;
            font-size: 18px;
            font-weight: 600;
          }

          .close-btn {
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
            color: #666;
            padding: 0;
            width: 30px;
            height: 30px;
            display: flex;
            align-items: center;
            justify-content: center;
          }

          .close-btn:hover {
            color: #333;
          }

          .comment-modal-body {
            padding: 24px;
          }

          .comment-form {
            margin-bottom: 24px;
            padding-bottom: 20px;
            border-bottom: 1px solid #eee;
          }

          .comment-form textarea {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 8px;
            font-size: 14px;
            font-family: inherit;
            resize: vertical;
            min-height: 80px;
          }

          .comment-form textarea:focus {
            outline: none;
            border-color: #ff2f6e;
          }

          .comment-form-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 12px;
          }

          .char-count {
            font-size: 12px;
            color: #999;
          }

          .submit-btn {
            background-color: #ff2f6e;
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 6px;
            font-size: 14px;
            cursor: pointer;
          }

          .submit-btn:hover {
            background-color: #e61e4d;
          }

          .error-message {
            color: #ff2f6e;
            font-size: 14px;
            margin-bottom: 16px;
            padding: 8px 12px;
            background-color: #fff5f5;
            border-radius: 6px;
            border: 1px solid #ffebeb;
          }

          .loading, .no-comments {
            text-align: center;
            padding: 40px 0;
            color: #666;
            font-size: 16px;
          }

          .comments-list {
            display: flex;
            flex-direction: column;
            gap: 16px;
          }

          .comment-item {
            border-bottom: 1px solid #f0f0f0;
            padding-bottom: 16px;
          }

          .comment-item:last-child {
            border-bottom: none;
          }

          .comment-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 8px;
          }

          .commenter-info {
            display: flex;
            align-items: center;
            gap: 12px;
          }

          .commenter-name {
            font-weight: 600;
            font-size: 14px;
            color: #333;
          }

          .comment-date {
            font-size: 12px;
            color: #999;
          }

          .comment-content {
            font-size: 14px;
            line-height: 1.6;
            color: #333;
            margin-bottom: 12px;
          }

          .edit-form textarea {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 14px;
            font-family: inherit;
            resize: vertical;
            min-height: 60px;
          }

          .edit-actions {
            display: flex;
            gap: 8px;
            margin-top: 8px;
          }

          .save-btn, .cancel-btn {
            padding: 6px 12px;
            border-radius: 4px;
            font-size: 12px;
            border: none;
            cursor: pointer;
          }

          .save-btn {
            background-color: #ff2f6e;
            color: white;
          }

          .cancel-btn {
            background-color: #f5f5f5;
            color: #666;
          }

          .comment-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
          }

          .comment-actions {
            display: flex;
            gap: 12px;
          }

          .action-btn {
            background: none;
            border: none;
            font-size: 12px;
            color: #666;
            cursor: pointer;
            padding: 4px 8px;
            border-radius: 4px;
            transition: background-color 0.2s;
          }

          .action-btn:hover {
            background-color: #f5f5f5;
          }

          .action-btn.liked {
            color: #ff2f6e;
          }

          .action-btn.edit-btn {
            color: #666;
          }

          .action-btn.delete-btn {
            color: #ff4757;
          }

          .action-btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
          }
        `}</style>
      </div>
    </div>
  );
}

export default CommentList; 