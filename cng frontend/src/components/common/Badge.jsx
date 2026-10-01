import React from 'react';

export function Badge({ children, tone = 'neutral', icon }) {
  const toneMap = {
    pass: 'badge-success',
    cleared: 'badge-success',
    eligible: 'badge-success',
    success: 'badge-success',
    valid: 'badge-success',
    
    warn: 'badge-warning',
    pending: 'badge-warning',
    fueling: 'badge-warning',
    in_queue: 'badge-warning',
    
    fail: 'badge-danger',
    blocked: 'badge-danger',
    critical: 'badge-danger',
    high: 'badge-danger',
    expired: 'badge-danger',

    neutral: 'badge-neutral'
  };

  const badgeClass = toneMap[tone.toLowerCase()] || 'badge-neutral';

  return (
    <span className={`badge ${badgeClass}`}>
      <span className="badge-dot" />
      {icon && <span className="badge-icon">{icon}</span>}
      {children}
    </span>
  );
}
