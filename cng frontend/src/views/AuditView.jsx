import React, { useState, useEffect } from 'react';
import { auditApi } from '../api/services.js';
import { SystemErrorState } from '../components/common/SystemNotice.jsx';

export function AuditView({ stationId }) {
  const [pageData, setPageData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    auditApi
      .list(stationId, 'size=50&page=0')
      .then((d) => active && setPageData(d))
      .catch((e) => active && setError(e.message || 'Audit trail unavailable.'));
    return () => {
      active = false;
    };
  }, [stationId]);

  const rows = pageData?.content || [];

  return (
    <div className="view-container">
      <div className="page-header">
        <div className="page-header-title">
          <div className="eyebrow">IMMUTABLE GOVERNANCE TRAIL</div>
          <h1>Station Operational Audit Logs</h1>
          <p>Traceable history of operator actions, ANPR detections, bay assignments, and compliance overrides</p>
        </div>
      </div>

      {error && <SystemErrorState message={error} />}

      <div className="panel">
        <div className="panel-header">
          <div>
            <div className="panel-title">Audit Activity Register</div>
            <div className="panel-subtitle">{pageData?.totalElements ?? rows.length} logged entries</div>
          </div>
        </div>

        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>TIMESTAMP</th>
                <th>ENTITY TYPE</th>
                <th>ACTION PERFORMED</th>
                <th>OPERATIONAL DETAILS</th>
                <th>OPERATOR / USER</th>
              </tr>
            </thead>
            <tbody>
              {rows.length > 0 ? (
                rows.map((r) => (
                  <tr key={r.id}>
                    <td style={{ fontSize: '11px', color: 'var(--text-subtle)', fontFamily: 'var(--font-mono)' }}>
                      {r.timestamp ? new Date(r.timestamp).toLocaleString('en-GB') : '—'}
                    </td>
                    <td>
                      <strong style={{ fontSize: '12px' }}>
                        {r.entityType || 'SYSTEM'} {r.entityId ? `#${r.entityId}` : ''}
                      </strong>
                    </td>
                    <td>
                      <strong style={{ color: 'var(--color-brand)' }}>{r.action || 'EVENT'}</strong>
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      {r.details || r.description || '—'}
                    </td>
                    <td style={{ fontSize: '11px', fontWeight: 600 }}>
                      {r.username || r.userName || r.userId || 'System Engine'}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-subtle)' }}>
                    No audit log entries recorded.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
