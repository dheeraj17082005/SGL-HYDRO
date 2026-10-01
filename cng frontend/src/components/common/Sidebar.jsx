import React from 'react';

const navItems = [
  { name: 'Overview', icon: '◫' },
  { name: 'Live Monitoring', icon: '◉' },
  { name: 'Bays & Queue', icon: '⇄' },
  { name: 'Vehicle Verification', icon: '▣' },
  { name: 'Alerts', icon: '⚑' },
  { name: 'Compliance', icon: '▤' },
  { name: 'Audit Logs', icon: '≡' },
];

export function Sidebar({ page, setPage }) {
  const username = localStorage.getItem('sgl_username') || 'Operator';
  const role = localStorage.getItem('sgl_user_role') || 'Operations';

  return (
    <aside className="sidebar">
      <div className="brand-header">
        <div className="brand-logo">S</div>
        <div className="brand-info">
          <div className="brand-title">SABARMATI GAS</div>
          <div className="brand-subtitle">OPERATIONS CONTROL</div>
        </div>
      </div>

      <div className="nav-section-label">Station Navigation</div>

      <nav>
        {navItems.map((item) => (
          <button
            key={item.name}
            onClick={() => setPage(item.name)}
            className={page === item.name ? 'selected' : ''}
          >
            <span className="nav-icon">{item.icon}</span>
            <span>{item.name}</span>
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-card">
          <div className="user-avatar">{username.slice(0, 2).toUpperCase()}</div>
          <div className="user-meta">
            <span className="user-name">{username}</span>
            <span className="user-role">{role}</span>
          </div>
        </div>

        <div className="version-tag">
          <span>SGL PLATFORM</span>
          <span>v2.4.0</span>
        </div>
      </div>
    </aside>
  );
}
