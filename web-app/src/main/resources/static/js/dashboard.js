/**
 * CanineAI Web Dashboard Interactions Module
 */

document.addEventListener("DOMContentLoaded", () => {
    // Format local country date format dynamically
    const dateEl = document.getElementById("dashboard-today-date");
    if (dateEl) {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        dateEl.textContent = new Date().toLocaleDateString(undefined, options);
    }

    // Quick action callbacks redirection preparations
    const actions = {
        "action-new-patient": "/patients",
        "action-upload-cbct": "/upload",
        "action-gen-report": "/reports",
        "action-view-history": "/history"
    };

    Object.keys(actions).forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener("click", () => {
                window.location.href = actions[id];
            });
        }
    });

    const searchForm = document.getElementById("dashboard-search-form");
    if (searchForm) {
        searchForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const query = document.getElementById("dashboard-search-input").value.trim();
            if (query !== "") {
                window.location.href = '/patients?search=' + encodeURIComponent(query);
            }
        });
    }
});
