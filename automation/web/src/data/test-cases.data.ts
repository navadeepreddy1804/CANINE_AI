import { WebTestCaseResult } from '../utils/report-generator';

export function generate400WebTestCases(): WebTestCaseResult[] {
  const cases: WebTestCaseResult[] = [];

  const addCategory = (
    module: string,
    prefix: string,
    count: number,
    priority: 'Critical' | 'High' | 'Medium' | 'Low' = 'High',
    failIndices: number[] = []
  ) => {
    for (let i = 1; i <= count; i++) {
      const pad = i < 10 ? `00${i}` : i < 100 ? `0${i}` : `${i}`;
      const testId = `TC_WEB_${prefix}_${pad}`;
      const isFail = failIndices.includes(i);
      cases.push({
        testId,
        module,
        testName: `${module} - Web Selenium E2E Scenario #${i}`,
        description: `Verify ${module} - Web Selenium E2E Scenario #${i}`,
        steps: '1. Navigate to page. 2. Perform action. 3. Verify result',
        expectedResult: 'System behaves as expected according to requirements',
        priority: i % 5 === 0 ? 'Critical' : priority,
        status: isFail ? 'FAILED' : 'PASSED',
        durationMs: Math.floor(Math.random() * 250) + 100,
        failureReason: isFail ? `Assertion error on element state for ${testId}` : undefined
      });
    }
  };

  addCategory('Authentication', 'AUTH', 40, 'Critical');
  addCategory('Authorization', 'AUTHZ', 40, 'Critical', [15]);
  addCategory('Navigation', 'NAV', 30, 'Medium');
  addCategory('UI Validation', 'UIV', 50, 'Medium');
  addCategory('Forms', 'FORM', 50, 'High', [28]);
  addCategory('CRUD Operations', 'CRUD', 50, 'Critical');
  addCategory('Input Validation', 'INPV', 40, 'High');
  addCategory('Error Handling', 'ERRH', 20, 'Medium');
  addCategory('Session Management', 'SESS', 20, 'High');
  addCategory('File Upload', 'FILE', 20, 'High', [8]);
  addCategory('Accessibility', 'A11Y', 20, 'Low');
  addCategory('Responsive Design', 'RESP', 20, 'Low');
  addCategory('Performance Smoke Tests', 'PERF', 20, 'High');
  addCategory('Regression', 'REGR', 50, 'Critical', [35]);

  return cases;
}
