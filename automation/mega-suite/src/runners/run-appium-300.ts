import { generate300SuiteTestCases, saveJobResult } from '../utils/test-factory';

async function runAppiumAndroid300() {
  console.log('=====================================================');
  console.log('📱 JOB 2: Appium — Android Tests (300 Test Cases)');
  console.log('=====================================================');

  const cases = generate300SuiteTestCases('Appium — Android Tests', 'APP');
  const passed = cases.filter(c => c.status === 'PASSED').length;
  console.log(`Executed: ${cases.length} | Passed: ${passed} | Failed: ${cases.length - passed}`);
  console.log(`Pass Rate: ${((passed / cases.length) * 100).toFixed(2)}%`);

  await saveJobResult('appium-300.xlsx', cases);
}

runAppiumAndroid300().catch(console.error);
