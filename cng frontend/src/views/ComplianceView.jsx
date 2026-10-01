import React, { useState, useEffect } from 'react';
import { dashboardApi, journeyApi } from '../api/services.js';
import { Badge } from '../components/common/Badge.jsx';
import { Plate } from '../components/common/Plate.jsx';
import { SystemErrorState } from '../components/common/SystemNotice.jsx';

export function ComplianceView({ stationId }) {
  const [metrics, setMetrics] = useState(null);
  const [journeys, setJourneys] = useState([]);
  const [query, setQuery] = useState('');
  const [error, setError] = useState('');
  const [selectedJourney, setSelectedJourney] = useState(null);

  useEffect(() => {
    let active = true;
    Promise.all([dashboardApi.metrics(stationId), journeyApi.list(stationId)])
      .then(([m, j]) => {
        if (active) {
          setMetrics(m);
          setJourneys(Array.isArray(j) ? j : []);
        }
      })
      .catch((e) => active && setError(e.message));
    return () => {
      active = false;
    };
  }, [stationId]);

  const records = journeys.filter((j) => !query || String(j.registrationNumber).includes(query.toUpperCase()));

  return (
    <div className="view-container">
      <div className="page-header">
        <div className="page-header-title">
          <div className="eyebrow">REGULATORY GOVERNANCE</div>
          <h1>Station Compliance & Audit Investigation</h1>
          <p>Investigate compliance decisions, statutory hydro-test validity, and safety audit history</p>
        </div>
      </div>

      {error && <SystemErrorState message={error} />}

      {/* Compliance Metrics */}
      <div className="metrics-grid">
        <div className="metric-card accent-blue">
          <div className="metric-header">CHECKED TODAY</div>
          <div className="metric-value">{metrics?.totalVehiclesProcessedToday ?? journeys.length}</div>
          <div className="metric-note">Total ANPR verified</div>
        </div>

        <div className="metric-card accent-green">
          <div className="metric-header">ELIGIBLE & CLEARED</div>
          <div className="metric-value">{metrics?.eligibleVehicles ?? journeys.filter((j) => j.complianceStatus === 'ELIGIBLE').length}</div>
          <div className="metric-note good">Cleared for refueling</div>
        </div>

        <div className="metric-card accent-red">
          <div className="metric-header">BLOCKED VEHICLES</div>
          <div className="metric-value">{metrics?.blockedVehicles ?? journeys.filter((j) => j.complianceStatus !== 'ELIGIBLE').length}</div>
          <div className="metric-note bad">Compliance interlock</div>
        </div>

        <div className="metric-card accent-amber">
          <div className="metric-header">HYDRO TEST ISSUES</div>
          <div className="metric-value">{(metrics?.expiredHydroTestCount ?? 0) + (metrics?.missingHydroTestCount ?? 0)}</div>
          <div className="metric-note warn">
            {metrics?.expiredHydroTestCount ?? 0} expired · {metrics?.missingHydroTestCount ?? 0} missing
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="panel" style={{ padding: '16px 20px' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <strong style={{ fontSize: '12px', color: 'var(--text-subtle)' }}>SEARCH RECORDS:</strong>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value.replace(/[\s-]/g, '').toUpperCase())}
            placeholder="Search license plate number..."
            style={{
              height: '36px',
              border: '1px solid var(--border-medium)',
              borderRadius: 'var(--radius-md)',
              padding: '0 12px',
              width: '260px',
              fontFamily: 'var(--font-mono)',
            }}
          />
          <span style={{ fontSize: '12px', color: 'var(--text-subtle)', marginLeft: 'auto' }}>
            Showing {records.length} records
          </span>
        </div>
      </div>

      {/* Compliance Log Table */}
      <div className="panel">
        <div className="panel-header">
          <div>
            <div className="panel-title">Station Vehicle Journeys & Compliance History</div>
            <div className="panel-subtitle">Click any row to inspect complete audit trail</div>
          </div>
        </div>

        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>VEHICLE PLATE</th>
                <th>ENTRY TIMESTAMP</th>
                <th>JOURNEY STAGE</th>
                <th>COMPLIANCE DECISION</th>
                <th>ASSIGNED BAY</th>
                <th>ACTION</th>
              </tr>
            </thead>
            <tbody>
              {records.length > 0 ? (
                records.map((j) => {
                  const isEligible = String(j.complianceStatus).toUpperCase() === 'ELIGIBLE';
                  return (
                    <tr
                      key={j.id}
                      style={{ cursor: 'pointer' }}
                      onClick={() => setSelectedJourney(j)}
                    >
                      <td>
                        <Plate plate={j.registrationNumber || 'UNKNOWN'} />
                      </td>
                      <td style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                        {j.entryTime ? new Date(j.entryTime).toLocaleString('en-GB') : '—'}
                      </td>
                      <td>
                        <strong style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                          {String(j.status || '—').replaceAll('_', ' ')}
                        </strong>
                      </td>
                      <td>
                        <Badge tone={isEligible ? 'pass' : 'fail'}>
                          {String(j.complianceStatus || 'UNKNOWN').replaceAll('_', ' ')}
                        </Badge>
                      </td>
                      <td>{j.assignedBayNumber ? `Bay ${j.assignedBayNumber}` : '—'}</td>
                      <td>
                        <button className="btn-outline" style={{ height: '28px', padding: '0 10px', fontSize: '11px' }}>
                          Inspect ➔
                        </button>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-subtle)' }}>
                    No matching compliance records found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Investigation Details Modal / Drawer if a row is selected */}
      {selectedJourney && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(15, 23, 42, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            backdropFilter: 'blur(4px)',
          }}
          onClick={() => setSelectedJourney(null)}
        >
          <div
            className="panel"
            style={{ width: '540px', maxWidth: '90%', padding: '24px', background: '#fff' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div className="eyebrow">COMPLIANCE INVESTIGATION RECORD</div>
              <button className="btn-outline" style={{ height: '28px', padding: '0 8px' }} onClick={() => setSelectedJourney(null)}>
                ✕ Close
              </button>
            </div>

            <div style={{ display: 'flex', gap: '16px', alignItems: 'center', marginBottom: '20px' }}>
              <Plate plate={selectedJourney.registrationNumber} size="large" />
              <Badge tone={selectedJourney.complianceStatus === 'ELIGIBLE' ? 'pass' : 'fail'}>
                {selectedJourney.complianceStatus}
              </Badge>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px', borderTop: '1px solid var(--border-subtle)', paddingTop: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Journey ID:</span>
                <strong>#{selectedJourney.id}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Station ID:</span>
                <strong>Station #{stationId}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Entry Timestamp:</span>
                <span>{selectedJourney.entryTime ? new Date(selectedJourney.entryTime).toLocaleString('en-GB') : 'N/A'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Current Journey Stage:</span>
                <strong>{selectedJourney.status}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Compliance Reason / Flag:</span>
                <span style={{ color: selectedJourney.complianceStatus === 'ELIGIBLE' ? 'var(--color-success)' : 'var(--color-danger)', fontWeight: 600 }}>
                  {selectedJourney.complianceReason || selectedJourney.reason || 'All statutory checks passed'}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
