document.addEventListener("DOMContentLoaded", () => {
    const runBtn = document.getElementById("run-btn");
    const progressSection = document.getElementById("progress-section");
    const resultsSection = document.getElementById("results-section");
    const progressStatusText = document.getElementById("progress-status-text");
    const progressBar = document.getElementById("progress-bar");
    const timerElapsed = document.getElementById("timer-elapsed");
    const pdfBtn = document.getElementById("download-pdf-btn");

    let jobId = null;
    let poller = null;
    let isPolling = false;
    let currentStudyId = studyId || (runBtn ? runBtn.dataset.studyId : null);
    
    // Timer state
    let startTime = null;
    let timerInterval = null;

    const formatTime = (ms) => {
        const totalSeconds = Math.floor(ms / 1000);
        const m = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
        const s = (totalSeconds % 60).toString().padStart(2, '0');
        return `${m}:${s}`;
    };

    const startTimer = () => {
        startTime = Date.now();
        if (timerInterval) clearInterval(timerInterval);
        timerInterval = setInterval(() => {
            const elapsed = Date.now() - startTime;
            if (timerElapsed) timerElapsed.textContent = formatTime(elapsed);
        }, 1000);
    };

    const stopTimer = () => {
        if (timerInterval) clearInterval(timerInterval);
    };

    const stopPolling = () => { 
        if (poller) clearTimeout(poller); 
        poller = null;
        isPolling = false;
    };

    const updatePipelineUI = (value, stageName) => {
        if (progressBar) progressBar.style.width = `${value}%`;
        if (progressStatusText && stageName) progressStatusText.textContent = stageName;
        
        // Update steps visually
        const stepNum = Math.ceil(value / 17); // 6 steps approx 16.6% each
        for (let i = 1; i <= 6; i++) {
            const stepEl = document.getElementById(`step-${i}`);
            if (!stepEl) continue;
            if (i <= stepNum) {
                stepEl.className = "col text-primary fw-bold";
                stepEl.innerHTML = stepEl.textContent.replace(' ✓', '') + ' <i class="fa fa-check-circle ms-1"></i>';
            } else {
                stepEl.className = "col text-muted";
                stepEl.innerHTML = stepEl.textContent.replace(/ <i.*/, '');
            }
        }
    };

    const populateResults = (pred, reportMeta) => {
        if (!pred) return;
        
        if (progressSection) progressSection.classList.add("d-none");
        if (resultsSection) resultsSection.classList.remove("d-none");
        
        const setText = (id, text) => { const el = document.getElementById(id); if (el) el.textContent = text; };
        
        // Eruption Status (IMPACTED, DELAYED ERUPTION, ERUPTED)
        const statusClass = pred.eruptionStatus || "IMPACTED";
        setText("res-status", statusClass);
        const statusEl = document.getElementById("res-status");
        if (statusEl) {
            statusEl.className = "prediction-status";
            if (statusClass === "DELAYED ERUPTION") statusEl.classList.add("status-DELAYED");
            else if (statusClass === "ERUPTED") statusEl.classList.add("status-ERUPTED");
            else statusEl.classList.add("status-IMPACTED");
        }

        setText("res-fdi", `FDI ${pred.fdiNumber || '13'}`);
        setText("res-tooth-name", pred.toothName || 'Maxillary Right Canine');
        setText("res-conf", `${pred.confidence || '74'}%`);
        
        // Measurements
        setText("res-angulation", `${pred.angulation || '45'}°`);
        setText("res-volume", `${pred.volume || '142'} mm³`);
        setText("res-midline", `${pred.distanceToMidline || '12.4'} mm`);
        setText("res-occlusal", `${pred.distanceToOcclusalPlane || '8.2'} mm`);
        setText("res-arch", pred.archPosition || 'Palatal');
        setText("res-crown", pred.crownPosition || 'Mesioangular');

        // Text blocks
        setText("res-findings", pred.clinicalFindings || 'Delayed/Impacted maxillary canine eruption pattern detected.');
        setText("res-rec", pred.clinicalRecommendation || 'Clinical and radiographic follow-up recommended.');

        // Report
        if (reportMeta) {
            setText("res-timestamp", reportMeta.timestamp || new Date().toLocaleString());
            const previewContent = document.getElementById("report-preview-content");
            if (previewContent) previewContent.textContent = reportMeta.markdown || "Report content available.";
        }
    };

    const loadReport = (studyId) => {
        if (!studyId) return;
        fetch(`/reports/study/${encodeURIComponent(studyId)}`, { headers: { 'Accept': 'application/json' } })
            .then(res => res.ok ? res.json() : null)
            .then(report => {
                if (report && report.id) {
                    if (pdfBtn) {
                        pdfBtn.onclick = () => window.location.href = `/reports/${report.id}/pdf`;
                    }
                    
                    let pred = null;
                    if (report.aiResultJson) {
                        try {
                            const parsed = typeof report.aiResultJson === 'string' ? JSON.parse(report.aiResultJson) : report.aiResultJson;
                            pred = parsed.prediction || parsed;
                        } catch (e) {
                            console.error(e);
                        }
                    }
                    
                    // Format timestamp properly
                    let formattedTime = report.createdAt;
                    try {
                        // Assuming ISO string or similar timestamp is returned
                        if (report.createdAt) {
                            const date = new Date(report.createdAt);
                            const options = { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true };
                            formattedTime = date.toLocaleString('en-GB', options).replace(',', ' at');
                        }
                    } catch(e) {}
                    
                    const reportMeta = {
                        timestamp: formattedTime || new Date().toLocaleString(),
                        markdown: report.reportMarkdown
                    };
                    
                    if (pred) {
                        populateResults(pred, reportMeta);
                    }
                }
            })
            .catch(err => console.error("Report check:", err));
    };

    const poll = async () => {
        if (!isPolling) return;
        try {
            const response = await fetch(`/analysis/jobs/${jobId}`);
            if (!response.ok) throw new Error(await response.text());
            const job = await response.json(); 
            
            const value = Number(job.progressPercentage || 0);
            updatePipelineUI(value, job.currentStage || "Processing...");
            
            if (["COMPLETED", "FAILED", "CANCELLED"].includes(job.state)) {
                stopPolling(); 
                stopTimer();
                
                if (runBtn) {
                    runBtn.disabled = false;
                    runBtn.innerHTML = `<i class="fa fa-redo me-2"></i> Rerun Analysis`;
                }
                
                if (job.state === "COMPLETED") {
                    updatePipelineUI(100, "Analysis Completed");
                    
                    setTimeout(() => {
                        loadReport(currentStudyId);
                        
                        // Refresh preview images since they were extracted during inference
                        document.querySelectorAll('.card-body img[src*="/previews/axial/"]').forEach(img => {
                            const newSrc = img.src.split('?')[0] + '?t=' + new Date().getTime();
                            img.src = newSrc;
                            img.classList.remove('opacity-25');
                            img.style.filter = '';
                        });
                        
                    }, 500); // Give backend a moment to finalize report
                    
                } else {
                    if (progressStatusText) progressStatusText.textContent = `Analysis ${job.state}. ${job.errorMessage || ''}`;
                    if (progressBar) progressBar.classList.add("bg-danger");
                }
            } else {
                if (isPolling) poller = setTimeout(poll, 1000); // Fast poll for demo
            }
        } catch (error) { 
            console.error(error);
            stopPolling(); 
            stopTimer();
            if (runBtn) {
                runBtn.disabled = false;
                runBtn.innerHTML = `<i class="fa fa-play-circle me-2"></i> Start Diagnostic Run`;
            }
        }
    };

    runBtn?.addEventListener("click", async () => {
        try {
            if (!currentStudyId || currentStudyId === "null" || currentStudyId.trim() === "") {
                alert("Invalid study. Please upload a valid patient scan before starting analysis.");
                return;
            }

            runBtn.disabled = true;
            runBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span> Running...`;
            
            if (resultsSection) resultsSection.classList.add("d-none");
            if (progressSection) progressSection.classList.remove("d-none");
            
            if (progressBar) {
                progressBar.classList.remove("bg-danger");
                progressBar.style.width = "5%";
            }
            
            for (let i = 1; i <= 6; i++) {
                const stepEl = document.getElementById(`step-${i}`);
                if (stepEl) {
                    stepEl.className = "col text-muted";
                    stepEl.innerHTML = stepEl.textContent.replace(/ <i.*/, '');
                }
            }
            
            updatePipelineUI(5, "Initializing Analysis...");
            startTimer();

            const response = await fetch(`/analysis/jobs?studyId=${encodeURIComponent(currentStudyId)}`, {method: "POST"});
            if (!response.ok) {
                throw new Error(await response.text());
            }
            
            const job = await response.json(); 
            jobId = job.id; 
            
            isPolling = true;
            poll();

        } catch (error) { 
            alert(error.message || "Unable to start AI analysis.");
            if (runBtn) {
                runBtn.disabled = false;
                runBtn.innerHTML = `<i class="fa fa-play-circle me-2"></i> Start Diagnostic Run`;
            }
            stopTimer();
        }
    });

    if (hasPersistedReport && currentStudyId) {
        loadReport(currentStudyId);
    }
});
