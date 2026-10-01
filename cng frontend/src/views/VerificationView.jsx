import React, { useState } from 'react';
import { vehicleApi, toVehicleView } from '../api/services.js';
import { Badge } from '../components/common/Badge.jsx';
import { Plate } from '../components/common/Plate.jsx';
import { SystemErrorState } from '../components/common/SystemNotice.jsx';

export function VerificationView({ stationId, setPage }) {
  const [query, setQuery] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(false);

  async function verify(e) {
    e?.preventDefault();
    const plate = query.toUpperCase().replace(/[\s-]/g, '');
    if (!plate) return;

    setError('');
    setNotFound(false);
    setLoading(true);
    setResult(null);

    try {
      const response = await vehicleApi.verify(plate, stationId);
      const vehicle = toVehicleView(response);
      setResult(vehicle);
      setNotFound(!response.vehicleFound);
    } catch (err) {
      setError(err.message || 'Vehicle verification failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="view-container">
      <div className="page-header">
        <div className="page-header-title">
          <div className="eyebrow">VEHICLE REGISTRY & COMPLIANCE</div>
          <h1>Vehicle Verification Lookup</h1>
          <p>Manual compliance check against SGL CNG Registry, RTO database, and Hydro-test certificates</p>
        </div>
      </div>

      <div className="verification-layout">
        {/* Left Column: Manual Plate Input Form */}
        <div className="panel" style={{ padding: '24px' }}>
          <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--color-brand)', marginBottom: '8px' }}>
            MANUAL PLATE LOOKUP
          </div>
          <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '20px', fontWeight: 800, marginBottom: '8px' }}>
            Verify Vehicle Registration
          </h2>
          <p style={{ fontSize: '13px', color: 'var(--text-subtle)', marginBottom: '20px' }}>
            Enter the vehicle registration number exactly as shown on the HSRP license plate.
          </p>

          <form onSubmit={verify} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', display: 'block', marginBottom: '6px' }}>
                REGISTRATION PLATE NUMBER
              </label>
              <input
                required
                maxLength={14}
                value={query}
                onChange={(e) => setQuery(e.target.value.toUpperCase().replace(/[\s-]/g, ''))}
                placeholder="e.g. GJ01AB1234"
                style={{
                  width: '100%',
                  height: '42px',
                  border: '1px solid var(--border-medium)',
                  borderRadius: 'var(--radius-md)',
                  padding: '0 14px',
                  fontSize: '15px',
                  fontFamily: 'var(--font-mono)',
                  letterSpacing: '1px',
                }}
              />
            </div>

            <button type="submit" className="btn-primary" disabled={loading} style={{ height: '42px' }}>
              {loading ? 'Querying SGL Registry...' : '🔍 Perform Verification Check'}
            </button>
          </form>
        </div>

        {/* Right Column: Verification Results Card */}
        <div className="verification-card-full">
          {loading ? (
            <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-subtle)' }}>
              <div className="badge-dot" style={{ width: '12px', height: '12px', margin: '0 auto 12px', background: 'var(--color-brand)' }} />
              <strong>Querying SGL Compliance Database...</strong>
            </div>
          ) : error ? (
            <SystemErrorState message={error} onRetry={verify} />
          ) : notFound ? (
            <div style={{ textAlign: 'center', padding: '40px', background: 'var(--color-danger-bg)', borderRadius: 'var(--radius-md)' }}>
              <h3 style={{ color: 'var(--color-danger)', marginBottom: '8px' }}>Vehicle Record Not Found</h3>
              <p style={{ fontSize: '13px', color: 'var(--text-subtle)' }}>
                No active registration record matches <Plate plate={query} /> in SGL database. Refueling is strictly prohibited.
              </p>
            </div>
          ) : result ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div className="eyebrow">VERIFICATION RESULT</div>
                  <div style={{ marginTop: '6px' }}>
                    <Plate plate={result.plate} size="large" />
                  </div>
                </div>
                <Badge tone={result.decision === 'CLEARED' ? 'pass' : 'fail'}>
                  {result.decision === 'CLEARED' ? 'ELIGIBILITY PASSED' : 'COMPLIANCE FAILED'}
                </Badge>
              </div>

              {/* Compliance Breakdown Table */}
              <div className="compliance-check-list">
                {Object.entries(result.checks || {}).map(([name, status]) => (
                  <div key={name} className="compliance-check-row">
                    <span>{name}</span>
                    <Badge tone={status === 'VALID' ? 'pass' : status === 'EXPIRED' ? 'fail' : 'neutral'}>
                      {status}
                    </Badge>
                  </div>
                ))}
              </div>

              {/* Hydro-Test Expiry Details */}
              <div style={{ padding: '12px', background: 'var(--bg-surface-subtle)', borderRadius: 'var(--radius-md)', fontSize: '12px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <div>
                  <strong>Hydro-Test Expiry:</strong>{' '}
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: result.decision === 'CLEARED' ? 'var(--color-success)' : 'var(--color-danger)' }}>
                    {result.hydroExpiry || 'N/A'}
                  </span>
                </div>
                <div>
                  <strong>Certificate #:</strong> <span>{result.certificate || 'N/A'}</span>
                </div>
                <div>
                  <strong>Cylinder Serial #:</strong> <span>{result.cylinder || 'N/A'}</span>
                </div>
              </div>

              {/* Final Decision Banner */}
              <div className={`decision-banner ${result.decision === 'CLEARED' ? 'cleared' : 'blocked'}`}>
                <h3>
                  {result.decision === 'CLEARED' ? '✓ CLEARED FOR FUELING' : '✕ FUELING BLOCKED'}
                </h3>
                {result.reason && <p style={{ fontSize: '12px' }}>{result.reason}</p>}
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                {result.decision === 'CLEARED' ? (
                  <button className="btn-secondary" style={{ flex: 1 }} onClick={() => setPage('Bays & Queue')}>
                    Assign Bay Now ➔
                  </button>
                ) : (
                  <button className="btn-danger" style={{ flex: 1 }} onClick={() => setPage('Alerts')}>
                    Record Compliance Alert ⚑
                  </button>
                )}
                <button className="btn-outline" onClick={() => window.print()}>
                  🖨 Print Certificate
                </button>
              </div>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-subtle)' }}>
              Enter a registration number on the left to verify vehicle compliance.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
