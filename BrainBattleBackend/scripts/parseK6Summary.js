const fs = require('fs');

function getMetricValue(metricObj, key) {
    if (!metricObj) return 'N/A';
    if (metricObj.values && metricObj.values[key] !== undefined) {
        return metricObj.values[key];
    }
    if (metricObj[key] !== undefined) {
        return metricObj[key];
    }
    return 'N/A';
}

function parseK6Summary() {
    const summaryPath = 'summary.json';
    if (!fs.existsSync(summaryPath)) {
        console.error('summary.json not found!');
        process.exit(1);
    }
    
    const summary = JSON.parse(fs.readFileSync(summaryPath, 'utf8'));
    const metrics = summary.metrics || {};
    
    const reqs = metrics.http_reqs || {};
    const duration = metrics.http_req_duration || {};
    const failed = metrics.http_req_failed || {};
    const checks = metrics.checks || {};
    
    const totalRequests = getMetricValue(reqs, 'count');
    const rps = getMetricValue(reqs, 'rate');
    
    const avgLatency = getMetricValue(duration, 'avg');
    const minLatency = getMetricValue(duration, 'min');
    const maxLatency = getMetricValue(duration, 'max');
    const p95Latency = getMetricValue(duration, 'p(95)');
    
    const failRate = getMetricValue(failed, 'rate');
    const checkRate = getMetricValue(checks, 'rate');
    
    let md = `### 🚀 API Load Testing Results (100 VUs / 1m)\n\n`;
    md += `| Metric | Value |\n`;
    md += `|--------|-------|\n`;
    md += `| **Total Requests Sent** | ${totalRequests} |\n`;
    md += `| **Throughput (RPS)** | ${typeof rps === 'number' ? rps.toFixed(2) : rps} req/s |\n`;
    md += `| **Request Failure Rate** | ${typeof failRate === 'number' ? (failRate * 100).toFixed(2) + '%' : failRate} |\n`;
    md += `| **Assertions Passed** | ${typeof checkRate === 'number' ? (checkRate * 100).toFixed(2) + '%' : checkRate} |\n`;
    md += `| **Average Latency** | ${typeof avgLatency === 'number' ? avgLatency.toFixed(2) + 'ms' : avgLatency} |\n`;
    md += `| **Min Latency** | ${typeof minLatency === 'number' ? minLatency.toFixed(2) + 'ms' : minLatency} |\n`;
    md += `| **Max Latency** | ${typeof maxLatency === 'number' ? maxLatency.toFixed(2) + 'ms' : maxLatency} |\n`;
    md += `| **p95 Latency** | ${typeof p95Latency === 'number' ? p95Latency.toFixed(2) + 'ms' : p95Latency} |\n`;

    if (process.env.GITHUB_STEP_SUMMARY) {
        fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, md + '\n');
    } else {
        console.log(md);
    }
}

parseK6Summary();
