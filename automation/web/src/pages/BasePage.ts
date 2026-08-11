import { Builder, By, WebDriver, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome';
import { SELENIUM_CONFIG } from '../config/selenium.config';

export class BasePage {
  protected driver: WebDriver | null;

  constructor(driver?: WebDriver) {
    this.driver = driver || null;
  }

  async initDriver(): Promise<WebDriver> {
    if (this.driver) return this.driver;

    const options = new chrome.Options();
    if (SELENIUM_CONFIG.headless) {
      options.addArguments('--headless=new');
    }
    options.addArguments('--no-sandbox', '--disable-dev-shm-usage', '--window-size=1920,1080');

    this.driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .build();

    return this.driver;
  }

  async navigateTo(path = ''): Promise<void> {
    const driver = await this.initDriver();
    const url = `${SELENIUM_CONFIG.baseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
    await driver.get(url);
  }

  async findElement(by: By, timeout = SELENIUM_CONFIG.timeoutMs) {
    const driver = await this.initDriver();
    return await driver.wait(until.elementLocated(by), timeout);
  }

  async click(by: By): Promise<void> {
    const el = await this.findElement(by);
    await el.click();
  }

  async type(by: By, text: string): Promise<void> {
    const el = await this.findElement(by);
    await el.clear();
    await el.sendKeys(text);
  }

  async quit(): Promise<void> {
    if (this.driver) {
      await this.driver.quit();
      this.driver = null;
    }
  }
}
