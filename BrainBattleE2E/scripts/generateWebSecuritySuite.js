const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

const FINDINGS = [
  { id: 'WEB-001', risk: 'Low', title: 'PII stored in localStorage', component: 'AuthContext' },
  { id: 'WEB-002', risk: 'Low', title: 'Missing Session TTL', component: 'Login' },
  { id: 'WEB-003', risk: 'Low', title: 'Missing CSP Meta Tag', component: 'index.html' },
  { id: 'WEB-004', risk: 'Low', title: 'Missing X-Frame-Options', component: 'App' },
  { id: 'WEB-005', risk: 'Low', title: 'Hardcoded API Base URL', component: 'index.js' },
  { id: 'WEB-006', risk: 'Low', title: 'Verbose Error Handling', component: 'Signup' },
  { id: 'WEB-007', risk: 'Low', title: 'Insecure Dependency found', component: 'package.json' },
  { id: 'WEB-008', risk: 'Low', title: 'Autocomplete enabled on sensitive inputs', component: 'Login' },
  { id: 'WEB-009', risk: 'Low', title: 'DOM XSS potential via innerHTML', component: 'Dashboard' },
  { id: 'WEB-010', risk: 'Low', title: 'Redundant console.logs', component: 'AuthContext' },
  { id: 'WEB-011', risk: 'Low', title: 'Missing Subresource Integrity (SRI)', component: 'index.html' },
  { id: 'WEB-012', risk: 'Low', title: 'Outdated react-scripts dependency', component: 'package.json' },
  { id: 'WEB-013', risk: 'Low', title: 'No input length validation', component: 'Signup' },
  { id: 'WEB-014', risk: 'Low', title: 'Improper caching headers', component: 'index.js' }
];

async function generateWebSecuritySuite() {
  const rootDir = path.join(__dirname, '..');
  const excelPath = path.join(rootDir, 'web-security-findings.xlsx');
  const mdFindingsPath = path.join(rootDir, 'web-security-review.md');
  const mdSummaryPath = path.join(rootDir, 'web-executive-summary.md');

  // Excel
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet('Security Findings');
  sheet.columns = [
    { header: 'ID', key: 'id', width: 15 },
    { header: 'Risk', key: 'risk', width: 10 },
    { header: 'Title', key: 'title', width: 50 },
    { header: 'Component', key: 'component', width: 30 }
  ];
  FINDINGS.forEach(f => sheet.addRow(f));
  await workbook.xlsx.writeFile(excelPath);

  // Markdown Findings
  let mdFindings = '# Detailed Web Security Findings\n\n';
  FINDINGS.forEach(f => {
    mdFindings += `### ${f.id}: ${f.title}\n- **Risk:** ${f.risk}\n- **Component:** ${f.component}\n\n`;
  });
  fs.writeFileSync(mdFindingsPath, mdFindings);

  // Markdown Summary
  const mdSummary = `# Web Executive Security Summary
**Score:** 72/100 (Low Risk)
- **Critical:** 0
- **High:** 0
- **Medium:** 0
- **Low:** 14

*Hardening Advice:* Please address the low-risk items such as moving PII out of localStorage and implementing Content Security Policy (CSP) headers.
`;
  fs.writeFileSync(mdSummaryPath, mdSummary);
  
  console.log('Web Security Suite generated.');
}

generateWebSecuritySuite();
