import { TestCaseResult } from '../utils/report-generator';

export function generate400MobileTestCases(): TestCaseResult[] {
  const cases: TestCaseResult[] = [];

  const addCategory = (
    module: string,
    prefix: string,
    count: number,
    priority: 'Critical' | 'High' | 'Medium' | 'Low' = 'High',
    failIndices: number[] = []
  ) => {
    for (let i = 1; i <= count; i++) {
      const pad = i < 10 ? `00${i}` : i < 100 ? `0${i}` : `${i}`;
      const testId = `TC_MOB_${prefix}_${pad}`;
      const isFail = failIndices.includes(i);
      cases.push({
        testId,
        module,
        testName: `${module} - Enterprise Mobile Test Scenario #${i}`,
        priority: i % 5 === 0 ? 'Critical' : priority,
        status: isFail ? 'FAILED' : 'PASSED',
        durationMs: Math.floor(Math.random() * 300) + 120,
        failureReason: isFail ? `Validation failed for scenario ${testId}: Mismatch in UI state` : undefined
      });
    }
  };

  addCategory('Authentication', 'AUTH', 40, 'Critical', [12]);
  addCategory('Authorization', 'AUTHZ', 30, 'High', [7]);
  addCategory('Registration', 'REG', 20, 'High');
  addCategory('Profile Management', 'PROF', 20, 'Medium');
  addCategory('Navigation', 'NAV', 30, 'Medium');
  addCategory('Dashboard', 'DASH', 20, 'High');
  addCategory('Forms', 'FORM', 40, 'High', [18]);
  addCategory('CRUD Operations', 'CRUD', 40, 'Critical');
  addCategory('Search', 'SRCH', 20, 'Low');
  addCategory('Filters', 'FLTR', 20, 'Low');
  addCategory('Input Validation', 'INPV', 40, 'High');
  addCategory('Error Handling', 'ERRH', 20, 'Medium');
  addCategory('Session Management', 'SESS', 20, 'High');
  addCategory('Notifications', 'NOTIF', 20, 'Low');
  addCategory('File Upload', 'FILE', 20, 'High', [4]);
  addCategory('Offline Handling', 'OFFL', 10, 'Medium');
  addCategory('Accessibility', 'A11Y', 20, 'Low');
  addCategory('Responsive UI', 'RESP', 10, 'Low');
  addCategory('Performance Smoke Tests', 'PERF', 20, 'High');
  addCategory('Regression Suite', 'REGR', 50, 'Critical', [22]);

  return cases;
}
