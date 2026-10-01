import React, { useState } from 'react';

export function LoginView({ onLogin, error, loading }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f8fafc',
        padding: '20px',
      }}
    >
      <div
        className="panel"
        style={{
          width: '420px',
          maxWidth: '100%',
          padding: '36px',
          borderRadius: 'var(--radius-xl)',
          boxShadow: '0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.04)',
          background: '#ffffff',
          border: '1px solid #e2e8f0',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
          <div
            className="brand-logo"
            style={{ width: '48px', height: '48px', fontSize: '22px', borderRadius: '12px' }}
          >
            S
          </div>
          <div>
            <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '18px', fontWeight: 800, color: 'var(--text-main)' }}>
              SABARMATI GAS
            </h2>
            <div style={{ fontSize: '10px', letterSpacing: '1.5px', color: 'var(--color-brand)', fontWeight: 700 }}>
              CNG OPERATIONS CONTROL
            </div>
          </div>
        </div>

        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: '22px', fontWeight: 800, marginBottom: '6px' }}>
          Operator Sign In
        </h1>
        <p style={{ fontSize: '13px', color: 'var(--text-subtle)', marginBottom: '24px' }}>
          Enter authorized station operator credentials to access real-time station control.
        </p>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            onLogin({ username, password });
          }}
          style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}
        >
          <div>
            <label style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', display: 'block', marginBottom: '6px' }}>
              OPERATOR USERNAME
            </label>
            <input
              autoComplete="username"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. operator1"
              style={{
                width: '100%',
                height: '42px',
                border: '1px solid var(--border-medium)',
                borderRadius: 'var(--radius-md)',
                padding: '0 12px',
                fontSize: '13px',
              }}
            />
          </div>

          <div>
            <label style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', display: 'block', marginBottom: '6px' }}>
              PASSWORD
            </label>
            <input
              autoComplete="current-password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{
                width: '100%',
                height: '42px',
                border: '1px solid var(--border-medium)',
                borderRadius: 'var(--radius-md)',
                padding: '0 12px',
                fontSize: '13px',
              }}
            />
          </div>

          {error && (
            <div style={{ padding: '10px', background: 'var(--color-danger-bg)', border: '1px solid var(--color-danger-border)', color: 'var(--color-danger)', borderRadius: 'var(--radius-md)', fontSize: '12px' }}>
              {error}
            </div>
          )}

          <button type="submit" className="btn-primary" disabled={loading} style={{ height: '44px', width: '100%', marginTop: '8px', fontSize: '14px' }}>
            {loading ? 'Authenticating...' : 'Sign In to Station Control ➔'}
          </button>
        </form>

        <div style={{ marginTop: '24px', textAlign: 'center', fontSize: '11px', color: 'var(--text-subtle)', borderTop: '1px solid var(--border-subtle)', paddingTop: '16px' }}>
          🔒 Protected Sabarmati Gas Limited Enterprise Portal
        </div>
      </div>
    </div>
  );
}
