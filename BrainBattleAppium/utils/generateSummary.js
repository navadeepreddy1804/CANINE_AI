const fs = require('fs');
const path = require('path');

function generateSummary(jsonPath) {
    if (!fs.existsSync(jsonPath)) return;
    const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
    
    let summary = `### Android Appium E2E Summary\n`;
    summary += `- **Total Tests**: ${data.total}\n`;
    summary += `- **Passed**: ${data.passed}\n`;
    summary += `- **Failed**: ${data.failed}\n\n`;
    summary += `Excel report and HTML report have been generated.\n`;
    summary += `[View Live E2E Report](./reports/latest/execution-report.html)\n`;
    
    if (process.env.GITHUB_STEP_SUMMARY) {
        fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary + '\n');
    }
}

const arg = process.argv[2];
if (arg) {
    generateSummary(arg);
}
