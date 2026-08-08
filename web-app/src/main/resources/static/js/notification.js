/**
 * CanineAI Web Notification Workspace Script
 */

document.addEventListener("DOMContentLoaded", () => {
    // Audit log advanced filters drawer toggles
    const timelineFilterToggle = document.getElementById("timeline-filter-toggle");
    const timelineFilterDrawer = document.getElementById("timeline-filter-drawer");

    if (timelineFilterToggle && timelineFilterDrawer) {
        timelineFilterToggle.addEventListener("click", () => {
            timelineFilterDrawer.classList.toggle("d-none");
        });
    }

    // Dismiss notifications actions
    const notifyRows = document.querySelectorAll(".notification-log-row");
    notifyRows.forEach(row => {
        const dismissBtn = row.querySelector(".dismiss-notification-btn");
        if (dismissBtn) {
            dismissBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                row.style.opacity = "0.5";
                setTimeout(() => {
                    row.remove();
                    if (typeof CanineUI !== "undefined") {
                        CanineUI.showToast("Notification cleared.", "info");
                    }
                }, 400);
            });
        }

        // Mark read on click
        row.addEventListener("click", () => {
            const dot = row.querySelector(".unread-notify-dot");
            if (dot) {
                dot.remove();
                if (typeof CanineUI !== "undefined") {
                    CanineUI.showToast("Notification marked as read.", "success");
                }
            }
        });
    });
});
