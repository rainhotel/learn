import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const notifyPath = __ENV.NOTIFY_PATH || '/api/notifications';
const vus = Number(__ENV.VUS || 50);
const duration = __ENV.DURATION || '60s';
const testRunId = __ENV.TEST_RUN_ID || 'course-static';

if (!Number.isInteger(vus) || vus <= 0) {
  throw new Error('VUS must be a positive integer');
}

export const businessErrors = new Counter('notify_business_errors');
export const requestDuration = new Trend('notify_closed_client_duration', true);
export const contractSuccess = new Rate('notify_closed_contract_success');

export const options = {
  scenarios: {
    closed_notify: {
      executor: 'constant-vus',
      vus,
      duration,
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'http_req_duration{endpoint:notify-create}': ['p(95)<500', 'p(99)<1000'],
    'notify_closed_client_duration': ['p(95)<500'],
    'notify_closed_contract_success': ['rate>0.99'],
  },
};

export default function () {
  const response = http.post(
    `${baseUrl}${notifyPath}`,
    JSON.stringify({
      channel: 'mock',
      recipient: `${testRunId}-closed-${__VU}-${__ITER}@example.invalid`,
      template: 'notifyflow-smoke',
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'notify-create', load_model: 'closed' },
    },
  );

  const ok = check(response, {
    'status is 2xx or 202': (res) => res.status >= 200 && res.status < 300,
  });
  if (response.timings && Number.isFinite(response.timings.duration)) {
    requestDuration.add(response.timings.duration, { endpoint: 'notify-create' });
  }
  contractSuccess.add(ok);
  if (!ok) {
    businessErrors.add(1);
  }
}
