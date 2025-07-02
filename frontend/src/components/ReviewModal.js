import React, { useState } from 'react';

function ReviewModal({ movieTitle, onSave, onClose }) {
  const [content, setContent] = useState('');
  const [rating, setRating] = useState(5);
  const [spoiler, setSpoiler] = useState(false);
  const maxLength = 10000;

  return (
    <div className="review-modal-overlay">
      <div className="review-modal">
        <button className="close-btn" onClick={onClose}>&times;</button>
        <h2>{movieTitle}</h2>
        <div className="review-modal-desc">이 작품에 대한 생각을 자유롭게 표현해주세요.</div>
        <div className="star-rating">
          {[1,2,3,4,5].map(star => (
            <span
              key={star}
              style={{ color: star <= rating ? '#ff2f6e' : '#eee', fontSize: 36, cursor: 'pointer' }}
              onClick={() => setRating(star)}
            >★</span>
          ))}
        </div>
        <textarea
          className="review-textarea"
          placeholder="대단한 작품이군요! 감동을 글로 남겨보세요"
          value={content}
          onChange={e => setContent(e.target.value)}
          maxLength={maxLength}
        />
        <div className="review-modal-footer">
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <span style={{ fontSize: 24, marginRight: 8 }}>✗</span>
            <span style={{ marginRight: 8 }}>스포일러</span>
            <label className="switch">
              <input type="checkbox" checked={spoiler} onChange={e => setSpoiler(e.target.checked)} />
              <span className="slider round"></span>
            </label>
          </div>
          <span style={{ color: '#888', marginLeft: 16 }}>임시저장됨  {content.length}/{maxLength}</span>
          <button
            className="save-btn"
            style={{ background: '#ff2f6e', color: 'white', border: 'none', borderRadius: 10, padding: '12px 32px', fontSize: 18, marginLeft: 16 }}
            onClick={() => onSave(content, rating, spoiler)}
            disabled={!content.trim()}
          >저장</button>
        </div>
      </div>
      <style>{`
        .review-modal-overlay {
          position: fixed; left: 0; top: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.2); z-index: 1000; display: flex; align-items: center; justify-content: center;
        }
        .review-modal {
          background: white; border-radius: 16px; padding: 32px; min-width: 400px; max-width: 600px; box-shadow: 0 4px 32px rgba(0,0,0,0.15); position: relative;
        }
        .close-btn {
          position: absolute; right: 16px; top: 16px; background: #eee; border: none; border-radius: 50%; width: 32px; height: 32px; font-size: 22px; cursor: pointer;
        }
        .review-modal-desc { color: #888; margin-bottom: 16px; }
        .review-textarea {
          width: 100%; min-height: 120px; font-size: 18px; border-radius: 8px; border: 1px solid #eee; padding: 16px; margin-bottom: 16px;
        }
        .review-modal-footer { display: flex; align-items: center; justify-content: space-between; }
        .switch { position: relative; display: inline-block; width: 40px; height: 24px; }
        .switch input { opacity: 0; width: 0; height: 0; }
        .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background: #ccc; border-radius: 24px; transition: .4s; }
        .slider:before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: .4s; }
        input:checked + .slider { background: #ff2f6e; }
        input:checked + .slider:before { transform: translateX(16px); }
      `}</style>
    </div>
  );
}

export default ReviewModal; 