import http from 'k6/http';
import { check } from 'k6';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const recoveryPath = __ENV.RECOVERY_PATH || '/internal/lab/recovery/drain-one';
const baseRate = Number(__ENV.RECOVERY_BASE_RATE || 100);
const preAllocatedVUs = Number(__ENV.PREALLOCATED_VUS || 20);
const maxVUs = Number(__ENV.MAX_VUS || 200);
const testRunId = __ENV.TEST_RUN_ID || 'course-static';

if (!Number.isInteger(baseRate) || baseRate <= 0
    || !Number.isInteger(preAllocatedVUs) || preAllocatedVUs <= 0
    || !Number.isInteger(maxVUs) || maxVUs < preAllocatedVUs) {
  throw new Error('RECOVERY_BASE_RATE must be a positive integer');
}

export const recoveryErrors = new Counter('notify_recovery_errors');
export const recoveredItems = new Counter('notify_recovery_processed');
export const backlogGauge = new Gauge('notify_recovery_backlog');
export const backlogSamples = new Trend('notify_recovery_backlog_samples', true);
export const backlogSamplePresent = new Rate('notify_recovery_backlog_sample_present');
export const recoveryDuration = new Trend('notify_recovery_probe_duration', true);
export const contractSuccess = new Rate('notify_recovery_contract_success');

function stage(rateRatio, startTime, duration, phase) {
  return {
    executor: 'constant-arrival-rate',
    rate: Math.max(1, Math.floor(baseRate * rateRatio)),
    timeUnit: '1s',
    duration,
    startTime,
    preAllocatedVUs,
    maxVUs,
    gracefulStop: '5s',
    exec: 'recoveryStep',
    tags: { endpoint: 'recovery-drain-one', recovery_phase: phase },
  };
}

function stopThreshold(expression) {
  return [{ threshold: expression, abortOnFail: true, delayAbortEval: '10s' }];
}

export const options = {
  scenarios: {
    recovery_01pct: stage(0.01, '0s', '30s', '01pct'),
    recovery_05pct: stage(0.05, '35s', '30s', '05pct'),
    recovery_20pct: stage(0.20, '70s', '30s', '20pct'),
    recovery_50pct: stage(0.50, '105s', '30s', '50pct'),
    recovery_100pct: stage(1.00, '140s', '60s', '100pct'),
  },
  thresholds: {
    'notify_recovery_contract_success': stopThreshold('rate>0.99'),
    'http_req_duration{endpoint:recovery-drain-one}': stopThreshold('p(99)<2000'),
    'notify_recovery_backlog_samples': stopThreshold('max<100000'),
    'notify_recovery_backlog_sample_present': stopThreshold('rate>0.99'),
    'dropped_iterations': stopThreshold('count<1'),
  },
};

export function recoveryStep() {
  const response = http.post(
    `${baseUrl}${recoveryPath}`,
    JSON.stringify({ channel: 'mock', testRunId, maxItems: 1 }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-NotifyFlow-Lab-Mode': 'true',
      },
      tags: { endpoint: 'recovery-drain-one', load_model: 'staged-recovery' },
    },
  );
  let body = null;
  try {
    body = response.json();
  } catch (_) {
    body = null;
  }
  const ok = check(response, {
    'recovery drain is successful': (res) => res.status >= 200 && res.status < 300,
    'recovery response is bounded': (res) => res.body && res.body.length < 64 * 1024,
    'at most one item is processed': () => body && Number.isFinite(Number(body.processed)) && Number(body.processed) >= 0 && Number(body.processed) <= 1,
    'backlog is non-negative': () => body && Number.isFinite(Number(body.backlog)) && Number(body.backlog) >= 0,
  });
  contractSuccess.add(ok);
  if (response.timings && Number.isFinite(response.timings.duration)) {
    recoveryDuration.add(response.timings.duration, { endpoint: 'recovery-drain-one' });
  }
  if (!ok) {
    recoveryErrors.add(1);
  }

  if (body && Number.isFinite(Number(body.processed))) {
    recoveredItems.add(Number(body.processed));
  }
  if (body && Number.isFinite(Number(body.backlog))) {
    backlogGauge.add(Number(body.backlog));
    backlogSamples.add(Number(body.backlog));
    backlogSamplePresent.add(true);
  } else {
    backlogSamplePresent.add(false);
  }
}
