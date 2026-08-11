import fs from 'fs';
import path from 'path';
import ExcelJS from 'exceljs';

export interface WebTestCaseResult {
  testId: string;
  module: string;
  testName: string;
  description: string;
  steps: string;
  expectedResult: string;
  priority: 'Critical' | 'High' | 'Medium' | 'Low';
  status: 'PASSED' | 'FAILED' | 'SKIPPED' | 'BLOCKED';
  durationMs: number;
  failureReason?: string;
}

export class WebReportGenerator {
  private baseDir: string;

  constructor(baseDir?: string) {
    this.baseDir = baseDir || path.resolve(__dirname, '../../reports');
  }

  public async generateAllReports(results: WebTestCaseResult[]): Promise<void> {
    const excelDir = path.join(this.baseDir, 'Excel');
    const htmlDir = path.join(this.baseDir, 'HTML');
    const jsonDir = path.join(this.baseDir, 'JSON');
    const summaryDir = path.join(this.baseDir, 'Summary');

    fs.mkdirSync(excelDir, { recursive: true });
    fs.mkdirSync(htmlDir, { recursive: true });
    fs.mkdirSync(jsonDir, { recursive: true });
    fs.mkdirSync(summaryDir, { recursive: true });

    await this.generateExcelReports(excelDir, results);
    this.generateHtmlReports(htmlDir, results);
    this.generateJsonReport(jsonDir, results);
    this.generateMarkdownSummary(summaryDir, results);
  }

  private async generateExcelReports(excelDir: string, results: WebTestCaseResult[]): Promise<void> {
    const workbook = new ExcelJS.Workbook();
    
    // Sheet 1: Executed Test Cases
    const sheet1 = workbook.addWorksheet('Executed Test Cases');
    sheet1.columns = [
      { header: 'Test ID', key: 'testId', width: 15 },
      { header: 'Module', key: 'module', width: 20 },
      { header: 'Test Case Name', key: 'testName', width: 35 },
      { header: 'Description', key: 'description', width: 50 },
      { header: 'Steps', key: 'steps', width: 50 },
      { header: 'Expected Result', key: 'expectedResult', width: 50 },
      { header: 'Status', key: 'status', width: 12 }
    ];
    results.forEach(r => sheet1.addRow(r));

    // Sheet 2: Passed Tests
    const sheet2 = workbook.addWorksheet('Passed Tests');
    sheet2.columns = sheet1.columns;
    results.filter(r => r.status === 'PASSED').forEach(r => sheet2.addRow(r));

    // Sheet 3: Failed Tests
    const sheet3 = workbook.addWorksheet('Failed Tests');
    sheet3.columns = [
      ...sheet1.columns,
      { header: 'Failure Reason', key: 'failureReason', width: 40 }
    ];
    results.filter(r => r.status === 'FAILED').forEach(r => sheet3.addRow(r));

    // Sheet 4: Skipped Tests
    const sheet4 = workbook.addWorksheet('Skipped Tests');
    sheet4.columns = sheet1.columns;
    results.filter(r => r.status === 'SKIPPED').forEach(r => sheet4.addRow(r));

    // Sheet 5: Metrics
    const total = results.length;
    const passed = results.filter(r => r.status === 'PASSED').length;
    const failed = results.filter(r => r.status === 'FAILED').length;
    const skipped = results.filter(r => r.status === 'SKIPPED').length;
    const passRate = total > 0 ? ((passed / total) * 100).toFixed(2) + '%' : '0%';

    const sheet5 = workbook.addWorksheet('Execution Metrics');
    sheet5.columns = [
      { header: 'Metric Name', key: 'name', width: 25 },
      { header: 'Value', key: 'value', width: 20 }
    ];
    sheet5.addRow({ name: 'Total Test Cases', value: total });
    sheet5.addRow({ name: 'Passed', value: passed });
    sheet5.addRow({ name: 'Failed', value: failed });
    sheet5.addRow({ name: 'Skipped', value: skipped });
    sheet5.addRow({ name: 'Pass Rate', value: passRate });

    await workbook.xlsx.writeFile(path.join(excelDir, 'Automation_Test_Report.xlsx'));
    await workbook.xlsx.writeFile(path.join(excelDir, 'Summary_Report.xlsx'));
  }

  private generateHtmlReports(htmlDir: string, results: WebTestCaseResult[]): void {
    const total = results.length;
    const passed = results.filter(r => r.status === 'PASSED').length;
    const failed = results.filter(r => r.status === 'FAILED').length;
    const skipped = results.filter(r => r.status === 'SKIPPED').length;
    const passRate = total > 0 ? ((passed / total) * 100).toFixed(2) : '0';

    const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Selenium Web E2E Execution Report</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
        .header { background: linear-gradient(135deg, #1e293b, #0f172a); padding: 20px; border-radius: 8px; margin-bottom: 20px; border: 1px solid #334155; }
        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-bottom: 25px; }
        .metric-card { background: #1e293b; padding: 15px; border-radius: 8px; border-left: 4px solid #3b82f6; text-align: center; }
        .metric-card.passed { border-color: #22c55e; }
        .metric-card.failed { border-color: #ef4444; }
        .metric-value { font-size: 28px; font-weight: bold; margin-top: 5px; }
        table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 8px; overflow: hidden; }
        th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #334155; }
        th { background: #0f172a; color: #94a3b8; text-transform: uppercase; font-size: 12px; }
        tr:hover { background: #334155; }
        .badge { padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 11px; }
        .badge-PASSED { background: #15803d; color: #dcfce7; }
        .badge-FAILED { background: #b91c1c; color: #fee2e2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Live GitHub Pages Selenium E2E Report</h1>
        <p>Executed on: ${new Date().toISOString()}</p>
    </div>
    <div class="metrics-grid">
        <div class="metric-card"><div class="metric-label">Total Tests</div><div class="metric-value">${total}</div></div>
        <div class="metric-card passed"><div class="metric-label">Passed</div><div class="metric-value">${passed}</div></div>
        <div class="metric-card failed"><div class="metric-label">Failed</div><div class="metric-value">${failed}</div></div>
        <div class="metric-card"><div class="metric-label">Pass Rate</div><div class="metric-value">${passRate}%</div></div>
    </div>
    <table>
        <thead>
            <tr><th>Test ID</th><th>Module</th><th>Test Name</th><th>Priority</th><th>Status</th><th>Time (ms)</th></tr>
        </thead>
        <tbody>
            ${results.map(r => `
                <tr>
                    <td>${r.testId}</td>
                    <td>${r.module}</td>
                    <td>${r.testName}</td>
                    <td>${r.priority}</td>
                    <td><span class="badge badge-${r.status}">${r.status}</span></td>
                    <td>${r.durationMs}</td>
                </tr>
            `).join('')}
        </tbody>
    </table>
</body>
</html>`;

    fs.writeFileSync(path.join(htmlDir, 'execution-report.html'), htmlContent);
  }

  private generateJsonReport(jsonDir: string, results: WebTestCaseResult[]): void {
    fs.writeFileSync(
      path.join(jsonDir, 'execution-results.json'),
      JSON.stringify(results, null, 2)
    );
  }

  private generateMarkdownSummary(summaryDir: string, results: WebTestCaseResult[]): void {
    const total = results.length;
    const passed = results.filter(r => r.status === 'PASSED').length;
    const failed = results.filter(r => r.status === 'FAILED').length;
    const skipped = results.filter(r => r.status === 'SKIPPED').length;
    const passRate = total > 0 ? ((passed / total) * 100).toFixed(2) + '%' : '0%';

    const content = `# Live GitHub Pages E2E Execution Summary

**Execution Date**: ${new Date().toISOString()}  
**Target Environment**: LIVE GitHub Pages Deployment  
**Total Test Cases**: ${total}  

| Metric | Value |
| --- | --- |
| Executed | ${total} |
| Passed | ${passed} |
| Failed | ${failed} |
| Skipped | ${skipped} |
| **Pass Percentage** | **${passRate}** |

### Executed Tests Summary
${results.slice(0, 15).map(r => `- [${r.status}] \`${r.testId}\` - ${r.testName} (${r.durationMs}ms)`).join('\n')}
`;

    fs.writeFileSync(path.join(summaryDir, 'summary.md'), content);
  }
}
