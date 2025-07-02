import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:80';

function ReviewList({ movieCd, currentUser }) {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [likeLoading, setLikeLoading] = useState({}); // 각 리뷰별 좋아요 로딩 상태

  useEffect(() => {
    if (movieCd) {
      fetchReviews();
    }
  }, [movieCd]);

  const fetchReviews = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.get(`${API_BASE_URL}/api/reviews/movie/${movieCd}`, { withCredentials: true });
      
      if (response.data.success) {
        setReviews(response.data.data);
      } else {
        setError('리뷰를 불러오는데 실패했습니다.');
      }
    } catch (err) {
      console.error('리뷰 목록 조회 실패:', err);
      setError('리뷰를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 리뷰 좋아요/취소 함수
  const handleLikeReview = async (reviewId, currentLiked) => {
    if (!currentUser) {
      alert('로그인이 필요합니다.');
      return;
    }

    setLikeLoading(prev => ({ ...prev, [reviewId]: true }));

    try {
      if (currentLiked) {
        // 좋아요 취소
        await axios.delete(`${API_BASE_URL}/api/reviews/dto/${reviewId}/like`, { withCredentials: true });
      } else {
        // 좋아요 추가
        await axios.post(`${API_BASE_URL}/api/reviews/dto/${reviewId}/like`, {}, { withCredentials: true });
      }

      // 리뷰 목록 새로고침
      fetchReviews();
    } catch (err) {
      console.error('좋아요 처리 실패:', err);
      alert('좋아요 처리에 실패했습니다.');
    } finally {
      setLikeLoading(prev => ({ ...prev, [reviewId]: false }));
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    
    // 전체 시간 차이 계산 (밀리초)
    const diffTime = now.getTime() - date.getTime();
    
    // 시간 단위별 계산
    const diffMinutes = Math.floor(diffTime / (1000 * 60));
    const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    
    console.log('날짜 디버깅:', {
      original: dateString,
      parsed: date,
      now: now,
      diffMinutes: diffMinutes,
      diffHours: diffHours,
      diffDays: diffDays
    });
    
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

  const renderStars = (rating) => {
    if (!rating) return null;
    
    return (
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
        {[1, 2, 3, 4, 5].map(star => (
          <span
            key={star}
            style={{
              color: star <= rating ? '#ff2f6e' : '#ddd',
              fontSize: 16,
              marginRight: 2
            }}
          >
            ★
          </span>
        ))}
        <span style={{ marginLeft: 8, fontSize: 14, color: '#666' }}>
          {rating}점
        </span>
      </div>
    );
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <div style={{ fontSize: 16, color: '#666' }}>리뷰를 불러오는 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <div style={{ fontSize: 16, color: '#ff2f6e' }}>{error}</div>
      </div>
    );
  }

  if (reviews.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <div style={{ fontSize: 16, color: '#666' }}>아직 리뷰가 없습니다.</div>
        <div style={{ fontSize: 14, color: '#999', marginTop: 8 }}>
          첫 번째 리뷰를 작성해보세요!
        </div>
      </div>
    );
  }

  return (
    <div className="review-list">
      <h3 style={{ marginBottom: 20, fontSize: 18, fontWeight: 600 }}>
        리뷰 ({reviews.length}개)
      </h3>
      
      {reviews.map((review) => (
        <div key={review.id} className="review-item">
          <div className="review-header">
            <div className="reviewer-info">
              <div className="reviewer-name">{review.userNickname}</div>
              <div className="review-date">{formatDate(review.createdAt)}</div>
            </div>
          </div>
          
          {renderStars(review.rating)}
          
          {review.content && (
            <div className="review-content">
              {review.content}
            </div>
          )}
          
          <div className="review-footer">
            <div className="review-actions">
              <button 
                className={`action-btn ${review.likedByMe ? 'liked' : ''}`}
                onClick={() => handleLikeReview(review.id, review.likedByMe)}
                disabled={likeLoading[review.id]}
              >
                <span style={{ marginRight: 4, color: review.likedByMe ? '#ff2f6e' : '#666' }}>
                  {review.likedByMe ? '❤️' : '🤍'}
                </span>
                좋아요 {review.likeCount > 0 && `(${review.likeCount})`}
              </button>
              <button className="action-btn">
                <span style={{ marginRight: 4 }}>💬</span>
                댓글
              </button>
            </div>
          </div>
        </div>
      ))}
      
      <style>{`
        .review-list {
          margin-top: 30px;
        }
        
        .review-item {
          border-bottom: 1px solid #f0f0f0;
          padding: 20px 0;
        }
        
        .review-item:last-child {
          border-bottom: none;
        }
        
        .review-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 12px;
        }
        
        .reviewer-info {
          display: flex;
          align-items: center;
          gap: 12px;
        }
        
        .reviewer-name {
          font-weight: 600;
          font-size: 14px;
          color: #333;
        }
        
        .review-date {
          font-size: 12px;
          color: #999;
        }
        
        .review-content {
          font-size: 14px;
          line-height: 1.6;
          color: #333;
          margin-bottom: 16px;
          white-space: pre-wrap;
        }
        
        .review-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        
        .review-actions {
          display: flex;
          gap: 16px;
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
        
        .action-btn:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
}

export default ReviewList; 