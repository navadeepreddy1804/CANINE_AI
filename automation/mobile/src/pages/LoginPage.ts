import { BasePage } from './BasePage';

export class LoginPage extends BasePage {
  private emailInput = '~email_input';
  private passwordInput = '~password_input';
  private loginBtn = '~login_button';
  private errorMsg = '~error_message';

  async login(email: string, pass: string) {
    await this.sendKeys(this.emailInput, email);
    await this.sendKeys(this.passwordInput, pass);
    await this.click(this.loginBtn);
  }

  async getErrorMessage(): Promise<string> {
    return await this.getText(this.errorMsg);
  }
}
