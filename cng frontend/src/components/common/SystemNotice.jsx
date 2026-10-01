import React from 'react';

export function SystemErrorState({ message, onRetry, lastSync }) {
  return (
    <div className="system-error-card" role="alert">
      <div className="system-error-icon">!</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
        <h3 style={{ fontSize: '16px', fontWeight: 800, color: 'var(--color-danger)' }}>
          STATION SERVICE UNAVAILABLE
        </h3>
        <p style={{ fontSize: '13px', color: 'var(--text-subtle)', maxWidth: '380px' }}>
          {message || 'Live station data could not be retrieved from the backend service. Check network connection or backend state.'}
        </p>
      </div>
      
      {lastSync && (
        <div style={{ fontSize: '11px', color: 'var(--text-subtle)', fontWeight: 600 }}>
          Last synchronized: <span style={{ fontFamily: 'var(--font-mono)' }}>{lastSync}</span>
        </div>
      )}

      {onRetry && (
        <button className="btn-outline" onClick={onRetry}>
          ↻ RETRY CONNECTION
        </button>
      )}
    </div>
  );
}

export function DemoDataNotice({ message }) {
  return (
    <div className="demo-data-badge" role="status">
      <span>⚠️ DEMO DATA</span>
      <span>·</span>
      <span>{message || 'Displaying simulated station vehicle records.'}</span>
    </div>
  );
}
