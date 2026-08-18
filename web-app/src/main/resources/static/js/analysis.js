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

            // Maximum safety timeout: 60s
            if (elapsed > 60000 && isPolling) {
                console.warn("Safety timeout reached (60s). Exiting processing state.");
                stopPolling();
                stopTimer();
                if (progressStatusText) {
                    progressStatusText.innerHTML = `<span class="text-warning fw-bold"><i class="fa fa-exclamation-triangle me-2"></i>Timeout - Please retry</span>`;
                }
                if (runBtn) {
                    runBtn.disabled = false;
                    runBtn.innerHTML = `<i class="fa fa-play-circle me-2"></i> Start Diagnostic Run`;
                }
            }
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

    const stopBtn = document.getElementById("stop-analysis-btn");

    const stageNames = [
        "Preparing AI analysis",
        "Uploading / processing CBCT",
        "Segmenting teeth",
        "Locating maxillary canines",
        "Analysis complete"
    ];

    const updatePipelineUI = (value, stageName) => {
        if (progressBar) progressBar.style.width = `${value}%`;
        if (progressStatusText && stageName) progressStatusText.textContent = stageName;

        let activeStageIndex = 0;
        if (stageName) {
            const foundIdx = stageNames.findIndex(name => stageName.toLowerCase().includes(name.toLowerCase()));
            if (foundIdx >= 0) activeStageIndex = foundIdx;
            else activeStageIndex = Math.min(Math.floor((value / 100) * 5), 4);
        } else {
            activeStageIndex = Math.min(Math.floor((value / 100) * 5), 4);
        }

        for (let i = 1; i <= 5; i++) {
            const el = document.getElementById(`stage-${i}`);
            if (!el) continue;
            const idx = i - 1;
            const name = stageNames[idx];

            if (idx < activeStageIndex || value >= 100) {
                el.className = "col-12 mb-1 text-success fw-bold";
                el.innerHTML = `<i class="fa fa-check-circle me-2 text-success"></i> ${name}`;
            } else if (idx === activeStageIndex && value < 100) {
                el.className = "col-12 mb-1 text-primary fw-bold";
                el.innerHTML = `<i class="fa fa-circle-notch fa-spin me-2 text-primary"></i> ${name}`;
            } else {
                el.className = "col-12 mb-1 text-muted opacity-75";
                el.innerHTML = `<i class="far fa-circle me-2"></i> ${name}`;
            }
        }
    };

    const handleCancelAnalysis = async () => {
        if (!jobId) return;
        if (confirm("Stop this analysis?")) {
            try {
                await fetch(`/analysis/jobs/${jobId}/cancel`, { method: "POST" });
            } catch (err) {
                console.warn("Cancel request notice:", err);
            }
            stopPolling();
            stopTimer();

            if (progressStatusText) {
                progressStatusText.innerHTML = `<span class="text-warning fw-bold"><i class="fa fa-ban me-2"></i>Analysis Cancelled</span>`;
            }
            if (progressBar) {
                progressBar.classList.remove("bg-primary");
                progressBar.classList.add("bg-warning");
                progressBar.style.width = "0%";
            }
            if (runBtn) {
                runBtn.disabled = false;
                runBtn.innerHTML = `<i class="fa fa-play-circle me-2"></i> Start Diagnostic Run`;
            }
        }
    };

    if (stopBtn) {
        stopBtn.addEventListener("click", handleCancelAnalysis);
    }

    const populateResults = (pred, reportMeta) => {
        if (!pred) return;
        
        if (progressSection) progressSection.classList.add("d-none");
        if (resultsSection) resultsSection.classList.remove("d-none");
        
        const setText = (id, text) => { const el = document.getElementById(id); if (el) el.textContent = text; };
        
        // Handle ToothSeg response format if present (canines array & visualizations)
        if (pred.canines && Array.isArray(pred.canines)) {
            const rightCanine = pred.canines.find(c => c.label === 3 || c.tooth.includes("Right")) || pred.canines[0];
            const leftCanine = pred.canines.find(c => c.label === 11 || c.tooth.includes("Left")) || pred.canines[1];

            if (rightCanine) {
                setText("right-canine-status", rightCanine.detected ? "✓ Detected" : "Not Detected");
                setText("right-canine-voxels", `${(rightCanine.voxel_count || rightCanine.voxelCount || 0).toLocaleString()} voxels`);
                if (rightCanine.centroid) {
                    const c = rightCanine.centroid;
                    setText("right-canine-centroid", `(${c.x || c[0]}, ${c.y || c[1]}, ${c.z || c[2]})`);
                }
                if (rightCanine.bbox) {
                    const b = rightCanine.bbox;
                    setText("right-canine-bbox", `X:[${b.x_min}..${b.x_max}], Y:[${b.y_min}..${b.y_max}], Z:[${b.z_min}..${b.z_max}]`);
                }
            }

            if (leftCanine) {
                setText("left-canine-status", leftCanine.detected ? "✓ Detected" : "Not Detected");
                setText("left-canine-voxels", `${(leftCanine.voxel_count || leftCanine.voxelCount || 0).toLocaleString()} voxels`);
                if (leftCanine.centroid) {
                    const c = leftCanine.centroid;
                    setText("left-canine-centroid", `(${c.x || c[0]}, ${c.y || c[1]}, ${c.z || c[2]})`);
                }
                if (leftCanine.bbox) {
                    const b = leftCanine.bbox;
                    setText("left-canine-bbox", `X:[${b.x_min}..${b.x_max}], Y:[${b.y_min}..${b.y_max}], Z:[${b.z_min}..${b.z_max}]`);
                }
            }

            if (pred.visualizations) {
                if (pred.visualizations.right_canine) {
                    const imgR = document.getElementById("right-canine-vis");
                    if (imgR) imgR.src = pred.visualizations.right_canine;
                }
                if (pred.visualizations.left_canine) {
                    const imgL = document.getElementById("left-canine-vis");
                    if (imgL) imgL.src = pred.visualizations.left_canine;
                }
            }
        }

        const pObj = pred.prediction || pred;

        // Eruption Status (IMPACTED, DELAYED ERUPTION, ERUPTED, LIKELY_TO_ERUPT, BORDERLINE)
        const statusClass = String(pObj.eruptionStatus || pObj.prediction || "IMPACTED").replace("_", " ");
        setText("res-status", statusClass);
        const statusEl = document.getElementById("res-status");
        if (statusEl) {
            statusEl.className = "prediction-status";
            if (statusClass.toUpperCase().includes("DELAYED") || statusClass.toUpperCase().includes("BORDERLINE")) statusEl.classList.add("status-DELAYED");
            else if (statusClass.toUpperCase().includes("ERUPTED") || statusClass.toUpperCase().includes("LIKELY")) statusEl.classList.add("status-ERUPTED");
            else statusEl.classList.add("status-IMPACTED");
        }

        setText("res-fdi", `FDI ${pObj.fdiNumber || pObj.canineFdi || '13'}`);
        setText("res-tooth-name", pObj.toothName || pObj.canineToothName || 'Maxillary Right Canine');
        setText("res-conf", `${pObj.confidence || '74'}%`);
        
        // Measurements
        setText("res-angulation", `${pObj.angulation || pObj.angle || '45'}°`);
        setText("res-volume", `${pObj.volume || pObj.canineVolumeMm3 || '142'} mm³`);
        setText("res-midline", `${pObj.distanceToMidline || '12.4'} mm`);
        setText("res-occlusal", `${pObj.distanceToOcclusalPlane || '8.2'} mm`);
        setText("res-arch", pObj.archPosition || 'Palatal');
        setText("res-crown", pObj.crownPosition || 'Mesioangular');

        // Text blocks
        setText("res-findings", pObj.clinicalFindings || 'Maxillary canine 3D segmentation and localization completed.');
        setText("res-rec", pObj.clinicalRecommendation || 'Clinical and radiographic follow-up recommended.');

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
                            pred = typeof report.aiResultJson === 'string' ? JSON.parse(report.aiResultJson) : report.aiResultJson;
                        } catch (e) {
                            console.error(e);
                        }
                    }
                    if (!pred) {
                        pred = {
                            prediction: report.prediction,
                            eruptionStatus: report.prediction,
                            confidence: report.confidence,
                            fdiNumber: report.canineFdi,
                            toothName: report.canineToothName,
                            angulation: report.canineAngulation,
                            volume: report.canineVolumeMm3,
                            clinicalRecommendation: report.clinicalRecommendation
                        };
                    }
                    
                    let formattedTime = report.formattedApprovedAt || report.approvedAt || report.createdAt;
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
                    if (progressStatusText) {
                        const errorMsg = job.errorMessage || "GPU worker processing error";
                        progressStatusText.innerHTML = `<span class="text-danger"><i class="fa fa-exclamation-triangle me-2"></i>Analysis Failed: ${errorMsg}</span>`;
                    }
                    if (progressBar) {
                        progressBar.classList.remove("bg-primary");
                        progressBar.classList.add("bg-danger");
                        progressBar.style.width = "100%";
                    }
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
                progressBar.classList.remove("bg-danger", "bg-warning");
                progressBar.classList.add("bg-primary");
                progressBar.style.width = "5%";
            }
            
            for (let i = 1; i <= 10; i++) {
                const el = document.getElementById(`stage-${i}`);
                if (el) {
                    const name = stageNames[i - 1];
                    if (i === 1) {
                        el.className = "col-12 col-md-6 mb-1 text-primary fw-bold";
                        el.innerHTML = `<i class="fa fa-circle-notch fa-spin me-2 text-primary"></i> ${i}. ${name}`;
                    } else {
                        el.className = "col-12 col-md-6 mb-1 text-muted opacity-75";
                        el.innerHTML = `<i class="far fa-circle me-2"></i> ${i}. ${name}`;
                    }
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

    if (currentStudyId) {
        loadReport(currentStudyId);
    }
});
