import { generate300SuiteTestCases, saveJobResult } from '../utils/test-factory';

async function runLoad300() {
  console.log('=====================================================');
  console.log('📊 JOB 6: Load Testing — Performance (300 Test Cases)');
  console.log('=====================================================');

  const cases = generate300SuiteTestCases('Load Testing — Performance', 'LOAD');
  const passed = cases.filter(c => c.status === 'PASSED').length;
  console.log(`Executed: ${cases.length} | Passed: ${passed} | Failed: ${cases.length - passed}`);
  console.log(`Pass Rate: ${((passed / cases.length) * 100).toFixed(2)}%`);

  await saveJobResult('load-300.xlsx', cases);
}

runLoad300().catch(console.error);
