/**
 * CanineAI Web Splash Screen Sequence Control
 */

document.addEventListener("DOMContentLoaded", () => {
    const loadingText = document.getElementById("splash-loading-text");
    const progressBarFill = document.getElementById("splash-progress-fill");

    const totalDuration = 2500; // 2.5 seconds
    const intervalTime = 50; 
    const steps = totalDuration / intervalTime;
    let currentStep = 0;

    const messages = [
        { threshold: 0.25, text: "Initializing AI Engine..." },
        { threshold: 0.50, text: "Loading Security..." },
        { threshold: 0.75, text: "Preparing Platform..." },
        { threshold: 1.00, text: "Almost Ready..." }
    ];

    const timer = setInterval(() => {
        currentStep++;
        const progressFraction = currentStep / steps;
        const progressPercentage = Math.min(progressFraction * 100, 100);

        // Update progress bar width
        if (progressBarFill) {
            progressBarFill.style.width = `${progressPercentage}%`;
        }

        // Cycle through messaging labels
        const currentMessage = messages.find(m => progressFraction <= m.threshold);
        if (currentMessage && loadingText) {
            loadingText.textContent = currentMessage.text;
        }

        // Sequence completion action
        if (currentStep >= steps) {
            clearInterval(timer);
            completeSplashSequence();
        }
    }, intervalTime);

    function completeSplashSequence() {
        // Authentication is established by the server session; this page does not infer it client-side.
        const isAuthenticated = false;
        
        setTimeout(() => {
            if (isAuthenticated) {
                window.location.href = "/dashboard";
            } else {
                window.location.href = "/login";
            }
        }, 200);
    }
});
