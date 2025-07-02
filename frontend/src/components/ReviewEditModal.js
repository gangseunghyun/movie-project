import React, { useState } from 'react';
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:80';

function ReviewEditModal({ review, isOpen, onClose, onUpdate }) {
  const [content, setContent] = useState(review?.content || '');
  const [rating, setRating] = useState(review?.rating || 0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 모달이 열릴 때마다 리뷰 데이터로 초기화
  React.useEffect(() => {
    if (review) {
      setContent(review.content || '');
      setRating(review.rating || 0);
      setError('');
    }
  }, [review]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!content.trim()) {
      setError('리뷰 내용을 입력해주세요.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await axios.put(
        `http://localhost:80/api/reviews/${review.id}`,
        {
          content: content.trim(),
          rating: rating > 0 ? rating : null
        },
        { withCredentials: true }
      );

      if (response.data.success) {
        onUpdate(response.data);
        onClose();
      } else {
        setError(response.data.message || '리뷰 수정에 실패했습니다.');
      }
    } catch (err) {
      console.error('리뷰 수정 실패:', err);
      setError(err.response?.data?.message || '리뷰 수정에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setContent(review?.content || '');
    setRating(review?.rating || 0);
    setError('');
    onClose();
  };

  if (!isOpen || !review) return null;

  return (
    <div className="modal-overlay" onClick={handleCancel}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>리뷰 수정</h3>
          <button className="close-btn" onClick={handleCancel}>×</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>평점</label>
            <div className="rating-input">
              {[1, 2, 3, 4, 5].map(star => (
                <button
                  key={star}
                  type="button"
                  className={`star-btn ${star <= rating ? 'active' : ''}`}
                  onClick={() => setRating(star)}
                >
                  ★
                </button>
              ))}
              {rating > 0 && (
                <span className="rating-text">{rating}점</span>
              )}
            </div>
          </div>

          <div className="form-group">
            <label>리뷰 내용</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="영화에 대한 생각을 자유롭게 작성해주세요."
              rows={5}
              maxLength={1000}
            />
            <div className="char-count">{content.length}/1000</div>
          </div>

          {error && (
            <div className="error-message">{error}</div>
          )}

          <div className="modal-footer">
            <button type="button" className="cancel-btn" onClick={handleCancel}>
              취소
            </button>
            <button type="submit" className="submit-btn" disabled={loading}>
              {loading ? '수정 중...' : '수정하기'}
            </button>
          </div>
        </form>
      </div>

      <style>{`
        .modal-overlay {
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

        .modal-content {
          background: white;
          border-radius: 12px;
          padding: 24px;
          width: 90%;
          max-width: 500px;
          max-height: 80vh;
          overflow-y: auto;
        }

        .modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
          padding-bottom: 16px;
          border-bottom: 1px solid #eee;
        }

        .modal-header h3 {
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

        .form-group {
          margin-bottom: 20px;
        }

        .form-group label {
          display: block;
          margin-bottom: 8px;
          font-weight: 500;
          color: #333;
        }

        .rating-input {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .star-btn {
          background: none;
          border: none;
          font-size: 24px;
          color: #ddd;
          cursor: pointer;
          padding: 4px;
          transition: color 0.2s;
        }

        .star-btn.active {
          color: #ff2f6e;
        }

        .star-btn:hover {
          color: #ff2f6e;
        }

        .rating-text {
          margin-left: 8px;
          font-size: 14px;
          color: #666;
        }

        textarea {
          width: 100%;
          padding: 12px;
          border: 1px solid #ddd;
          border-radius: 8px;
          font-size: 14px;
          font-family: inherit;
          resize: vertical;
          min-height: 100px;
        }

        textarea:focus {
          outline: none;
          border-color: #ff2f6e;
        }

        .char-count {
          text-align: right;
          font-size: 12px;
          color: #999;
          margin-top: 4px;
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

        .modal-footer {
          display: flex;
          gap: 12px;
          justify-content: flex-end;
          margin-top: 24px;
          padding-top: 16px;
          border-top: 1px solid #eee;
        }

        .cancel-btn, .submit-btn {
          padding: 10px 20px;
          border-radius: 6px;
          font-size: 14px;
          font-weight: 500;
          cursor: pointer;
          border: none;
          transition: all 0.2s;
        }

        .cancel-btn {
          background-color: #f5f5f5;
          color: #666;
        }

        .cancel-btn:hover {
          background-color: #e5e5e5;
        }

        .submit-btn {
          background-color: #ff2f6e;
          color: white;
        }

        .submit-btn:hover:not(:disabled) {
          background-color: #e61e4d;
        }

        .submit-btn:disabled {
          background-color: #ccc;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
}

export default ReviewEditModal; 