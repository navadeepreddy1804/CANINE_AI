import dotenv from 'dotenv';
dotenv.config();

export const SELENIUM_CONFIG = {
  baseUrl: process.env.BASE_URL || 'https://navadeepreddy1804.github.io/CANINE_AI/',
  browser: process.env.BROWSER || 'chrome',
  headless: process.env.HEADLESS !== 'false',
  timeoutMs: parseInt(process.env.TIMEOUT_MS || '15000', 10),
  retryAttempts: parseInt(process.env.RETRY_ATTEMPTS || '2', 10)
};
