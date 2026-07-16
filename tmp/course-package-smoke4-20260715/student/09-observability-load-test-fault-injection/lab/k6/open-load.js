import http from 'k6/http';
import { check } from 'k6';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const notifyPath = __ENV.NOTIFY_PATH || '/api/notifications';
const targetRate = Number(__ENV.RATE || 50);
const preAllocatedVUs = Number(__ENV.PREALLOCATED_VUS || 20);
const maxVUs = Number(__ENV.MAX_VUS || 200);
const testRunId = __ENV.TEST_RUN_ID || 'course-static';

if (!Number.isInteger(targetRate) || targetRate <= 0
    || !Number.isInteger(preAllocatedVUs) || preAllocatedVUs <= 0
    || !Number.isInteger(maxVUs) || maxVUs < preAllocatedVUs) {
  throw new Error('RATE must be a positive integer');
}

export const clientDuration = new Trend('notify_client_request_duration', true);
export const businessErrors = new Counter('notify_business_errors');
export const contractSuccess = new Rate('notify_contract_success');
export const backlogGauge = new Gauge('notify_backlog');

export const options = {
  scenarios: {
    open_notify: {
      executor: 'ramping-arrival-rate',
      startRate: targetRate,
      timeUnit: '1s',
      preAllocatedVUs,
      maxVUs,
      stages: [
        { target: targetRate, duration: '10s' },
        { target: targetRate * 2, duration: '20s' },
        { target: targetRate * 2, duration: '20s' },
        { target: targetRate, duration: '10s' },
      ],
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'notify_contract_success': ['rate>0.99'],
    'http_req_duration{endpoint:notify-create}': ['p(95)<500', 'p(99)<1000'],
    'notify_client_request_duration': ['p(95)<500'],
    'dropped_iterations': ['count<1'],
  },
};

export default function () {
  const payload = JSON.stringify({
    channel: 'mock',
    recipient: `${testRunId}-${__VU}-${__ITER}@example.invalid`,
    template: 'notifyflow-smoke',
  });
  const response = http.post(`${baseUrl}${notifyPath}`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'notify-create', load_model: 'open' },
  });

  const accepted = check(response, {
    'status is 2xx or 202': (res) => res.status >= 200 && res.status < 300,
    'response has bounded body': (res) => !res.body || res.body.length < 64 * 1024,
  });
  contractSuccess.add(accepted);
  if (!accepted) {
    businessErrors.add(1);
  }

  if (response.timings && Number.isFinite(response.timings.duration)) {
    clientDuration.add(response.timings.duration, { endpoint: 'notify-create' });
  }

  try {
    const body = response.json();
    if (body && Number.isFinite(Number(body.backlog))) {
      backlogGauge.add(Number(body.backlog), { channel: 'mock' });
    }
  } catch (_) {
    // A non-JSON error body is already captured by the check and threshold.
  }
}
