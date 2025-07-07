import React from 'react';
import './ReasonBadges.css';

export default function ReasonBadges({ reasons, max = 3 }) {
  if (!reasons || reasons.length === 0) return null;
  const visible = reasons.slice(0, max);
  const hiddenCount = reasons.length - max;

  return (
    <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
      {visible.map((reason, idx) => (
        <span key={idx} className={`badge badge-${reason}`}>{reason}</span>
      ))}
      {hiddenCount > 0 && (
        <span className="badge badge-plus">+{hiddenCount}</span>
      )}
    </div>
  );
} 