const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

async function generateFallbackReport() {
  console.log('Generating fallback error report due to WDIO crash.');
  const rootDir = path.join(__dirname, '..');
  const excelPath = path.join(rootDir, 'android-report.xlsx');
  const jsonPath = path.join(rootDir, 'android-report.json');

  const workbook = new ExcelJS.Workbook();
  const summarySheet = workbook.addWorksheet('Summary');
  summarySheet.columns = [
    { header: 'Metric', key: 'metric', width: 20 },
    { header: 'Value', key: 'value', width: 20 }
  ];
  summarySheet.addRow({ metric: 'Total Tests', value: 1 });
  summarySheet.addRow({ metric: 'Passed', value: 0 });
  summarySheet.addRow({ metric: 'Failed', value: 1 });
  summarySheet.addRow({ metric: 'Pass Rate', value: '0%' });

  await workbook.xlsx.writeFile(excelPath);
  
  fs.writeFileSync(jsonPath, JSON.stringify({
      total: 1,
      passed: 0,
      failed: 1,
      results: [{ category: 'System', name: 'Fatal WDIO Crash', status: 'failed', duration: 0, error: 'Suite crashed or failed to run' }],
      summaryData: {}
  }, null, 2));

  try {
      require('child_process').execSync(`node ${path.join(__dirname, 'generateHtmlReport.js')} ${jsonPath}`, { stdio: 'inherit' });
      require('child_process').execSync(`node ${path.join(__dirname, 'generateSummary.js')} ${jsonPath}`, { stdio: 'inherit' });
  } catch (e) {
      console.error(e);
  }
}

generateFallbackReport();
