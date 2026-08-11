import path from 'path';

export const APPIUM_CONFIG = {
  hostname: process.env.APPIUM_HOST || '127.0.0.1',
  port: parseInt(process.env.APPIUM_PORT || '4723', 10),
  path: '/',
  capabilities: {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:deviceName': process.env.ANDROID_DEVICE || 'Android Emulator',
    'appium:app': path.resolve(__dirname, '../../app/app-debug.apk'),
    'appium:appPackage': 'com.canineai.android',
    'appium:appActivity': 'com.canineai.android.ui.MainActivity',
    'appium:noReset': false,
    'appium:fullReset': false,
    'appium:newCommandTimeout': 300
  }
};
