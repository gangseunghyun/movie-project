import React from 'react';

function ReviewPreview({ reviews, onShowAll }) {
  const previewReviews = reviews.slice(0, 3);

  return (
    <div>
      <h3>코멘트</h3>
      {previewReviews.map((review) => (
        <div key={review.id} className="review-preview-item">
          <div style={{ fontWeight: 'bold' }}>{review.userNickname}</div>
          <div>{review.content}</div>
        </div>
      ))}
      <button onClick={onShowAll} className="show-all-btn">
        더보기
      </button>
    </div>
  );
}

export default ReviewPreview; 