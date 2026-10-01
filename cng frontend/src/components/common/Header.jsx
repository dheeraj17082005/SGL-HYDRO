import React, { useState, useEffect } from 'react';

export function Header({ page, onLogout, station, stations, onStationChange }) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const timeStr = now.toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    timeZone: 'Asia/Kolkata',
  });

  const dateStr = now.toLocaleDateString('en-GB', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'Asia/Kolkata',
  }).toUpperCase();

  const username = localStorage.getItem('sgl_username') || 'Operator';
  const role = localStorage.getItem('sgl_user_role') || 'Station Operator';

  return (
    <header className="topbar">
      <div className="breadcrumbs">
        <span>Operations</span>
        <span>/</span>
        <strong>{page}</strong>
      </div>

      <div className="topbar-actions">
        {stations?.length > 1 && (
          <div className="station-select-wrapper">
            <span style={{ fontSize: '12px' }}>📍</span>
            <select
              className="station-select"
              aria-label="Select station"
              value={station?.id || ''}
              onChange={(e) => onStationChange?.(Number(e.target.value))}
            >
              {stations.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name} {item.code ? `(${item.code})` : ''}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="system-clock">
          <div className="system-clock-time">{timeStr}</div>
          <div className="system-clock-date">{dateStr}</div>
        </div>

        <button className="btn-signout" onClick={onLogout} title={`Signed in as ${username} (${role})`}>
          <div className="user-avatar" style={{ width: '22px', height: '22px', fontSize: '10px' }}>
            {username.slice(0, 2).toUpperCase()}
          </div>
          <span>Sign out</span>
        </button>
      </div>
    </header>
  );
}
