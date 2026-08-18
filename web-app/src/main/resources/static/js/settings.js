/**
 * CanineAI Web Settings Workspace Scripts
 */

document.addEventListener("DOMContentLoaded", () => {
    // LLM Temperature Slider
    const tempSlider = document.getElementById("temp-slider");
    const tempLabel = document.getElementById("temp-slider-val");

    if (tempSlider && tempLabel) {
        tempSlider.addEventListener("input", (e) => {
            tempLabel.textContent = e.target.value;
        });
        tempSlider.addEventListener("change", (e) => {
            fetch(`/settings/save?temperature=${e.target.value}`, { method: 'POST' });
        });
    }

    // Trigger weekly digest email report
    const triggerEmailBtn = document.getElementById("trigger-weekly-email-btn");
    if (triggerEmailBtn) {
        triggerEmailBtn.addEventListener("click", () => {
            triggerEmailBtn.disabled = true;
            triggerEmailBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i>Sending Digest Email...';

            fetch('/settings/email-digest', { method: 'POST' })
                .then(() => {
                    triggerEmailBtn.disabled = false;
                    triggerEmailBtn.innerHTML = '<i class="fa fa-envelope me-2"></i>Email Weekly Digest Report Now';
                    if (typeof CanineUI !== "undefined") {
                        CanineUI.showToast("Weekly diagnostic summary report emailed successfully.", "success");
                    }
                });
        });
    }

    // Toggle theme/notifications persistence
    const darkModeSwitch = document.getElementById("darkModeSwitch");
    if (darkModeSwitch) {
        // Sync state on load
        darkModeSwitch.checked = document.documentElement.classList.contains('dark-theme');
        
        darkModeSwitch.addEventListener("change", (e) => {
            fetch(`/settings/save?darkMode=${e.target.checked}`, { method: 'POST' })
                .then(() => {
                    CanineUI.toggleTheme();
                });
        });
    }

    const notifySwitch = document.getElementById("notifySwitch");
    if (notifySwitch) {
        notifySwitch.addEventListener("change", (e) => {
            fetch(`/settings/save?soundAlerts=${e.target.checked}`, { method: 'POST' });
        });
    }

    // Clear Storage log caches
    const cleanStorageBtn = document.getElementById("clean-storage-btn");
    const storageUsedText = document.getElementById("storage-used-text");
    const storageBar = document.getElementById("storage-progress-bar");

    if (cleanStorageBtn && storageUsedText && storageBar) {
        cleanStorageBtn.addEventListener("click", () => {
            cleanStorageBtn.disabled = true;
            cleanStorageBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i>Clearing Storage...';

            fetch('/settings/clear-storage', { method: 'POST' })
                .then(res => res.json())
                .then(data => {
                    storageUsedText.textContent = `${data.used.toFixed(2)} GB used`;
                    if (storageBar) {
                        storageBar.style.width = `${(data.used / data.max) * 100}%`;
                    }
                    cleanStorageBtn.disabled = false;
                    cleanStorageBtn.innerHTML = '<i class="fa fa-broom me-2"></i>Clear EMR Storage Data';

                    if (typeof CanineUI !== "undefined") {
                        CanineUI.showToast("Clinical storage cleared successfully.", "success");
                    }
                });
        });
    }

    // Mock terminate device session logs
    const sessionRows = document.querySelectorAll(".device-session-row");
    sessionRows.forEach(row => {
        const terminateBtn = row.querySelector(".terminate-device-session");
        if (terminateBtn) {
            terminateBtn.addEventListener("click", () => {
                row.style.opacity = "0.5";
                terminateBtn.disabled = true;
                setTimeout(() => {
                    row.remove();
                    if (typeof CanineUI !== "undefined") {
                        CanineUI.showToast("Logged out of device session successfully.", "success");
                    }
                }, 500);
            });
        }
    });

    // Change Password Form Submission
    const changePasswordForm = document.getElementById("changePasswordForm");
    const changePasswordAlert = document.getElementById("changePasswordAlert");
    const btnSubmitChangePassword = document.getElementById("btnSubmitChangePassword");

    if (changePasswordForm) {
        changePasswordForm.addEventListener("submit", (e) => {
            e.preventDefault();
            
            const oldPassword = document.getElementById("oldPasswordInput").value.trim();
            const newPassword = document.getElementById("newPasswordInput").value.trim();
            const confirmPassword = document.getElementById("confirmPasswordInput").value.trim();

            changePasswordAlert.classList.add("d-none");
            changePasswordAlert.textContent = "";

            if (!oldPassword || !newPassword || !confirmPassword) {
                changePasswordAlert.textContent = "All password fields are required.";
                changePasswordAlert.classList.remove("d-none");
                return;
            }

            if (newPassword.length < 6) {
                changePasswordAlert.textContent = "New password must be at least 6 characters long.";
                changePasswordAlert.classList.remove("d-none");
                return;
            }

            if (newPassword !== confirmPassword) {
                changePasswordAlert.textContent = "New passwords do not match.";
                changePasswordAlert.classList.remove("d-none");
                return;
            }

            btnSubmitChangePassword.disabled = true;
            btnSubmitChangePassword.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i>Updating Password...';

            const formData = new URLSearchParams();
            formData.append("oldPassword", oldPassword);
            formData.append("newPassword", newPassword);

            fetch('/settings/change-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: formData.toString()
            })
            .then(res => res.json().then(data => ({ status: res.status, body: data })))
            .then(res => {
                btnSubmitChangePassword.disabled = false;
                btnSubmitChangePassword.innerHTML = '<i class="fa fa-save me-2"></i>Update Password';

                if (res.body.success) {
                    changePasswordForm.reset();
                    const modalEl = document.getElementById('changePasswordModal');
                    const modal = bootstrap.Modal.getInstance(modalEl);
                    if (modal) modal.hide();

                    if (typeof CanineUI !== "undefined") {
                        CanineUI.showToast("Account password updated successfully!", "success");
                    } else {
                        alert("Account password updated successfully!");
                    }
                } else {
                    changePasswordAlert.textContent = res.body.message || "Failed to update password.";
                    changePasswordAlert.classList.remove("d-none");
                }
            })
            .catch(err => {
                btnSubmitChangePassword.disabled = false;
                btnSubmitChangePassword.innerHTML = '<i class="fa fa-save me-2"></i>Update Password';
                changePasswordAlert.textContent = "An error occurred while communicating with the server.";
                changePasswordAlert.classList.remove("d-none");
            });
        });
    }
});
