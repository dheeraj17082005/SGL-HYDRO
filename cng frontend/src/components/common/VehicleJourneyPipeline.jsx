import React from 'react';

export function VehicleJourneyPipeline({ counts = {}, currentStage = '' }) {
  const steps = [
    { key: 'ENTRY', label: '1. ENTRY', count: counts.entered ?? counts.entry ?? 0 },
    { key: 'DETECTION', label: '2. ANPR DETECT', count: counts.detected ?? counts.detections ?? 0 },
    { key: 'VERIFICATION', label: '3. VERIFYING', count: counts.verifying ?? counts.verification ?? 0 },
    { key: 'COMPLIANCE', label: '4. COMPLIANCE', count: counts.cleared ?? counts.eligible ?? 0 },
    { key: 'QUEUE', label: '5. IN QUEUE', count: counts.inQueue ?? counts.queue ?? 0 },
    { key: 'BAY', label: '6. BAY ASSIGNED', count: counts.assigned ?? counts.bays ?? 0 },
    { key: 'FUELING', label: '7. FUELING', count: counts.fueling ?? 0 },
    { key: 'EXIT', label: '8. EXITED', count: counts.completed ?? counts.exited ?? 0 },
  ];

  return (
    <div className="journey-pipeline-container">
      <div className="journey-pipeline-header">
        <div className="journey-pipeline-title">
          <span>⚡</span> VEHICLE OPERATIONAL JOURNEY PIPELINE
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          {counts.blocked > 0 && (
            <span className="badge badge-danger">
              <span className="badge-dot" /> {counts.blocked} FUELING BLOCKED
            </span>
          )}
          <span className="badge badge-success">
            <span className="badge-dot" /> LIVE PIPELINE ACTIVE
          </span>
        </div>
      </div>

      <div className="journey-pipeline-steps">
        {steps.map((step, idx) => {
          const isActive = currentStage === step.key;
          const isBlocked = step.key === 'COMPLIANCE' && counts.blocked > 0;

          return (
            <div
              key={step.key}
              className={`journey-step-card ${isActive ? 'active' : ''} ${
                step.key === 'COMPLIANCE' ? 'stage-cleared' : ''
              }`}
            >
              <span className="step-number">STEP 0{idx + 1}</span>
              <span className="step-label">{step.label}</span>
              <span className="step-count">{step.count}</span>
              {idx < steps.length - 1 && <span className="journey-step-connector">➔</span>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
