import React, { useState } from 'react';

const FullStar = () => (
  <svg width="32" height="32" viewBox="0 0 24 24" fill="#fbc02d" stroke="#fbc02d"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
);
const HalfStar = () => (
  <svg width="32" height="32" viewBox="0 0 24 24">
    <defs>
      <linearGradient id="half">
        <stop offset="50%" stopColor="#fbc02d"/>
        <stop offset="50%" stopColor="#e0e0e0"/>
      </linearGradient>
    </defs>
    <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" fill="url(#half)" stroke="#fbc02d"/>
  </svg>
);
const EmptyStar = () => (
  <svg width="32" height="32" viewBox="0 0 24 24" fill="#e0e0e0" stroke="#e0e0e0"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
);

function StarRating({ movieCd, userRating, onChange, average, count, disabled }) {
  const [hovered, setHovered] = useState(0);
  // 별 5개, 0.5점 단위
  const getStarType = (index) => {
    const value = hovered || userRating || 0;
    if (value >= index + 1) return 'full';
    if (value >= index + 0.5) return 'half';
    return 'empty';
  };

  return (
    <div style={{ textAlign: 'center', margin: '16px 0 8px 0' }}>
      <div style={{ fontSize: '2.2rem', color: '#fbc02d', letterSpacing: 2, display: 'inline-block' }}>
        {[0, 1, 2, 3, 4].map((i) => {
          const starType = getStarType(i);
          return (
            <span
              key={i}
              style={{ cursor: disabled ? 'default' : 'pointer', display: 'inline-block', width: '2.2rem' }}
              onMouseMove={e => {
                if (disabled) return;
                const { left, width } = e.currentTarget.getBoundingClientRect();
                const x = e.clientX - left;
                setHovered(x < width / 2 ? i + 0.5 : i + 1);
              }}
              onMouseLeave={() => !disabled && setHovered(0)}
              onClick={() => !disabled && onChange(hovered || i + 1)}
              role="button"
              aria-label={`${i + 1}점`}
            >
              {starType === 'full' && <FullStar />}
              {starType === 'half' && <HalfStar />}
              {starType === 'empty' && <EmptyStar />}
            </span>
          );
        })}
      </div>
      <div style={{ fontSize: '1.05rem', color: '#888', marginTop: 4 }}>
        {userRating !== undefined && userRating !== null ? `${userRating.toFixed(1)}점` : '평가하기'}
      </div>
      {/* 평균 별점 표시 */}
      {average !== undefined && average !== null ? (
        <span>
          {(() => {
            // 0.5 단위로 반올림
            const rounded = Math.round(average * 2) / 2;
            // x.0이면 정수로, x.5면 그대로
            return (rounded % 1 === 0) ? rounded.toString().replace('.0', '') : rounded.toFixed(1);
          })()}
        </span>
      ) : '-'}
      <span style={{ fontSize: '1rem', color: '#888', marginLeft: 6 }}>
        {count !== undefined && count !== null ? `평균 별점(${count}명)` : '평균 별점(0명)'}
      </span>
    </div>
  );
}

export default StarRating; 