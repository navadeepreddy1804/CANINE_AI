/**
 * CanineAI Shared JavaScript UI Helpers
 * Integrates directly with the Thymeleaf fragment components.
 */

const CanineUI = (() => {
    // Private Helpers
    const getThemeKey = () => {
        const storedTheme = localStorage.getItem('canineai-theme');
        return storedTheme === 'dark' || storedTheme === 'light' ? storedTheme : 'light';
    };

    const applyTheme = (themeName, saveToStorage = false) => {
        const normalizedTheme = themeName === 'dark' ? 'dark' : 'light';
        document.documentElement.classList.toggle('dark-theme', normalizedTheme === 'dark');
        document.documentElement.style.colorScheme = normalizedTheme;
        document.documentElement.dataset.theme = normalizedTheme;
        if (saveToStorage) {
            localStorage.setItem('canineai-theme', normalizedTheme);
        }

        const sw = document.getElementById('darkModeSwitch');
        if (sw) {
            sw.checked = normalizedTheme === 'dark';
        }
    };

    return {
        /**
         * Initialize application themes on boot without modifying localStorage.
         */
        initTheme: () => {
            const currentTheme = getThemeKey();
            applyTheme(currentTheme, false);
        },

        /**
         * Explicitly set light or dark theme mode and persist selection.
         */
        setTheme: (themeName) => {
            applyTheme(themeName, true);
        },

        /**
         * Toggle light and dark theme mode and persist selection.
         */
        toggleTheme: () => {
            const isDark = getThemeKey() === 'dark';
            applyTheme(isDark ? 'light' : 'dark', true);
        },

        /**
         * Display a snackbar/toast message.
         * @param {string} message - Message text.
         * @param {string} type - 'success', 'warning', 'error', or 'info'.
         * @param {number} duration - Autohide duration in milliseconds.
         */
        showToast: (message, type = 'info', duration = 4000) => {
            let container = document.getElementById('canine-toast-container');
            if (!container) {
                container = document.createElement('div');
                container.id = 'canine-toast-container';
                container.style.position = 'fixed';
                container.style.bottom = '24px';
                container.style.right = '24px';
                container.style.zIndex = '9999';
                container.style.display = 'flex';
                container.style.flexDirection = 'column';
                container.style.gap = '8px';
                document.body.appendChild(container);
            }

            const toast = document.createElement('div');
            toast.className = `status-badge status-${type}`;
            toast.style.padding = '12px 20px';
            toast.style.borderRadius = '8px';
            toast.style.boxShadow = '0 10px 15px -3px rgba(0, 0, 0, 0.1)';
            toast.style.minWidth = '240px';
            toast.style.display = 'flex';
            toast.style.justifyContent = 'space-between';
            toast.style.alignItems = 'center';
            toast.style.fontSize = '13px';
            toast.style.textTransform = 'none';
            toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
            toast.style.transform = 'translateY(20px)';
            toast.style.opacity = '0';

            toast.innerHTML = `
                <span>${message}</span>
                <span style="margin-left: 16px; cursor: pointer; font-weight: bold;" onclick="this.parentElement.remove()">×</span>
            `;

            container.appendChild(toast);

            // Trigger Animation
            setTimeout(() => {
                toast.style.transform = 'translateY(0)';
                toast.style.opacity = '1';
            }, 10);

            // Auto Remove
            setTimeout(() => {
                toast.style.transform = 'translateY(20px)';
                toast.style.opacity = '0';
                setTimeout(() => {
                    toast.remove();
                }, 300);
            }, duration);
        },

        /**
         * Show an overlay loader covering the viewport during processing blocks.
         */
        showLoadingOverlay: () => {
            let overlay = document.getElementById('canine-loading-overlay');
            if (!overlay) {
                overlay = document.createElement('div');
                overlay.id = 'canine-loading-overlay';
                overlay.style.position = 'fixed';
                overlay.style.top = '0';
                overlay.style.left = '0';
                overlay.style.width = '100vw';
                overlay.style.height = '100vh';
                overlay.style.backgroundColor = 'rgba(248, 250, 252, 0.7)';
                overlay.style.backdropFilter = 'blur(4px)';
                overlay.style.display = 'flex';
                overlay.style.justifyContent = 'center';
                overlay.style.alignItems = 'center';
                overlay.style.zIndex = '10000';
                overlay.innerHTML = '<div class="spinner-loader"></div>';
                document.body.appendChild(overlay);
            }
        },

        /**
         * Hide the viewport processing loading overlay.
         */
        hideLoadingOverlay: () => {
            const overlay = document.getElementById('canine-loading-overlay');
            if (overlay) {
                overlay.remove();
            }
        }
    };
})();

// Automatically initialize theme & collapsible sidebar on loading scripts
document.addEventListener('DOMContentLoaded', () => {
    CanineUI.initTheme();

    // Collapsible Sidebar Session Restore
    const isCollapsed = sessionStorage.getItem("sidebar-collapsed") === "true";
    if (isCollapsed) {
        document.body.classList.add("sidebar-collapsed");
    }

    // Register Toggler Button Click Event
    const toggleBtn = document.getElementById("sidebar-toggle-btn");
    if (toggleBtn) {
        toggleBtn.addEventListener("click", () => {
            document.body.classList.toggle("sidebar-collapsed");
            const collapsed = document.body.classList.contains("sidebar-collapsed");
            sessionStorage.setItem("sidebar-collapsed", collapsed);
        });
    }
});

