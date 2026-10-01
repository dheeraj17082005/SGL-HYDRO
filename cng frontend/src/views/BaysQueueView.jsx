import React, { useState, useEffect, useRef } from 'react';
import { journeyApi, dashboardApi, stationApi, anprApi } from '../api/services.js';
import { Badge } from '../components/common/Badge.jsx';
import { Plate } from '../components/common/Plate.jsx';
import { SystemErrorState, DemoDataNotice } from '../components/common/SystemNotice.jsx';

const demoVehicles = [
  ...Array.from({ length: 18 }, (_, i) => ({ plate: `GJ01DE${String(1001 + i).padStart(4, '0')}`, hydro: 'VALID' })),
  ...Array.from({ length: 10 }, (_, i) => ({ plate: `GJ01DE${String(1076 + i).padStart(4, '0')}`, hydro: 'EXPIRED' })),
];

export function BaysQueueView({ stationId }) {
  const [journeys, setJourneys] = useState([]);
  const [bayRecords, setBayRecords] = useState([]);
  const [bayCount, setBayCount] = useState(6);
  const [choices, setChoices] = useState({});
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState('');
  const [demoPlate, setDemoPlate] = useState(demoVehicles[0].plate);
  const [demoMessage, setDemoMessage] = useState('');
  const [demoBusy, setDemoBusy] = useState(false);

  const busyRef = useRef(false);
  const refreshRef = useRef(null);
  const refreshAgainRef = useRef(false);

  async function refresh(force = false) {
    if (refreshRef.current) {
      if (force) refreshAgainRef.current = true;
      return refreshRef.current;
    }

    const pending = (async () => {
      do {
        refreshAgainRef.current = false;
        try {
          const [items, util, bayList] = await Promise.all([
            journeyApi.list(stationId),
            dashboardApi.utilization(stationId),
            stationApi.bays(stationId),
          ]);
          setJourneys(Array.isArray(items) ? items : []);
          setBayRecords(Array.isArray(bayList) ? bayList : []);
          setBayCount(Math.max(6, Number(util?.totalBays) || bayList?.length || 6));
          setError('');
        } catch (e) {
          setError(e.message || 'Station bay service unavailable.');
        }
      } while (refreshAgainRef.current);
    })();

    refreshRef.current = pending;
    try {
      await pending;
    } finally {
      refreshRef.current = null;
    }
  }

  useEffect(() => {
    refresh();
    const timer = setInterval(() => {
      if (!busyRef.current) refresh();
    }, 4000);
    return () => clearInterval(timer);
  }, [stationId]);

  const active = journeys.filter((j) => j.status !== 'EXITED' && j.status !== 'BLOCKED');
  const queue = active
    .filter((j) => ['ENTERED', 'IN_QUEUE'].includes(j.status))
    .sort((a, b) => new Date(a.queueEntryTime || a.entryTime || 0) - new Date(b.queueEntryTime || b.entryTime || 0));

  const assigned = active.filter((j) => ['BAY_ASSIGNED', 'FUELING'].includes(j.status) && j.assignedBayNumber);
  const occupiedMap = new Map(assigned.map((j) => [Number(j.assignedBayNumber), j]));

  const bays = bayRecords.length
    ? bayRecords
    : Array.from({ length: bayCount }, (_, i) => ({ id: i + 1, bayNumber: i + 1, status: 'AVAILABLE' }));

  const availableBays = bays.filter((b) => String(b.status).toUpperCase() === 'AVAILABLE' && !occupiedMap.has(Number(b.bayNumber)));
  const blockedJourneys = journeys.filter((j) => j.status === 'BLOCKED' || (j.complianceStatus !== 'ELIGIBLE' && j.status !== 'EXITED'));

  async function act(journey, operation) {
    busyRef.current = true;
    setBusy(journey.id);
    setError('');
    try {
      if (operation === 'queue') await journeyApi.enterQueue(journey.id);
      if (operation === 'assign') {
        const selected = availableBays.find((b) => String(b.id) === String(choices[journey.id]));
        if (!selected) throw new Error('Selected bay is no longer available. Please select another bay.');
        await journeyApi.assignBay(journey.id, Number(selected.id));
        setChoices((old) => ({ ...old, [journey.id]: '' }));
      }
      if (operation === 'start') await journeyApi.startFueling(journey.id);
      if (operation === 'finish') await journeyApi.completeFueling(journey.id);
      await refresh(true);
    } catch (e) {
      setError(e.message || 'Operation failed.');
    } finally {
      busyRef.current = false;
      setBusy(null);
    }
  }

  async function simulateArrival() {
    setDemoBusy(true);
    setDemoMessage('');
    try {
      const clean = demoPlate;
      const known = await journeyApi.list(stationId);
      const duplicate = (Array.isArray(known) ? known : []).find((j) => j.registrationNumber === clean && j.status !== 'EXITED');
      if (duplicate) {
        setDemoMessage(`${clean} already has an active station journey (#${duplicate.id}).`);
        return;
      }
      const created = await anprApi.ingest({
        stationId: Number(stationId),
        cameraId: Number(import.meta.env.VITE_CAMERA_ID) || 1,
        registrationNumber: clean,
        detectedAt: new Date().toISOString(),
      });
      const createdId = created.journeyId ?? created.id;
      let latest = created;
      if (String(created.complianceStatus).toUpperCase() === 'ELIGIBLE' && createdId && created.journeyStatus === 'ENTERED') {
        latest = await journeyApi.enterQueue(createdId);
      }
      setDemoMessage(`${clean}: Compliance ${String(created.complianceStatus || 'UNKNOWN').replaceAll('_', ' ')} · Stage ${String(latest.journeyStatus || latest.status || 'RECORDED').replaceAll('_', ' ')}`);
      await refresh(true);
    } catch (e) {
      setDemoMessage(e.message || 'Simulation failed.');
    } finally {
      demoBusy(false);
    }
  }

  const getElapsedTime = (j) => {
    const mins = Math.max(0, Math.floor((Date.now() - new Date(j.queueEntryTime || j.entryTime || Date.now()).getTime()) / 60000));
    return `${mins} min`;
  };

  return (
    <div className="view-container">
      <div className="page-header">
        <div className="page-header-title">
          <div className="eyebrow">STATION FLOOR CONTROL</div>
          <h1>Fueling Bays & Station Queue</h1>
          <p>6-Bay visual control board, queue sequencing, and bay assignment interlocks</p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn-outline" onClick={() => refresh(true)}>
            ↻ Refresh Board
          </button>
        </div>
      </div>

      {error && <SystemErrorState message={error} onRetry={() => refresh(true)} />}

      {/* 6-Bay Operational Grid */}
      <div className="panel" style={{ padding: '20px' }}>
        <div className="panel-header" style={{ padding: '0 0 16px', borderBottom: '1px solid var(--border-subtle)' }}>
          <div>
            <div className="panel-title" style={{ fontSize: '16px' }}>6-BAY FUELING DISPENSER FLOOR</div>
            <div className="panel-subtitle">Real-time status of all station dispensers</div>
          </div>
          <Badge tone={availableBays.length > 0 ? 'pass' : 'warn'}>
            {availableBays.length} OF 6 BAYS AVAILABLE
          </Badge>
        </div>

        <div className="bays-grid" style={{ marginTop: '20px' }}>
          {bays.map((b) => {
            const bayNum = Number(b.bayNumber);
            const journey = occupiedMap.get(bayNum);
            const isFueling = journey?.status === 'FUELING';

            return (
              <div
                key={b.id}
                className={`bay-card ${
                  journey ? (isFueling ? 'bay-fueling' : 'bay-occupied') : 'bay-available'
                }`}
              >
                <div className="bay-card-top">
                  <span className="bay-number">BAY {String(bayNum).padStart(2, '0')}</span>
                  <Badge tone={isFueling ? 'warn' : journey ? 'neutral' : 'pass'}>
                    {isFueling ? 'FUELING ACTIVE' : journey ? 'RESERVED' : 'AVAILABLE'}
                  </Badge>
                </div>

                <div className="bay-vehicle-details">
                  {journey ? (
                    <>
                      <Plate plate={journey.registrationNumber || 'UNKNOWN'} size="large" />
                      <div className="bay-timer">
                        <span>⏱ Fueling elapsed: {getElapsedTime(journey)}</span>
                      </div>
                      <Badge tone="pass">✓ CLEARED FOR FUELING</Badge>
                    </>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <strong style={{ color: 'var(--color-success)', fontSize: '14px' }}>Dispenser Ready</strong>
                      <span style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                        {queue.length ? `${queue.length} vehicle(s) waiting in queue` : 'No vehicles in queue'}
                      </span>
                    </div>
                  )}
                </div>

                <div className="bay-actions">
                  <span style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                    {journey ? `Journey #${journey.id}` : 'Bay Status: Open'}
                  </span>

                  {journey ? (
                    isFueling ? (
                      <button className="btn-secondary" disabled={busy === journey.id} onClick={() => act(journey, 'finish')}>
                        {busy === journey.id ? 'Completing...' : 'Finish Fueling ➔'}
                      </button>
                    ) : (
                      <button className="btn-primary" disabled={busy === journey.id} onClick={() => act(journey, 'start')}>
                        {busy === journey.id ? 'Starting...' : 'Start Fueling ▶'}
                      </button>
                    )
                  ) : (
                    <span style={{ fontSize: '11px', color: 'var(--color-success)', fontWeight: 600 }}>
                      Ready for assignment
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Sequenced Queue Order */}
      <div className="panel">
        <div className="panel-header">
          <div>
            <div className="panel-title">Vehicle Queue Sequencing</div>
            <div className="panel-subtitle">Verified vehicles awaiting bay assignment in arrival order</div>
          </div>
          <Badge tone={queue.length > 0 ? 'warn' : 'neutral'}>{queue.length} IN QUEUE</Badge>
        </div>

        <div className="table-container">
          {queue.length > 0 ? (
            queue.map((j, i) => (
              <div key={j.id} className="queue-row">
                <div className="queue-position">#{String(i + 1).padStart(2, '0')}</div>
                <div>
                  <Plate plate={j.registrationNumber || 'UNKNOWN'} />
                </div>
                <div>
                  <Badge tone={j.complianceStatus === 'ELIGIBLE' ? 'pass' : 'fail'}>
                    {String(j.complianceStatus || 'UNKNOWN').replaceAll('_', ' ')}
                  </Badge>
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-subtle)', fontFamily: 'var(--font-mono)' }}>
                  Wait: {getElapsedTime(j)}
                </div>
                <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                  {j.status === 'ENTERED' ? (
                    <button
                      className="btn-outline"
                      disabled={busy === j.id || j.complianceStatus !== 'ELIGIBLE'}
                      onClick={() => act(j, 'queue')}
                    >
                      {busy === j.id ? 'Saving...' : 'Add to Station Queue'}
                    </button>
                  ) : (
                    <>
                      <select
                        aria-label={`Select bay for ${j.registrationNumber}`}
                        value={choices[j.id] || ''}
                        onChange={(e) => setChoices({ ...choices, [j.id]: e.target.value })}
                        className="station-select"
                        style={{ border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-md)', padding: '0 10px' }}
                      >
                        <option value="">Select Available Bay</option>
                        {availableBays.map((b) => (
                          <option key={b.id} value={b.id}>
                            Bay {String(b.bayNumber).padStart(2, '0')}
                          </option>
                        ))}
                      </select>

                      <button
                        className="btn-primary"
                        disabled={busy === j.id || !availableBays.length || !choices[j.id]}
                        onClick={() => act(j, 'assign')}
                      >
                        {busy === j.id ? 'Assigning...' : 'Assign Bay ➔'}
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))
          ) : (
            <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-subtle)' }}>
              Queue is clear. Eligible vehicles detected at entry gate will be queued automatically.
            </div>
          )}
        </div>
      </div>

      {/* Demo Simulation Controls (Labeled Explicitly) */}
      <div className="panel" style={{ padding: '20px', background: 'var(--bg-surface-subtle)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
          <DemoDataNotice message="ANPR Vehicle Entry Simulation Tool" />
        </div>

        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <label style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)' }}>SELECT SAMPLE VEHICLE:</label>
          <select
            value={demoPlate}
            onChange={(e) => setDemoPlate(e.target.value)}
            className="station-select"
            style={{ background: '#fff', border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-md)', padding: '6px 12px' }}
          >
            {demoVehicles.map((v) => (
              <option key={v.plate} value={v.plate}>
                {v.plate} (Hydro-test: {v.hydro})
              </option>
            ))}
          </select>

          <button className="btn-outline" disabled={demoBusy} onClick={simulateArrival}>
            {demoBusy ? 'Simulating...' : '▶ Simulate Entry Detection'}
          </button>
        </div>

        {demoMessage && (
          <div style={{ marginTop: '10px', fontSize: '12px', color: 'var(--color-brand)', fontWeight: 600 }}>
            {demoMessage}
          </div>
        )}
      </div>
    </div>
  );
}
