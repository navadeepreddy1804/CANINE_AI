const fs = require('fs');
const path = require('path');

function generateHtmlReport(jsonPath) {
  if (!fs.existsSync(jsonPath)) {
      console.log('No json results found for HTML generation');
      return;
  }
  const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  const htmlPath = path.join(path.dirname(jsonPath), 'execution-report.html');

  const htmlContent = `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>BrainBattle Appium Execution Report</title>
  <style>
    body { font-family: Arial, sans-serif; background-color: #121212; color: #ffffff; margin: 0; padding: 20px; }
    h1 { text-align: center; color: #2196F3; }
    .summary-card { background-color: #1e1e1e; border-radius: 8px; padding: 20px; margin-bottom: 20px; display: flex; justify-content: space-around; }
    .stat { text-align: center; }
    .stat h3 { margin: 0; font-size: 24px; }
    .stat p { margin: 5px 0 0; color: #aaaaaa; }
    .passed-text { color: #4caf50; }
    .failed-text { color: #f44336; }
    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
    th, td { border: 1px solid #333; padding: 10px; text-align: left; }
    th { background-color: #1e1e1e; }
    tr:nth-child(even) { background-color: #1a1a1a; }
    .badge { padding: 4px 8px; border-radius: 4px; font-weight: bold; }
    .badge.passed { background-color: #4caf50; color: #fff; }
    .badge.failed { background-color: #f44336; color: #fff; }
  </style>
</head>
<body>
  <h1>BrainBattle Appium Test Report</h1>
  <div class="summary-card">
    <div class="stat">
      <h3>${data.total}</h3>
      <p>Total Tests</p>
    </div>
    <div class="stat">
      <h3 class="passed-text">${data.passed}</h3>
      <p>Passed</p>
    </div>
    <div class="stat">
      <h3 class="failed-text">${data.failed}</h3>
      <p>Failed</p>
    </div>
  </div>
  
  <h2>Detailed Results</h2>
  <table>
    <thead>
      <tr>
        <th>Category</th>
        <th>Test Name</th>
        <th>Status</th>
        <th>Duration (ms)</th>
        <th>Error Stack</th>
      </tr>
    </thead>
    <tbody>
      ${data.results.map(r => `
      <tr>
        <td>${r.category}</td>
        <td>${r.name}</td>
        <td><span class="badge ${r.status}">${r.status}</span></td>
        <td>${r.duration}</td>
        <td>${r.error || '-'}</td>
      </tr>
      `).join('')}
    </tbody>
  </table>
</body>
</html>
  `;
  fs.writeFileSync(htmlPath, htmlContent, 'utf8');
  console.log(`HTML report generated at: ${htmlPath}`);
}

const arg = process.argv[2];
if (arg) {
    generateHtmlReport(arg);
} else {
    module.exports = { generateHtmlReport };
}
