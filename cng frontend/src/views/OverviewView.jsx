import React, { useState, useEffect } from 'react';
import { dashboardApi, journeyApi } from '../api/services.js';
import { Badge } from '../components/common/Badge.jsx';
import { Plate } from '../components/common/Plate.jsx';
import { VehicleJourneyPipeline } from '../components/common/VehicleJourneyPipeline.jsx';
import { SystemErrorState, DemoDataNotice } from '../components/common/SystemNotice.jsx';

export function OverviewView({ setPage, stationId }) {
  const [data, setData] = useState(null);
  const [queueStats, setQueueStats] = useState(null);
  const [throughput, setThroughput] = useState(null);
  const [utilization, setUtilization] = useState(null);
  const [journeys, setJourneys] = useState([]);
  const [loadError, setLoadError] = useState('');
  const [lastSync, setLastSync] = useState('');

  const loadDashboard = () => {
    let live = true;
    setLoadError('');
    Promise.all([
      dashboardApi.get(stationId),
      journeyApi.list(stationId),
      dashboardApi.queueMetrics(stationId),
      dashboardApi.throughput(stationId),
      dashboardApi.utilization(stationId),
    ])
      .then(([metrics, items, queue, flow, use]) => {
        if (live) {
          setData(metrics);
          setJourneys(Array.isArray(items) ? items : []);
          setQueueStats(queue);
          setThroughput(flow);
          setUtilization(use);
          setLastSync(new Date().toLocaleTimeString('en-GB'));
        }
      })
      .catch((e) => {
        if (live) setLoadError(e.message || 'Live station operational data could not be retrieved.');
      });
    return () => {
      live = false;
    };
  };

  useEffect(() => {
    loadDashboard();
    const timer = setInterval(loadDashboard, 5000);
    return () => clearInterval(timer);
  }, [stationId]);

  const activeJourneys = journeys.filter((j) => j.status !== 'EXITED');
  const blockedJourneys = journeys.filter(
    (j) => j.status === 'BLOCKED' || (j.complianceStatus && j.complianceStatus !== 'ELIGIBLE' && j.status !== 'EXITED')
  );

  const pipelineCounts = {
    entered: activeJourneys.filter((j) => j.status === 'ENTERED').length,
    detected: activeJourneys.filter((j) => ['ENTERED', 'VERIFYING'].includes(j.status)).length,
    verifying: activeJourneys.filter((j) => j.status === 'VERIFYING').length,
    cleared: activeJourneys.filter((j) => j.complianceStatus === 'ELIGIBLE').length,
    inQueue: activeJourneys.filter((j) => j.status === 'IN_QUEUE').length,
    assigned: activeJourneys.filter((j) => j.status === 'BAY_ASSIGNED').length,
    fueling: activeJourneys.filter((j) => j.status === 'FUELING').length,
    completed: throughput?.completedJourneysToday ?? data?.completedJourneysToday ?? 0,
    blocked: blockedJourneys.length || (data?.blockedVehiclesToday ?? 0),
  };

  const stationName = data?.stationName || `Station #${stationId} Operations`;
  const isDemo = !data && journeys.length === 0;

  return (
    <div className="view-container">
      {/* Top Station Operational Header */}
      <div className="page-header">
        <div className="page-header-title">
          <div className="eyebrow">COMMAND & CONTROL DASHBOARD</div>
          <h1>{stationName}</h1>
          <p>Real-time station operations, ANPR vehicle tracking, and compliance enforcement</p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          {isDemo && <DemoDataNotice />}
          <Badge tone={data ? 'pass' : loadError ? 'fail' : 'warn'}>
            {data ? 'STATION ONLINE' : loadError ? 'OFFLINE / DISCONNECTED' : 'CONNECTING...'}
          </Badge>
          <button className="btn-outline" onClick={loadDashboard}>
            ↻ Refresh State
          </button>
        </div>
      </div>

      {loadError && (
        <SystemErrorState message={loadError} onRetry={loadDashboard} lastSync={lastSync} />
      )}

      {/* Primary UX Concept: VEHICLE JOURNEY PIPELINE */}
      <VehicleJourneyPipeline counts={pipelineCounts} currentStage="OVERVIEW" />

      {/* Operational Metric Cards */}
      <div className="metrics-grid">
        <div className="metric-card accent-blue">
          <div className="metric-header">
            <span>COMPLETED TODAY</span>
            <span>↗</span>
          </div>
          <div className="metric-value">
            {throughput?.completedJourneysToday ?? data?.completedJourneysToday ?? '—'}
          </div>
          <div className="metric-note">
            {throughput?.completedJourneysLastHour ?? '0'} completed in last hour
          </div>
        </div>

        <div className="metric-card accent-amber">
          <div className="metric-header">
            <span>VEHICLES IN QUEUE</span>
            <span>⇄</span>
          </div>
          <div className="metric-value">
            {queueStats?.currentQueueLength ?? data?.vehiclesInQueue ?? '—'}
          </div>
          <div className="metric-note warn">
            Average wait: {queueStats ? Math.round((queueStats.averageWaitingTimeSeconds || 0) / 60) : '0'} mins
          </div>
        </div>

        <div className="metric-card accent-green">
          <div className="metric-header">
            <span>FUELING IN PROGRESS</span>
            <span>⚡</span>
          </div>
          <div className="metric-value">{data?.vehiclesFueling ?? '—'}</div>
          <div className="metric-note good">
            {data?.availableBays ?? '—'} bays available for assignment
          </div>
        </div>

        <div className="metric-card accent-red">
          <div className="metric-header">
            <span>BLOCKED VEHICLES</span>
            <span>⚑</span>
          </div>
          <div className="metric-value">{data?.blockedVehiclesToday ?? blockedJourneys.length}</div>
          <div className="metric-note bad">Compliance interlock active</div>
        </div>
      </div>

      {/* Main Operations Control Grid */}
      <div className="control-room-grid">
        {/* Left Column: Active Vehicle Journeys */}
        <div className="panel">
          <div className="panel-header">
            <div>
              <div className="panel-title">Active Station Vehicles</div>
              <div className="panel-subtitle">Vehicles currently inside entry lane, queue, or fueling bays</div>
            </div>
            <button className="btn-outline" style={{ height: '30px', fontSize: '11px' }} onClick={() => setPage('Bays & Queue')}>
              Manage Bays ➔
            </button>
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>REGISTRATION</th>
                  <th>STAGE</th>
                  <th>COMPLIANCE</th>
                  <th>BAY</th>
                  <th>ARRIVED</th>
                </tr>
              </thead>
              <tbody>
                {activeJourneys.length > 0 ? (
                  activeJourneys.slice(0, 8).map((j) => {
                    const isEligible = String(j.complianceStatus).toUpperCase() === 'ELIGIBLE';
                    return (
                      <tr key={j.id}>
                        <td>
                          <Plate plate={j.registrationNumber || 'UNKNOWN'} />
                        </td>
                        <td>
                          <strong style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                            {String(j.status || 'ENTERED').replaceAll('_', ' ')}
                          </strong>
                        </td>
                        <td>
                          <Badge tone={isEligible ? 'pass' : 'fail'}>
                            {String(j.complianceStatus || 'UNKNOWN').replaceAll('_', ' ')}
                          </Badge>
                        </td>
                        <td>
                          {j.assignedBayNumber ? (
                            <strong style={{ color: 'var(--color-brand)' }}>BAY {String(j.assignedBayNumber).padStart(2, '0')}</strong>
                          ) : (
                            <span style={{ color: 'var(--text-subtle)' }}>Unassigned</span>
                          )}
                        </td>
                        <td style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                          {j.entryTime ? new Date(j.entryTime).toLocaleTimeString('en-GB') : '—'}
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={5} style={{ textAlign: 'center', padding: '30px', color: 'var(--text-subtle)' }}>
                      No active vehicles in the station. Detections from entry ANPR camera will appear here.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right Column: Station Overview Controls */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Station Capacity Panel */}
          <div className="panel">
            <div className="panel-header">
              <div>
                <div className="panel-title">Fueling Bay Utilization</div>
                <div className="panel-subtitle">
                  {data ? `${data.occupiedBays} of ${data.occupiedBays + data.availableBays} bays occupied` : 'Checking bays...'}
                </div>
              </div>
              <Badge tone={utilization?.utilizationPercentage > 80 ? 'warn' : 'pass'}>
                {utilization?.utilizationPercentage ?? '0'}% UTILIZED
              </Badge>
            </div>

            <div className="panel-body" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-subtle)', fontWeight: 500 }}>Active Vehicles in Queue</span>
                <strong>{data?.vehiclesInQueue ?? '0'}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-subtle)', fontWeight: 500 }}>Bays Currently Fueling</span>
                <strong>{data?.vehiclesFueling ?? '0'}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-subtle)', fontWeight: 500 }}>Available Fueling Bays</span>
                <strong style={{ color: 'var(--color-success)' }}>{data?.availableBays ?? '6'} Bays</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0' }}>
                <span style={{ color: 'var(--text-subtle)', fontWeight: 500 }}>Avg Fueling Duration</span>
                <strong>{data ? Math.round((data.averageFuelingDurationSeconds || 0) / 60) : '0'} mins</strong>
              </div>
            </div>
          </div>

          {/* Blocked Vehicles Spotlight */}
          <div className="panel" style={{ borderLeft: '4px solid var(--color-danger)' }}>
            <div className="panel-header">
              <div>
                <div className="panel-title" style={{ color: 'var(--color-danger)' }}>
                  ⚑ Compliance Interlock Summary
                </div>
                <div className="panel-subtitle">Vehicles blocked from refueling due to compliance failure</div>
              </div>
              <button className="btn-danger" style={{ height: '28px', padding: '0 10px', fontSize: '11px' }} onClick={() => setPage('Compliance')}>
                Inspect All
              </button>
            </div>

            <div className="panel-body" style={{ padding: '12px 20px' }}>
              {blockedJourneys.length > 0 ? (
                blockedJourneys.slice(0, 3).map((b) => (
                  <div
                    key={b.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 0',
                      borderBottom: '1px solid var(--border-subtle)',
                    }}
                  >
                    <div>
                      <Plate plate={b.registrationNumber || 'UNKNOWN'} />
                      <div style={{ fontSize: '11px', color: 'var(--color-danger)', marginTop: '4px', fontWeight: 600 }}>
                        {b.complianceReason || b.reason || 'Hydro-test certificate invalid / expired'}
                      </div>
                    </div>
                    <Badge tone="fail">FUELING BLOCKED</Badge>
                  </div>
                ))
              ) : (
                <div style={{ fontSize: '12px', color: 'var(--text-subtle)', padding: '10px 0' }}>
                  ✓ No non-compliant vehicles held at station currently.
                </div>
              )}
            </div>
          </div>

          {/* Actionable Station Alerts Banner */}
          <div
            style={{
              background: 'var(--color-warning-bg)',
              border: '1px solid var(--color-warning-border)',
              borderRadius: 'var(--radius-lg)',
              padding: '16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div>
              <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--color-warning-text)' }}>
                Station Operational Alerts
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '2px' }}>
                Review active compliance flags and system events
              </div>
            </div>
            <button className="btn-outline" style={{ height: '32px', fontSize: '11px' }} onClick={() => setPage('Alerts')}>
              Open Alert Register ➔
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
