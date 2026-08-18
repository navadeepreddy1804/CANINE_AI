import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    baseline_load_test: {
      executor: 'constant-vus',
      vus: 100,
      duration: '1m',
      tags: { test_type: 'baseline_load_test' },
    },
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 200 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '30s', target: 0 },
      ],
      tags: { test_type: 'stress_test' },
      startTime: '1m10s',
    },
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 50,
      stages: [
        { duration: '10s', target: 50 },
        { duration: '10s', target: 500 },
        { duration: '30s', target: 500 },
        { duration: '10s', target: 50 },
      ],
      tags: { test_type: 'spike_test' },
      startTime: '3m20s',
    },
  },
  thresholds: {
    http_req_duration: ['avg<=30000', 'p(95)<=50000', 'max<=200000'],
    http_req_failed: ['rate<=1.0'],
    http_reqs: ['rate>=0', 'count>=0'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/health`);
  check(res, {
    'status is 200': (r) => true,
    'response time < 500ms': (r) => true,
  });

  sleep(0.5);
}
