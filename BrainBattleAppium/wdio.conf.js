const xlsxReporter = require('./utils/xlsxReporter');
const path = require('path');
const { execSync } = require('child_process');

exports.config = {
    runner: 'local',
    port: 4723,
    specs: [
        process.env.WDIO_CI_SPEC || './tests/**/*.test.js'
    ],
    maxInstances: 1,
    capabilities: [{
        platformName: 'Android',
        'appium:automationName': 'UiAutomator2',
        'appium:app': process.env.APK_PATH || path.join(__dirname, '../BrainBattle/app/build/outputs/apk/debug/app-debug.apk'),
        'appium:autoGrantPermissions': true
    }],
    logLevel: 'warn',
    bail: 0,
    waitforTimeout: 10000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,
    framework: 'mocha',
    mochaOpts: {
        ui: 'bdd',
        timeout: 120000
    },
    
    onPrepare: function (config, capabilities) {
        console.log('Starting WDIO Appium tests...');
        xlsxReporter.startRun();
    },
    
    afterTest: function (test, context, { error, result, duration, passed, retries }) {
        // Record each test to our custom reporter
        xlsxReporter.recordTest({
            title: test.title,
            fullTitle: test.fullTitle,
            state: passed ? 'passed' : 'failed',
            duration: duration,
            error: error
        });
    },
    
    after: function (result, capabilities, specs) {
        // Intercept fatal crashes if needed
        if (result === 1 && xlsxReporter.results.length === 0) {
            xlsxReporter.recordTest({
                title: 'Appium Setup / Fatal Crash',
                fullTitle: 'Category 1: Fatal Error',
                state: 'failed',
                duration: 10,
                error: new Error('Appium connection or setup failed entirely')
            });
        }
    },
    
    onComplete: async function (exitCode, config, capabilities, results) {
        const reportPath = path.join(__dirname, 'android-report.xlsx');
        await xlsxReporter.generateReport(reportPath);
        
        try {
            execSync(`node utils/generateHtmlReport.js ${reportPath.replace('.xlsx', '.json')}`, { stdio: 'inherit' });
            execSync(`node utils/generateSummary.js ${reportPath.replace('.xlsx', '.json')}`, { stdio: 'inherit' });
        } catch (e) {
            console.error('Failed to generate HTML or Summary report:', e);
        }
    }
};
