/**
 * CanineAI — Login / Registration Page
 * Client-side validation, tab switching, and post-registration redirect.
 */

document.addEventListener("DOMContentLoaded", () => {

    // ── Element references ────────────────────────────────────────────────────

    const tabLogin         = document.getElementById("tab-login");
    const tabRegister      = document.getElementById("tab-register");
    const loginForm        = document.getElementById("login-form");
    const registerForm     = document.getElementById("register-form");

    // Login form
    const loginUsernameInput  = document.getElementById("login-username");
    const loginPasswordInput  = document.getElementById("login-password");
    const loginSubmitBtn      = document.getElementById("login-submit-btn");
    const loginPasswordToggle = document.getElementById("login-password-toggle");
    const loginUsernameError  = document.getElementById("login-username-error");
    const loginPasswordError  = document.getElementById("login-password-error");

    // Register form
    const regFullNameInput        = document.getElementById("reg-fullName");
    const regUsernameInput        = document.getElementById("reg-username");
    const regEmailInput           = document.getElementById("reg-email");
    const regPhoneInput           = document.getElementById("reg-phone");
    const regPasswordInput        = document.getElementById("reg-password");
    const regConfirmPasswordInput = document.getElementById("reg-confirmPassword");
    const regSubmitBtn            = document.getElementById("register-submit-btn");
    const regPasswordToggle       = document.getElementById("reg-password-toggle");

    const regFullNameError        = document.getElementById("reg-fullName-error");
    const regUsernameError        = document.getElementById("reg-username-error");
    const regEmailError           = document.getElementById("reg-email-error");
    const regPhoneError           = document.getElementById("reg-phone-error");
    const regPasswordError        = document.getElementById("reg-password-error");
    const regConfirmPasswordError = document.getElementById("reg-confirmPassword-error");

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const phoneRegex = /^\+?[0-9 .\-()]{7,25}$/;

    // ── Tab switching ─────────────────────────────────────────────────────────

    function showLoginTab() {
        tabLogin.classList.add("active");
        tabRegister.classList.remove("active");
        loginForm.style.display    = "block";
        registerForm.style.display = "none";
        history.replaceState(null, "", window.location.pathname
            + window.location.search.replace(/#.*/, "") + "#signin");
    }

    function showRegisterTab() {
        tabLogin.classList.remove("active");
        tabRegister.classList.add("active");
        loginForm.style.display    = "none";
        registerForm.style.display = "block";
        history.replaceState(null, "", window.location.pathname
            + window.location.search.replace(/#.*/, "") + "#signup");
    }

    if (tabLogin)    tabLogin.addEventListener("click",    showLoginTab);
    if (tabRegister) tabRegister.addEventListener("click", showRegisterTab);

    // ── Initial tab selection based on URL params / hash ─────────────────────

    const params        = new URLSearchParams(window.location.search);
    const isRegistered  = params.has("registered");
    const isRegError    = params.has("registerError");
    const isUserExists  = params.has("userExists");
    const hash          = window.location.hash;

    if (isRegistered || isRegError || isUserExists || hash === "#signup") {
        showRegisterTab();
    } else {
        showLoginTab();
    }

    // ── Post-registration success: countdown → redirect to sign-in ────────────

    if (isRegistered) {
        const countdownEl = document.getElementById("redirect-countdown");
        let seconds = 3;

        const tick = setInterval(() => {
            seconds -= 1;
            if (countdownEl) countdownEl.textContent = seconds;

            if (seconds <= 0) {
                clearInterval(tick);
                // Switch to sign-in tab and clean up URL params
                showLoginTab();
                // Remove the ?registered param so the banner doesn't persist
                const clean = window.location.pathname + "#signin";
                history.replaceState(null, "", clean);
                // Remove banner
                const banner = document.getElementById("register-success-banner");
                if (banner) banner.remove();
            }
        }, 1000);
    }

    // ── Password visibility toggles ───────────────────────────────────────────

    function setupPasswordToggle(input, btn) {
        if (!input || !btn) return;
        btn.addEventListener("click", () => {
            const isHidden = input.type === "password";
            input.type     = isHidden ? "text" : "password";
            btn.textContent = isHidden ? "HIDE" : "SHOW";
        });
    }

    setupPasswordToggle(loginPasswordInput, loginPasswordToggle);
    setupPasswordToggle(regPasswordInput,   regPasswordToggle);

    // ── Validation helpers ────────────────────────────────────────────────────

    function showError(input, el, msg) {
        if (input) input.classList.add("is-invalid");
        if (el)    { el.textContent = msg; el.style.display = "block"; }
    }

    function clearError(input, el) {
        if (input) input.classList.remove("is-invalid");
        if (el)    el.style.display = "none";
    }

    // ── Login validation ──────────────────────────────────────────────────────

    function validateLoginEmail() {
        const v = (loginUsernameInput?.value || "").trim();
        if (!v)                  { showError(loginUsernameInput, loginUsernameError, "Email is required");               return false; }
        if (!emailRegex.test(v)) { showError(loginUsernameInput, loginUsernameError, "Enter a valid email address");     return false; }
        clearError(loginUsernameInput, loginUsernameError);
        return true;
    }

    function validateLoginPassword() {
        const v = (loginPasswordInput?.value || "").trim();
        if (!v)          { showError(loginPasswordInput, loginPasswordError, "Password is required");              return false; }
        if (v.length < 8){ showError(loginPasswordInput, loginPasswordError, "Password must be at least 8 characters"); return false; }
        clearError(loginPasswordInput, loginPasswordError);
        return true;
    }

    function refreshLoginBtn() {
        if (!loginSubmitBtn) return;
        const email = (loginUsernameInput?.value || "").trim();
        const pw    = (loginPasswordInput?.value  || "").trim();
        loginSubmitBtn.disabled = !(emailRegex.test(email) && pw.length >= 8);
    }

    loginUsernameInput?.addEventListener("input", refreshLoginBtn);
    loginUsernameInput?.addEventListener("blur",  validateLoginEmail);
    loginPasswordInput?.addEventListener("input", refreshLoginBtn);
    loginPasswordInput?.addEventListener("blur",  validateLoginPassword);

    // ── Register validation ───────────────────────────────────────────────────

    function validateFullName() {
        const v = (regFullNameInput?.value || "").trim();
        if (!v) { showError(regFullNameInput, regFullNameError, "Full name is required"); return false; }
        clearError(regFullNameInput, regFullNameError);
        return true;
    }

    function validateRegUsername() {
        const v = (regUsernameInput?.value || "").trim();
        if (!v) { showError(regUsernameInput, regUsernameError, "Username is required"); return false; }
        if (v.length < 3) { showError(regUsernameInput, regUsernameError, "Username must be at least 3 characters"); return false; }
        clearError(regUsernameInput, regUsernameError);
        return true;
    }

    function validateRegEmail() {
        const v = (regEmailInput?.value || "").trim();
        if (!v)                  { showError(regEmailInput, regEmailError, "Email is required");                  return false; }
        if (!emailRegex.test(v)) { showError(regEmailInput, regEmailError, "Enter a valid email address");        return false; }
        clearError(regEmailInput, regEmailError);
        return true;
    }

    function validateRegPhone() {
        const v = (regPhoneInput?.value || "").trim();
        if (!v)                  { showError(regPhoneInput, regPhoneError, "Phone number is required");           return false; }
        if (!phoneRegex.test(v)) { showError(regPhoneInput, regPhoneError, "Enter a valid phone number");         return false; }
        clearError(regPhoneInput, regPhoneError);
        return true;
    }

    function validateRegPassword() {
        const v = (regPasswordInput?.value || "").trim();
        if (!v)              { showError(regPasswordInput, regPasswordError, "Password is required");                                            return false; }
        if (v.length < 8)    { showError(regPasswordInput, regPasswordError, "Password must be at least 8 characters");                         return false; }
        if (!/[A-Z]/.test(v)){ showError(regPasswordInput, regPasswordError, "Password must contain at least one uppercase letter");            return false; }
        if (!/[a-z]/.test(v)){ showError(regPasswordInput, regPasswordError, "Password must contain at least one lowercase letter");            return false; }
        if (!/\d/.test(v))   { showError(regPasswordInput, regPasswordError, "Password must contain at least one number");                      return false; }
        clearError(regPasswordInput, regPasswordError);
        return true;
    }

    function validateRegConfirmPassword() {
        const v  = (regConfirmPasswordInput?.value || "").trim();
        const pw = (regPasswordInput?.value        || "").trim();
        if (!v)        { showError(regConfirmPasswordInput, regConfirmPasswordError, "Please confirm your password"); return false; }
        if (v !== pw)  { showError(regConfirmPasswordInput, regConfirmPasswordError, "Passwords do not match");       return false; }
        clearError(regConfirmPasswordInput, regConfirmPasswordError);
        return true;
    }

    // Re-validate confirm password whenever main password changes
    regPasswordInput?.addEventListener("blur", () => {
        validateRegPassword();
        if ((regConfirmPasswordInput?.value || "").trim()) validateRegConfirmPassword();
    });

    regFullNameInput?.addEventListener("blur",        validateFullName);
    regUsernameInput?.addEventListener("blur",        validateRegUsername);
    regEmailInput?.addEventListener("blur",           validateRegEmail);
    regPhoneInput?.addEventListener("blur",           validateRegPhone);
    regConfirmPasswordInput?.addEventListener("blur", validateRegConfirmPassword);

    const regSecurityQuestionInput = document.getElementById("reg-securityQuestion");
    const regSecurityAnswerInput   = document.getElementById("reg-securityAnswer");
    const regSecurityQuestionError = document.getElementById("reg-securityQuestion-error");
    const regSecurityAnswerError   = document.getElementById("reg-securityAnswer-error");

    function validateSecurityQuestion() {
        const v = (regSecurityQuestionInput?.value || "").trim();
        if (!v) { showError(regSecurityQuestionInput, regSecurityQuestionError, "Security question is required"); return false; }
        clearError(regSecurityQuestionInput, regSecurityQuestionError);
        return true;
    }

    function validateSecurityAnswer() {
        const v = (regSecurityAnswerInput?.value || "").trim();
        if (!v) { showError(regSecurityAnswerInput, regSecurityAnswerError, "Security answer is required"); return false; }
        clearError(regSecurityAnswerInput, regSecurityAnswerError);
        return true;
    }

    regSecurityQuestionInput?.addEventListener("blur", validateSecurityQuestion);
    regSecurityAnswerInput?.addEventListener("blur",   validateSecurityAnswer);

    function isPasswordStrong(pw) {
        return pw.length >= 8 && /[A-Z]/.test(pw) && /[a-z]/.test(pw) && /\d/.test(pw);
    }

    function refreshRegisterBtn() {
        if (!regSubmitBtn) return;
        const ok = (regFullNameInput?.value        || "").trim() !== ""
            &&     (regUsernameInput?.value        || "").trim().length >= 3
            &&     emailRegex.test((regEmailInput?.value || "").trim())
            &&     phoneRegex.test((regPhoneInput?.value || "").trim())
            &&     isPasswordStrong((regPasswordInput?.value || "").trim())
            &&     (regConfirmPasswordInput?.value || "").trim() === (regPasswordInput?.value || "").trim()
            &&     (regSecurityQuestionInput?.value || "").trim() !== ""
            &&     (regSecurityAnswerInput?.value || "").trim() !== "";
        regSubmitBtn.disabled = !ok;
    }

    [regFullNameInput, regUsernameInput, regEmailInput, regPhoneInput,
     regPasswordInput, regConfirmPasswordInput, regSecurityQuestionInput, regSecurityAnswerInput].forEach(el => {
        el?.addEventListener("input", refreshRegisterBtn);
    });

    // ── Form submit handlers ──────────────────────────────────────────────────

    loginForm?.addEventListener("submit", (e) => {
        const ok = validateLoginEmail() & validateLoginPassword();   // both must run
        if (!ok) { e.preventDefault(); return; }
        loginSubmitBtn.disabled  = true;
        loginSubmitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i>Signing in&hellip;';
    });

    registerForm?.addEventListener("submit", (e) => {
        const ok = [
            validateFullName(),
            validateRegUsername(),
            validateRegEmail(),
            validateRegPhone(),
            validateRegPassword(),
            validateRegConfirmPassword(),
            validateSecurityQuestion(),
            validateSecurityAnswer()
        ].every(Boolean);

        if (!ok) { e.preventDefault(); return; }
        regSubmitBtn.disabled  = true;
        regSubmitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i>Creating account&hellip;';
    });

    // ── Initial button state ──────────────────────────────────────────────────

    refreshLoginBtn();
    refreshRegisterBtn();
});
