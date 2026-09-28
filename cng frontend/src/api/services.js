export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
export const AI_API_BASE_URL = (import.meta.env.VITE_AI_API_BASE_URL || '/ai-api').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message, status) { super(message); this.name = 'ApiError'; this.status = status; }
}

async function request(path, options = {}) {
  const token = localStorage.getItem('sgl_access_token');
  const headers = { Accept: 'application/json', ...options.headers };
  if (options.body && !(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  let response;
  try { response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers }); }
  catch { throw new ApiError('Cannot reach the CNG service. Check that the backend is running at ' + API_BASE_URL + '.', 0); }
  let body;
  try { body = await response.json(); } catch { body = null; }
  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem('sgl_access_token');
      window.dispatchEvent(new Event('sgl:unauthorized'));
    }
    const message = body?.message || body?.error || body?.detail || (response.status === 401 ? 'Your session has expired. Please sign in again.' : `Request failed (${response.status}).`);
    throw new ApiError(message, response.status);
  }
  return body?.data ?? body;
}

export const authApi = {
  login: credentials => request('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
};
export const vehicleApi = {
  verify: (registrationNumber, stationId) => request('/api/v1/vehicles/verify', { method: 'POST', body: JSON.stringify({ registrationNumber }) }).catch(error => {
    // The current backend writes an audit row inside a read-only vehicle-verification transaction.
    // Fall back to its read-only compliance endpoint until that backend transaction is corrected.
    if (error.status !== 500) throw error;
    return request('/api/v1/compliance/verify', { method: 'POST', body: JSON.stringify({ registrationNumber, stationId }) })
      .then(result => ({ ...result, source: 'CNG COMPLIANCE' }));
  }),
};
export const stationApi = {
  list: () => request('/api/v1/stations'),
  bays: stationId => request(`/api/v1/stations/${stationId}/bays`),
};
export const dashboardApi = {
  get: stationId => request(`/api/v1/stations/${stationId}/dashboard`),
  metrics: stationId => request(`/api/v1/stations/${stationId}/metrics/compliance`),
  queueMetrics: stationId => request(`/api/v1/stations/${stationId}/metrics/queue`),
  throughput: stationId => request(`/api/v1/stations/${stationId}/metrics/throughput`),
  utilization: stationId => request(`/api/v1/stations/${stationId}/metrics/utilization`),
};
export const journeyApi = {
  list: stationId => request(`/api/v1/stations/${stationId}/journeys`),
  queue: stationId => request(`/api/v1/stations/${stationId}/queue`),
  get: id => request(`/api/v1/journeys/${id}`),
  enterQueue: id => request(`/api/v1/journeys/${id}/queue`, { method: 'POST' }),
  assignBay: (id, bayId) => request(`/api/v1/journeys/${id}/assign-bay`, { method: 'POST', body: JSON.stringify({ bayId }) }),
  startFueling: id => request(`/api/v1/journeys/${id}/fueling/start`, { method: 'POST' }),
  completeFueling: id => request(`/api/v1/journeys/${id}/fueling/complete`, { method: 'POST' }),
  exit: id => request(`/api/v1/journeys/${id}/exit`, { method: 'POST' }),
};
export const alertApi = {
  list: stationId => request(stationId ? `/api/v1/stations/${stationId}/alerts` : '/api/v1/alerts'),
  resolve: id => request(`/api/v1/alerts/${id}/resolve`, { method: 'POST' }),
};
export const auditApi = { list: (stationId, params = '') => request(`/api/v1/stations/${stationId}/audit-logs${params ? `?${params}` : ''}`) };
export const anprApi = {
  ingest: payload => request('/api/v1/anpr/detections', { method: 'POST', body: JSON.stringify(payload) }),
  recognize: async file => {
    const token = localStorage.getItem('sgl_access_token');
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    let response;
    const form = new FormData(); form.append('file', file);
    try { response = await fetch(`${AI_API_BASE_URL}/api/v1/anpr/recognize`, { method: 'POST', headers, body: form }); }
    catch { throw new ApiError('Cannot reach the ANPR service. Check that the AI service is running at ' + AI_API_BASE_URL + '.', 0); }
    const body = await response.json().catch(() => null);
    if (!response.ok) throw new ApiError(body?.detail || `ANPR request failed (${response.status}).`, response.status);
    return body;
  },
};

export function toVehicleView(v) {
  const hydro = String(v.hydroTestStatus || 'UNKNOWN').toUpperCase();
  const registration = v.registrationStatus || (v.registrationValid === true ? 'VALID' : v.registrationValid === false ? 'INVALID' : 'UNKNOWN');
  const eligible = v.eligible === true || String(v.complianceStatus || v.result || '').toUpperCase() === 'ELIGIBLE';
  const checks = {
    Registration: registration,
    Insurance: typeof v.insuranceValid === 'boolean' ? (v.insuranceValid ? 'VALID' : 'EXPIRED') : 'UNKNOWN',
    Fitness: typeof v.fitnessValid === 'boolean' ? (v.fitnessValid ? 'VALID' : 'EXPIRED') : 'UNKNOWN',
    PUC: typeof v.pucValid === 'boolean' ? (v.pucValid ? 'VALID' : 'EXPIRED') : 'UNKNOWN',
    'Hydro test': hydro,
  };
  return {
    plate: v.registrationNumber,
    make: v.makeModel || v.vehicleType || 'Vehicle record',
    fuel: v.fuelType || 'CNG compliance check',
    decision: eligible ? 'CLEARED' : 'BLOCKED',
    reason: v.reasons?.join('; ') || v.reason || (eligible ? '' : `Compliance status: ${v.complianceStatus || v.result || 'unknown'}`),
    source: v.source || 'CNG REGISTRY', checks, complianceStatus: v.complianceStatus || v.result,
    vehicleFound: v.vehicleFound ?? String(v.result).toUpperCase() !== 'VEHICLE_NOT_FOUND',
    owner: v.ownerNameMasked, rto: v.rtoOffice, hydroExpiry: v.hydroTestExpiry,
    certificate: v.hydroTestCertNumber, cylinder: v.cylinderSerialNo,
    verifiedAt: v.verifiedAt, raw: v,
  };
}

// Kept only as a visual fallback when the service is unavailable; live pages should use API results.
export const vehicles = [];
