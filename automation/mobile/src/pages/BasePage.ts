export class BasePage {
  protected driver: any;

  constructor(driver: any) {
    this.driver = driver;
  }

  async waitForElement(selector: string, timeout = 10000) {
    if (!this.driver) return null;
    const el = await this.driver.$(selector);
    await el.waitForDisplayed({ timeout });
    return el;
  }

  async click(selector: string) {
    const el = await this.waitForElement(selector);
    if (el) await el.click();
  }

  async sendKeys(selector: string, value: string) {
    const el = await this.waitForElement(selector);
    if (el) {
      await el.clearValue();
      await el.addValue(value);
    }
  }

  async getText(selector: string): Promise<string> {
    const el = await this.waitForElement(selector);
    return el ? await el.getText() : '';
  }

  async isDisplayed(selector: string): Promise<boolean> {
    try {
      const el = await this.driver.$(selector);
      return await el.isDisplayed();
    } catch {
      return false;
    }
  }
}
