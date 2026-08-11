import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: {
    'http_req_failed': ['rate<0.05'], // failure rate under 5%
    'http_req_duration': ['p(95)<1500'] // 95th percentile under 1.5 seconds
  }
};

export default function () {
  // Use environment variable with a fallback
  const url = __ENV.BACKEND_URL || 'http://127.0.0.1:5000/api/health';
  const res = http.get(url);
  
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
