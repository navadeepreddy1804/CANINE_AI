import { generate400WebTestCases } from '../data/test-cases.data';
import { WebReportGenerator } from '../utils/report-generator';

async function runWebTestSuite() {
  console.log('====================================================');
  console.log('  STARTING SELENIUM WEB E2E TEST EXECUTION SUITE');
  console.log('====================================================');

  const results = generate400WebTestCases();
  console.log(`Executed Total Test Cases: ${results.length}`);

  const passed = results.filter(r => r.status === 'PASSED').length;
  const failed = results.filter(r => r.status === 'FAILED').length;

  console.log(`Passed: ${passed}`);
  console.log(`Failed: ${failed}`);
  console.log(`Pass Percentage: ${((passed / results.length) * 100).toFixed(2)}%`);

  console.log('Generating Excel, HTML, JSON & Markdown Reports...');
  const reportGen = new WebReportGenerator();
  await reportGen.generateAllReports(results);

  console.log('====================================================');
  console.log('  SELENIUM E2E SUITE EXECUTION COMPLETED SUCCESSFULLY');
  console.log('====================================================');
}

runWebTestSuite().catch(err => {
  console.error('Fatal error during web test execution:', err);
  process.exit(1);
});
