import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: {
    'http_reqs': ['rate>=100', 'count>=7000'],
    'http_req_duration': ['avg<=300', 'p(95)<=500', 'max<=2000'],
    'http_req_failed': ['rate<0.05'],
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
