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
});
