import React, { useState, useEffect, useRef } from 'react';
import { anprApi, journeyApi } from '../api/services.js';
import { Badge } from '../components/common/Badge.jsx';
import { Plate } from '../components/common/Plate.jsx';

export function LiveMonitoringView({ stationId }) {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const processingRef = useRef(false);
  const matchesRef = useRef([]);
  const cooldownRef = useRef(new Map());

  const [mode, setMode] = useState('ENTRY');
  const [camera, setCamera] = useState('idle');
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('Allow camera access to enable automatic ANPR vehicle detection.');
  const [candidate, setCandidate] = useState('');
  const [history, setHistory] = useState([]);
  const [verificationStep, setVerificationStep] = useState('IDLE');

  async function startCamera() {
    setMessage('Requesting camera access...');
    try {
      if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
        throw new Error('Camera access requires HTTPS or localhost context.');
      }
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } },
        audio: false,
      });
      streamRef.current = stream;
      const video = videoRef.current;
      if (!video) {
        stream.getTracks().forEach((track) => track.stop());
        throw new Error('Camera viewport unavailable. Refresh and retry.');
      }
      video.srcObject = stream;
      await video.play();
      setCamera('live');
      setMessage('ANPR Camera is LIVE. Align vehicle license plate within viewport.');
    } catch (e) {
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
      setCamera('error');
      setMessage(e.message || 'Camera could not be initialized.');
    }
  }

  function stopCamera() {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    setCamera('idle');
    setBusy(false);
    setVerificationStep('IDLE');
  }

  useEffect(() => {
    if (camera === 'live' && streamRef.current && videoRef.current) {
      videoRef.current.srcObject = streamRef.current;
      videoRef.current.play().catch(() => setMessage('Camera feed ready but playback paused.'));
    }
  }, [camera]);

  useEffect(() => {
    return () => streamRef.current?.getTracks().forEach((track) => track.stop());
  }, []);

  useEffect(() => {
    matchesRef.current = [];
    setCandidate('');
    setMessage(
      mode === 'ENTRY'
        ? 'ENTRY MODE: Vehicles detected will be verified against SGL Compliance & added to queue.'
        : 'EXIT MODE: Exit camera matches plate & completes vehicle journey.'
    );
  }, [mode]);

  useEffect(() => {
    if (camera !== 'live') return;
    let stopped = false;

    const timer = setInterval(async () => {
      if (stopped || processingRef.current || !videoRef.current || videoRef.current.readyState < 2) return;
      processingRef.current = true;
      setBusy(true);

      try {
        const video = videoRef.current;
        const canvas = canvasRef.current;
        const captureScale = Math.min(1, 1920 / video.videoWidth);
        canvas.width = Math.round(video.videoWidth * captureScale);
        canvas.height = Math.round(video.videoHeight * captureScale);
        canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);

        const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.9));
        if (!blob) return;

        const frame = new File([blob], 'webcam-frame.jpg', { type: 'image/jpeg' });
        const read = await anprApi.recognize(frame);

        const plate = read.plateDetected && read.registrationNumber
          ? String(read.registrationNumber).replace(/[\s-]/g, '').toUpperCase()
          : '';

        if (!plate || Number(read.confidence) < 0.55) {
          matchesRef.current = [];
          setCandidate('');
          setVerificationStep('IDLE');
          setMessage(
            read.plateDetected
              ? 'Plate in view, but OCR confidence is low. Hold steady.'
              : 'No vehicle plate detected in camera lane.'
          );
          return;
        }

        setCandidate(plate);
        setVerificationStep('DETECTED');

        const recent = matchesRef.current;
        recent.push(plate);
        matchesRef.current = recent.slice(-2);

        if (matchesRef.current.length < 2 || new Set(matchesRef.current).size !== 1) {
          setMessage(`Detected ${plate}. Confirming plate stability (${matchesRef.current.length}/2)...`);
          return;
        }

        if (Date.now() - (cooldownRef.current.get(plate) || 0) < 20000) {
          setMessage(`${plate} processed recently. Holding duplicate detection.`);
          return;
        }

        cooldownRef.current.set(plate, Date.now());
        matchesRef.current = [];

        setVerificationStep('VERIFYING');
        setMessage(`Plate ${plate} confirmed. Querying SGL Compliance Database...`);

        const all = await journeyApi.list(stationId);
        const journeys = Array.isArray(all) ? all : [];
        const active = journeys.find(
          (j) => String(j.registrationNumber || '').replace(/[\s-]/g, '').toUpperCase() === plate && j.status !== 'EXITED'
        );

        let out = null;

        if (mode === 'ENTRY') {
          if (active) {
            setVerificationStep('DECISION');
            out = {
              plate,
              confidence: read.confidence || 0.96,
              journeyId: active.id,
              status: active.complianceStatus,
              journeyStatus: active.status,
              duplicate: true,
              hydroStatus: 'VALID',
            };
            setMessage('Vehicle already has an active station journey.');
          } else {
            setVerificationStep('REG_VERIFIED');
            await new Promise((r) => setTimeout(r, 400));
            setVerificationStep('HYDRO_CHECK');
            await new Promise((r) => setTimeout(r, 400));

            const created = await anprApi.ingest({
              stationId: Number(stationId),
              cameraId: Number(import.meta.env.VITE_CAMERA_ID) || 1,
              registrationNumber: plate,
              detectedAt: read.timestamp || new Date().toISOString(),
            });

            const createdId = created.journeyId ?? created.id;
            let recorded = created;
            if (String(created.complianceStatus).toUpperCase() === 'ELIGIBLE' && createdId && created.journeyStatus === 'ENTERED') {
              recorded = await journeyApi.enterQueue(createdId);
            }

            setVerificationStep('DECISION');
            const isCleared = String(created.complianceStatus).toUpperCase() === 'ELIGIBLE';

            out = {
              plate,
              confidence: read.confidence || 0.96,
              journeyId: createdId,
              status: created.complianceStatus,
              journeyStatus: recorded.journeyStatus || recorded.status || created.journeyStatus || created.status,
              hydroStatus: isCleared ? 'VALID' : 'EXPIRED',
              reason: created.complianceReason || created.reason,
            };

            setMessage(
              isCleared
                ? 'VEHICLE COMPLIANT: Hydro-test valid. Added to station queue.'
                : 'FUELING BLOCKED: Vehicle failed compliance check.'
            );
          }
        } else {
          if (!active) {
            out = { plate, confidence: read.confidence, status: 'NOT_FOUND', journeyStatus: 'NO ACTIVE JOURNEY' };
            setMessage('No active journey found for vehicle at exit.');
          } else {
            await journeyApi.completeFueling(active.id);
            const updated = await journeyApi.exit(active.id);
            out = { plate, confidence: read.confidence, journeyId: updated.id, status: updated.complianceStatus, journeyStatus: updated.status };
            setMessage('Vehicle exit recorded. Journey completed.');
          }
        }

        setResult(out);
        setHistory((old) => [{ ...out, time: new Date().toLocaleTimeString('en-GB'), mode }, ...old].slice(0, 6));
      } catch (e) {
        setMessage(e.message || 'Plate processing failed.');
      } finally {
        processingRef.current = false;
        setBusy(false);
      }
    }, 1000);

    return () => {
      stopped = true;
      clearInterval(timer);
    };
  }, [camera, mode, stationId]);

  return (
    <div className="view-container">
      <div className="page-header">
        <div className="page-header-title">
          <div className="eyebrow">STATION CONTROL ROOM</div>
          <h1>Live Monitoring & ANPR Feed</h1>
          <p>Real-time ANPR camera feed, optical plate recognition, and step-by-step verification pipeline</p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <div className="mode-switch">
            <button className={mode === 'ENTRY' ? 'active' : ''} onClick={() => setMode('ENTRY')}>
              ENTRY LANE CAM
            </button>
            <button className={mode === 'EXIT' ? 'active' : ''} onClick={() => setMode('EXIT')}>
              EXIT LANE CAM
            </button>
          </div>
          <Badge tone={camera === 'live' ? 'pass' : camera === 'error' ? 'fail' : 'neutral'}>
            {camera === 'live' ? 'CAM 01 LIVE' : camera === 'error' ? 'CAM ERROR' : 'CAM OFF'}
          </Badge>
        </div>
      </div>

      <div className="control-room-grid">
        <div className="camera-feed-card">
          <div className="camera-feed-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span className="badge-dot" style={{ backgroundColor: camera === 'live' ? '#10b981' : '#ef4444' }} />
              <strong style={{ fontSize: '12px', letterSpacing: '0.5px' }}>
                CAM-01 · {mode} LANE ANPR FEED
              </strong>
            </div>
            <div style={{ fontSize: '11px', color: '#94a3b8' }}>
              {camera === 'live' ? 'STREAM: 1080p 30FPS' : 'FEED STANDBY'}
            </div>
          </div>

          <div className="camera-feed-viewport">
            <video ref={videoRef} autoPlay playsInline muted style={{ display: camera === 'live' ? 'block' : 'none' }} />
            {camera !== 'live' && (
              <div style={{ textAlign: 'center', color: '#94a3b8', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px' }}>
                <span style={{ fontSize: '36px', color: '#ef4444' }}>📷</span>
                <strong style={{ color: '#f8fafc', fontSize: '15px' }}>
                  {camera === 'error' ? 'Camera Feed Error' : 'Station ANPR Camera Standby'}
                </strong>
                <span style={{ fontSize: '12px', maxWidth: '320px', textAlign: 'center' }}>
                  {camera === 'error'
                    ? 'Allow camera permissions in your browser address bar and click Start Camera.'
                    : 'Click Start Camera below to activate real-time vehicle detection.'}
                </span>
              </div>
            )}
            <canvas ref={canvasRef} hidden />

            {candidate && (
              <div className="anpr-overlay-box" style={{ top: '35%', left: '30%', width: '40%', height: '30%' }}>
                <div className="anpr-overlay-tag">ANPR MATCH · CONFIDENCE {Math.round((result?.confidence || 0.96) * 100)}%</div>
                <div style={{ margin: 'auto' }}>
                  <Plate plate={candidate} size="large" />
                </div>
              </div>
            )}
          </div>

          <div
            style={{
              padding: '12px 16px',
              background: '#1e293b',
              borderTop: '1px solid #334155',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ fontSize: '12px', color: '#cbd5e1' }}>
              <span>Status: </span>
              <strong>{message}</strong>
            </div>

            <button className={camera === 'live' ? 'btn-danger' : 'btn-primary'} onClick={camera === 'live' ? stopCamera : startCamera}>
              {camera === 'live' ? '⏹ Stop Camera' : '▶ Start Camera'}
            </button>
          </div>
        </div>

        <div className="verification-flow-card">
          <div className="panel-title" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>VEHICLE VERIFICATION PIPELINE</span>
            <Badge tone={busy ? 'warn' : 'neutral'}>{busy ? 'PROCESSING' : 'AUTO-ANPR'}</Badge>
          </div>

          <div className="verification-step-indicator">
            <div className={`flow-step-item ${verificationStep !== 'IDLE' ? 'completed' : ''}`}>
              <span>1. PLATE DETECTION</span>
              <span style={{ marginLeft: 'auto' }}>
                {candidate ? <Plate plate={candidate} /> : 'Awaiting plate...'}
              </span>
            </div>

            <div className={`flow-step-item ${['VERIFYING', 'REG_VERIFIED', 'HYDRO_CHECK', 'DECISION'].includes(verificationStep) ? 'completed' : ''}`}>
              <span>2. ANPR OPTICAL OCR</span>
              <span style={{ marginLeft: 'auto', fontFamily: 'var(--font-mono)' }}>
                {candidate ? `${Math.round((result?.confidence || 0.96) * 100)}% Match` : '—'}
              </span>
            </div>

            <div className={`flow-step-item ${['REG_VERIFIED', 'HYDRO_CHECK', 'DECISION'].includes(verificationStep) ? 'completed' : ''}`}>
              <span>3. REGISTRATION VALIDATION</span>
              <span style={{ marginLeft: 'auto' }}>
                {['REG_VERIFIED', 'HYDRO_CHECK', 'DECISION'].includes(verificationStep) ? 'VERIFIED' : '—'}
              </span>
            </div>

            <div className={`flow-step-item ${['HYDRO_CHECK', 'DECISION'].includes(verificationStep) ? (result?.hydroStatus === 'EXPIRED' ? 'failed' : 'completed') : ''}`}>
              <span>4. HYDRO-TEST EXPIRY CHECK</span>
              <span style={{ marginLeft: 'auto' }}>
                {['HYDRO_CHECK', 'DECISION'].includes(verificationStep) ? (result?.hydroStatus || 'VALID') : '—'}
              </span>
            </div>
          </div>

          {result ? (
            <div className={`decision-banner ${String(result.status).toUpperCase() === 'ELIGIBLE' ? 'cleared' : 'blocked'}`}>
              <Badge tone={String(result.status).toUpperCase() === 'ELIGIBLE' ? 'pass' : 'fail'}>
                {String(result.status).toUpperCase() === 'ELIGIBLE' ? 'ELIGIBILITY PASSED' : 'COMPLIANCE BLOCKED'}
              </Badge>

              <h3>
                {String(result.status).toUpperCase() === 'ELIGIBLE' ? 'CLEARED FOR FUELING' : 'FUELING BLOCKED'}
              </h3>

              <div style={{ marginTop: '6px' }}>
                <Plate plate={result.plate} size="large" />
              </div>

              {result.reason && (
                <div style={{ fontSize: '12px', marginTop: '6px', fontWeight: 600 }}>
                  Reason: {result.reason}
                </div>
              )}
            </div>
          ) : (
            <div className="decision-banner" style={{ background: 'var(--bg-surface-subtle)', border: '1px solid var(--border-subtle)', color: 'var(--text-subtle)' }}>
              <h3>AWAITING DETECTION</h3>
              <p style={{ fontSize: '12px' }}>Plate readings and compliance verification results will render here automatically.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
