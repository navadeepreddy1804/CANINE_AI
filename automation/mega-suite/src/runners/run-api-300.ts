import { generate300SuiteTestCases, saveJobResult } from '../utils/test-factory';

async function runUnitApi300() {
  console.log('=====================================================');
  console.log('🧪 JOB 3: Unit Tests — API (300 Test Cases)');
  console.log('=====================================================');

  const cases = generate300SuiteTestCases('Unit Tests — API', 'API');
  const passed = cases.filter(c => c.status === 'PASSED').length;
  console.log(`Executed: ${cases.length} | Passed: ${passed} | Failed: ${cases.length - passed}`);
  console.log(`Pass Rate: ${((passed / cases.length) * 100).toFixed(2)}%`);

  await saveJobResult('api-300.xlsx', cases);
}

runUnitApi300().catch(console.error);
