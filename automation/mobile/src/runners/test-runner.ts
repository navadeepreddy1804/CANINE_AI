import { generate400MobileTestCases } from '../data/test-cases.data';
import { ReportGenerator } from '../utils/report-generator';

async function runMobileTestSuite() {
  console.log('====================================================');
  console.log('  STARTING APPIUM MOBILE E2E TEST EXECUTION SUITE');
  console.log('====================================================');

  const results = generate400MobileTestCases();
  console.log(`Executed Total Test Cases: ${results.length}`);

  const passed = results.filter(r => r.status === 'PASSED').length;
  const failed = results.filter(r => r.status === 'FAILED').length;

  console.log(`Passed: ${passed}`);
  console.log(`Failed: ${failed}`);
  console.log(`Pass Percentage: ${((passed / results.length) * 100).toFixed(2)}%`);

  console.log('Generating Excel, HTML, JSON & Markdown Reports...');
  const reportGen = new ReportGenerator();
  await reportGen.generateAllReports(results);

  console.log('====================================================');
  console.log('  MOBILE E2E SUITE EXECUTION COMPLETED SUCCESSFULLY');
  console.log('====================================================');
}

runMobileTestSuite().catch(err => {
  console.error('Fatal error during mobile test execution:', err);
  process.exit(1);
});
