import React, { useState, useEffect } from 'react';
import { alertApi } from '../api/services.js';
import { Badge } from '../components/common/Badge.jsx';
import { Plate } from '../components/common/Plate.jsx';
import { SystemErrorState } from '../components/common/SystemNotice.jsx';

export function AlertsView({ stationId, setPage }) {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  async function refresh() {
    setLoading(true);
    try {
      const list = await alertApi.list(stationId);
      setRows(Array.isArray(list) ? list : Array.isArray(list?.content) ? list.content : []);
      setError('');
    } catch (e) {
      setError(e.message || 'Alert register unavailable.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, [stationId]);

  async function resolve(id) {
    try {
      await alertApi.resolve(id);
      await refresh();
    } catch (e) {
      setError(e.message || 'Failed to resolve alert.');
    }
  }

  const openAlerts = rows.filter((a) => ['OPEN', 'IN_PROGRESS'].includes(String(a.status).toUpperCase()));
  const highPriority = rows.filter(
    (a) => ['HIGH', 'CRITICAL'].includes(String(a.severity).toUpperCase()) && ['OPEN', 'IN_PROGRESS'].includes(String(a.status).toUpperCase())
  );

  return (
    <div className="view-container">
      <div className="page-header">
        <div className="page-header-title">
          <div className="eyebrow">STATION SAFETY & INCIDENT CONTROL</div>
          <h1>Actionable Operational Alerts</h1>
          <p>Real-time station compliance warnings, expired hydro-test alerts, and security interlocks</p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn-outline" onClick={refresh}>
            ↻ Refresh Alerts
          </button>
        </div>
      </div>

      {error && <SystemErrorState message={error} onRetry={refresh} />}

      {/* Alert Summary Metrics */}
      <div className="metrics-grid">
        <div className="metric-card accent-amber">
          <div className="metric-header">OPEN ALERTS</div>
          <div className="metric-value">{openAlerts.length}</div>
          <div className="metric-note warn">Active incident queue</div>
        </div>

        <div className="metric-card accent-red">
          <div className="metric-header">HIGH PRIORITY / CRITICAL</div>
          <div className="metric-value">{highPriority.length}</div>
          <div className="metric-note bad">Requires immediate review</div>
        </div>

        <div className="metric-card accent-green">
          <div className="metric-header">RESOLVED ALERTS</div>
          <div className="metric-value">{rows.length - openAlerts.length}</div>
          <div className="metric-note good">In loaded alert history</div>
        </div>

        <div className="metric-card accent-blue">
          <div className="metric-header">TOTAL RECORDED</div>
          <div className="metric-value">{rows.length}</div>
          <div className="metric-note">Station alert register</div>
        </div>
      </div>

      {/* Actionable Alert Cards List */}
      <div className="alerts-grid">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-subtle)' }}>
            Loading station alert register...
          </div>
        ) : rows.length > 0 ? (
          rows.map((alert) => {
            const severity = String(alert.severity || 'MEDIUM').toUpperCase();
            const isOpen = ['OPEN', 'IN_PROGRESS'].includes(String(alert.status).toUpperCase());
            const plate = alert.vehicleNumber || alert.registrationNumber || 'RJ14AB8821';

            return (
              <div
                key={alert.id}
                className={`alert-card ${severity.toLowerCase()}`}
              >
                <div className="alert-info">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <Badge tone={['HIGH', 'CRITICAL'].includes(severity) ? 'fail' : severity === 'MEDIUM' ? 'warn' : 'neutral'}>
                      {severity} PRIORITY
                    </Badge>
                    <Plate plate={plate} />
                    <Badge tone={isOpen ? 'warn' : 'pass'}>{alert.status || 'OPEN'}</Badge>
                  </div>

                  <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-main)', marginTop: '4px' }}>
                    {alert.type ? String(alert.type).replaceAll('_', ' ') : 'Hydro-test certificate expired'}
                  </div>

                  <p style={{ fontSize: '12px', color: 'var(--text-subtle)' }}>
                    {alert.message || 'Vehicle cylinder hydro-test certificate has expired. Fueling automatically blocked by interlock.'}
                  </p>

                  <div className="alert-meta">
                    <span>Station #{alert.stationId || stationId}</span>
                    <span>·</span>
                    <span>{alert.createdAt ? new Date(alert.createdAt).toLocaleString('en-GB') : 'Just now'}</span>
                  </div>
                </div>

                <div className="alert-actions">
                  <button className="btn-outline" style={{ height: '32px', fontSize: '11px' }} onClick={() => setPage('Vehicle Verification')}>
                    View Vehicle
                  </button>
                  <button className="btn-outline" style={{ height: '32px', fontSize: '11px' }} onClick={() => setPage('Compliance')}>
                    View Compliance
                  </button>
                  {isOpen && (
                    <button className="btn-primary" style={{ height: '32px', fontSize: '11px' }} onClick={() => resolve(alert.id)}>
                      ✓ Acknowledge & Resolve
                    </button>
                  )}
                </div>
              </div>
            );
          })
        ) : (
          <div className="panel" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-subtle)' }}>
            ✓ No operational alerts recorded for this station.
          </div>
        )}
      </div>
    </div>
  );
}
