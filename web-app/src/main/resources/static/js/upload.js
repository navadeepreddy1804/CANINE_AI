/**
 * CanineAI DICOM Upload & Clinical Workspace controller script
 * Implements rigid patient context validation, multi-format extension check,
 * sequential progress milestones, database synchronization, and DICOM slice viewer binds.
 */

document.addEventListener("DOMContentLoaded", () => {
    // UI Elements
    const dropzone = document.getElementById("dicom-dropzone");
    const fileInput = document.getElementById("file-input");
    const folderInput = document.getElementById("folder-input");
    const fileSelectBtn = document.getElementById("file-select-btn");
    const folderSelectBtn = document.getElementById("folder-select-btn");
    
    const uploadQueue = document.getElementById("upload-queue");
    const progressFill = document.getElementById("upload-progress-fill");
    const progressPercent = document.getElementById("upload-progress-percent");
    const queueFileName = document.getElementById("queue-file-name");
    
    const workspaceContainer = document.getElementById("workspace-container");
    const proceedBtn = document.getElementById("workspace-proceed-btn");
    const patientSelect = document.getElementById("pacs-patient-select");
    const viewerMainImage = document.getElementById("viewer-main-image");
    const viewerSliceLabel = document.getElementById("viewer-slice-label");
    const viewerSliceSlider = document.getElementById("viewer-slice-slider");
    const viewerPrevBtn = document.getElementById("viewer-prev-btn");
    const viewerNextBtn = document.getElementById("viewer-next-btn");
    const viewerHint = document.getElementById("slice-viewer-hint");
    
    // Milestones Elements
    const milestoneUpload = document.getElementById("milestone-upload");
    const iconMilestoneUpload = document.getElementById("icon-milestone-upload");
    const badgeMilestoneUpload = document.getElementById("badge-milestone-upload");

    const milestonePreprocess = document.getElementById("milestone-preprocess");
    const iconMilestonePreprocess = document.getElementById("icon-milestone-preprocess");
    const badgeMilestonePreprocess = document.getElementById("badge-milestone-preprocess");

    const milestonePrepare = document.getElementById("milestone-prepare");
    const iconMilestonePrepare = document.getElementById("icon-milestone-prepare");
    const badgeMilestonePrepare = document.getElementById("badge-milestone-prepare");

    let currentViewerSlice = 0;
    let currentViewerStudyId = null;
    let viewerSliceCount = 12;
    let currentPatientId = null;
    let workspaceRefreshAttempts = 0;
    let workspaceReady = false;
    let previewLoadInProgress = false;
    let previewLoadStarted = false;

    // Retrieve active patient ID from dropdown selection
    if (patientSelect) {
        currentPatientId = patientSelect.value;
        
        patientSelect.addEventListener("change", (e) => {
            const val = e.target.value;
            if (val) {
                window.location.href = `/upload?patientId=${val}`;
            } else {
                window.location.href = `/upload`;
            }
        });
    }

    // Bind file choose triggers
    if (fileSelectBtn && fileInput) {
        fileSelectBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            fileInput.click();
        });
    }

    if (folderSelectBtn && folderInput) {
        folderSelectBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            folderInput.click();
        });
    }

    // Drag-and-drop binds
    if (dropzone) {
        dropzone.addEventListener("dragover", (e) => {
            e.preventDefault();
            if (currentPatientId) {
                dropzone.classList.add("dragover");
            }
        });

        dropzone.addEventListener("dragleave", () => {
            dropzone.classList.remove("dragover");
        });

        dropzone.addEventListener("drop", (e) => {
            e.preventDefault();
            dropzone.classList.remove("dragover");
            if (!currentPatientId) return;
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                validateAndUpload(files);
            }
        });

        dropzone.addEventListener("click", () => {
            if (currentPatientId && fileInput) {
                fileInput.click();
            }
        });
    }

    if (fileInput) {
        fileInput.addEventListener("change", () => {
            if (fileInput.files.length > 0) {
                validateAndUpload(fileInput.files);
            }
        });
    }

    if (folderInput) {
        folderInput.addEventListener("change", () => {
            if (folderInput.files.length > 0) {
                validateAndUpload(folderInput.files);
            }
        });
    }

    // Robust file validation check
    function validateAndUpload(files) {
        if (!currentPatientId) {
            showNotification("No patient selected. Please connect an EMR profile first.", "error");
            return;
        }

        const allowedExtensions = ['.zip', '.nii', '.nii.gz', '.dcm', '.mha', '.mhd', '.nrrd'];
        const validFiles = [];
        const invalidFiles = [];

        Array.from(files).forEach(file => {
            const nameLower = file.name.toLowerCase();
            const isValid = allowedExtensions.some(ext => nameLower.endsWith(ext));
            if (isValid) {
                validFiles.push(file);
            } else {
                invalidFiles.push(file);
            }
        });

        if (invalidFiles.length > 0) {
            showNotification(`Rejected ${invalidFiles.length} unsupported files. Supported: .DCM, .ZIP, .NII, .NII.GZ`, "error");
        }

        if (validFiles.length > 0) {
            uploadFilesToServer(validFiles);
        }
    }

    // Multi-format sequential upload logic
    async function uploadFilesToServer(files) {
        uploadQueue.classList.remove("d-none");
        progressFill.style.width = "0%";
        progressPercent.textContent = "Uploading: 0%";
        
        // Reset Milestone states
        setMilestoneState("upload", "ACTIVE");
        setMilestoneState("preprocess", "PENDING");
        setMilestoneState("prepare", "PENDING");

        const isSinglePackage = files.length === 1 && (
            files[0].name.toLowerCase().endsWith(".zip") ||
            files[0].name.toLowerCase().endsWith(".nii") ||
            files[0].name.toLowerCase().endsWith(".nii.gz") ||
            files[0].name.toLowerCase().endsWith(".mha") ||
            files[0].name.toLowerCase().endsWith(".mhd") ||
            files[0].name.toLowerCase().endsWith(".nrrd")
        );

        if (isSinglePackage) {
            const file = files[0];
            queueFileName.textContent = `${file.name} (${(file.size / (1024 * 1024)).toFixed(1)} MB)`;

            const formData = new FormData();
            formData.append("patientId", currentPatientId);
            formData.append("file", file);

            const xhr = new XMLHttpRequest();
            xhr.open("POST", "/upload/file", true);

            xhr.upload.addEventListener("progress", (e) => {
                if (e.lengthComputable) {
                    const percent = Math.round((e.loaded / e.total) * 100);
                    progressFill.style.width = `${percent}%`;
                    progressPercent.textContent = `Uploading: ${percent}%`;
                }
            });

            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        try {
                            const res = JSON.parse(xhr.responseText);
                            if (res.success && res.session) {
                                setMilestoneState("upload", "COMPLETED");
                                pollUploadSessionStatus(res.session.id);
                            } else {
                                handleUploadFailure(res.message || "Failed backend initialize");
                            }
                        } catch (ex) {
                            handleUploadFailure("JSON parsing failure on response payload");
                        }
                    } else {
                        handleUploadFailure(`Upload failed. Code: ${xhr.status}`);
                    }
                }
            };

            xhr.send(formData);
        } else {
            // Folder / Multiple slices upload
            queueFileName.textContent = `DICOM Folder Study (${files.length} frames)`;
            progressPercent.textContent = "Initializing upload socket...";
            
            const totalSize = files.reduce((sum, f) => sum + f.size, 0);

            try {
                const initRes = await fetch(`/upload/initialize?patientId=${currentPatientId}&totalSize=${totalSize}&totalFiles=${files.length}`, {
                    method: 'POST'
                });
                
                if (!initRes.ok) throw new Error("Upload hand-shake refused.");
                const session = await initRes.json();
                const sessionId = session.id;

                let bytesUploaded = 0;
                setMilestoneState("upload", "ACTIVE");

                for (let i = 0; i < files.length; i++) {
                    const file = files[i];
                    const chunkData = new FormData();
                    chunkData.append("sessionId", sessionId);
                    chunkData.append("fileName", file.name);
                    chunkData.append("file", file);

                    progressPercent.textContent = `Uploading slice ${i+1}/${files.length}`;

                    const chunkRes = await fetch('/upload/chunk', {
                        method: 'POST',
                        body: chunkData
                    });

                    if (!chunkRes.ok) throw new Error(`Chunk upload failed: ${file.name}`);

                    bytesUploaded += file.size;
                    const percent = Math.round((bytesUploaded / totalSize) * 100);
                    progressFill.style.width = `${percent}%`;
                }

                setMilestoneState("upload", "COMPLETED");
                pollUploadSessionStatus(sessionId);

            } catch (err) {
                handleUploadFailure(err.message);
            }
        }
    }

    function handleUploadFailure(msg) {
        showNotification(`Pipeline failure: ${msg}`, "error");
        uploadQueue.classList.add("d-none");
        setMilestoneState("upload", "PENDING");
        setMilestoneState("preprocess", "PENDING");
        setMilestoneState("prepare", "PENDING");
    }

    // Milestones Status Updates
    function setMilestoneState(milestone, state) {
        let el, icon, badge;
        if (milestone === "upload") {
            el = milestoneUpload; icon = iconMilestoneUpload; badge = badgeMilestoneUpload;
        } else if (milestone === "preprocess") {
            el = milestonePreprocess; icon = iconMilestonePreprocess; badge = badgeMilestonePreprocess;
        } else if (milestone === "prepare") {
            el = milestonePrepare; icon = iconMilestonePrepare; badge = badgeMilestonePrepare;
        }

        if (!el || !icon || !badge) return;

        el.className = "d-flex align-items-center justify-content-between small";
        if (state === "PENDING") {
            el.classList.add("text-muted");
            icon.className = "fa fa-circle me-1.5";
            badge.textContent = "PENDING";
            badge.className = "font-weight-bold text-muted";
        } else if (state === "ACTIVE") {
            el.classList.add("text-primary", "font-weight-bold");
            icon.className = "fa fa-circle-notch fa-spin me-1.5 text-primary";
            badge.textContent = "ACTIVE";
            badge.className = "font-weight-bold text-primary";
        } else if (state === "COMPLETED") {
            el.classList.add("text-success");
            icon.className = "fa fa-check-circle me-1.5 text-success";
            badge.textContent = "COMPLETED";
            badge.className = "font-weight-bold text-success";
        }
    }

    // Status polling
    function pollUploadSessionStatus(sessionId) {
        const checkInterval = 1000;
        
        const poll = setInterval(async () => {
            try {
                const res = await fetch(`/upload/status/${sessionId}`);
                if (!res.ok) {
                    clearInterval(poll);
                    const text = await res.text();
                    handleUploadFailure(`Upload status request failed: ${res.status} ${text}`);
                    return;
                }

                const session = await res.json();
                const status = session.status;

                if (status === "UPLOADING") {
                    setMilestoneState("upload", "ACTIVE");
                    progressPercent.textContent = "Transferring clinical bytes...";
                } else if (status === "VALIDATING") {
                    setMilestoneState("upload", "COMPLETED");
                    setMilestoneState("preprocess", "ACTIVE");
                    progressPercent.textContent = "Extracting CBCT volumes and tags...";
                } else if (status === "PROCESSING") {
                    setMilestoneState("upload", "COMPLETED");
                    setMilestoneState("preprocess", "COMPLETED");
                    setMilestoneState("prepare", "ACTIVE");
                    progressPercent.textContent = "Unpacking slices and generating preview...";
                } else if (status === "PREVIEW_READY") {
                    setMilestoneState("upload", "COMPLETED");
                    setMilestoneState("preprocess", "COMPLETED");
                    setMilestoneState("prepare", "ACTIVE");
                    progressPercent.textContent = "Preview ready. Finalizing workspace...";

                    if (!previewLoadInProgress) {
                        previewLoadInProgress = true;
                        const loaded = await loadWorkspacePreviews();
                        previewLoadInProgress = false;
                        if (loaded) {
                            clearInterval(poll);
                        }
                    }
                } else if (status === "COMPLETED") {
                    setMilestoneState("upload", "COMPLETED");
                    setMilestoneState("preprocess", "COMPLETED");
                    setMilestoneState("prepare", "COMPLETED");
                    
                    progressPercent.textContent = "Upload completed successfully. Preparing workspace...";
                    progressFill.className = "progress-bar bg-success";
                    clearInterval(poll);

                    // Upload is completed, backend automatically saves study data
                    if (!previewLoadStarted) {
                        previewLoadStarted = true;
                        await loadWorkspacePreviews();
                    }

                } else if (status === "FAILED") {
                    clearInterval(poll);
                    handleUploadFailure(session.errorMessage || "DICOM validation checks rejected study.");
                }
            } catch (e) {
                console.error("Poll status request error:", e);
            }
        }, checkInterval);
    }

    // DICOM Slice Viewer bindings
    function updateSliceViewer(index, studyId) {
        if (!viewerMainImage || !viewerSliceSlider || !viewerSliceLabel) return;
        if (!studyId) return;

        const safeIndex = Math.max(0, Math.min(index, viewerSliceCount - 1));
        currentViewerSlice = safeIndex;
        viewerSliceSlider.value = safeIndex;
        viewerSliceLabel.textContent = `Slice ${safeIndex + 1} of ${viewerSliceCount}`;
        
        if (viewerHint) viewerHint.textContent = `Loading slice frame...`;

        const url = `/studies/${studyId}/previews/axial/${safeIndex}`;
        
        viewerMainImage.onload = () => {
            if (viewerHint) viewerHint.textContent = `Frame ${safeIndex + 1} synchronized`;
            viewerMainImage.style.opacity = "1";
        };

        viewerMainImage.onerror = () => {
            if (viewerHint) viewerHint.textContent = `Frame rendering unavailable.`;
            viewerMainImage.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='512' height='512'%3E%3Crect width='512' height='512' fill='%230f172a'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%230ea5e9' font-size='22' font-family='sans-serif'%3EPreview Slice Frame%3C/text%3E%3C/svg%3E";
        };

        viewerMainImage.src = url;
    }

    if (viewerSliceSlider) {
        viewerSliceSlider.addEventListener("input", (e) => {
            if (currentViewerStudyId) {
                updateSliceViewer(Number(e.target.value), currentViewerStudyId);
            }
        });
    }

    if (viewerPrevBtn) {
        viewerPrevBtn.addEventListener("click", () => {
            if (currentViewerStudyId) {
                updateSliceViewer(Math.max(0, currentViewerSlice - 1), currentViewerStudyId);
            }
        });
    }

    if (viewerNextBtn) {
        viewerNextBtn.addEventListener("click", () => {
            if (currentViewerStudyId) {
                updateSliceViewer(Math.min(viewerSliceCount - 1, currentViewerSlice + 1), currentViewerStudyId);
            }
        });
    }

    // Synchronize current study detail previews on load
    async function loadWorkspacePreviews() {
        if (!currentPatientId) return;

        try {
            const res = await fetch(`/upload/studies/${currentPatientId}`);
            if (!res.ok) throw new Error(`Study lookup failed: ${res.status}`);

            const studies = await res.json();
            if (!Array.isArray(studies) || studies.length === 0) {
                workspaceRefreshAttempts += 1;
                if (workspaceRefreshAttempts <= 20) {
                    setTimeout(() => {
                        loadWorkspacePreviews();
                    }, 1500);
                    return;
                }

                if (viewerHint) viewerHint.textContent = "Study workspace is still syncing. Please wait a moment and refresh the page in a few seconds.";
                setTimeout(() => {
                    loadWorkspacePreviews();
                }, 5000);
                return;
            }

            workspaceRefreshAttempts = 0;
            studies.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
            const latestStudy = studies[0];
            const studyId = latestStudy.id;
            currentViewerStudyId = studyId;

            const previewCheck = await fetch(`/studies/${studyId}/previews/axial/6`, { cache: "no-store" });
            if (!previewCheck.ok) {
                throw new Error(`Preview readiness check failed: ${previewCheck.status}`);
            }

            // Load slice preview thumbnails
            for (let i = 0; i < 12; i++) {
                const thumb = document.getElementById(`slice-img-${i}`);
                if (thumb) {
                    thumb.src = `/studies/${studyId}/previews/axial/${i}`;
                    thumb.style.display = "block";
                    if (thumb.parentElement) {
                        thumb.parentElement.style.opacity = "1";
                    }
                }
            }

            updateSliceViewer(6, studyId);

            // Unlock Workspace Panel opacity
            if (workspaceContainer) {
                workspaceContainer.classList.remove("opacity-50");
                workspaceContainer.style.pointerEvents = "auto";
            }

            if (proceedBtn) {
                proceedBtn.removeAttribute("disabled");
                proceedBtn.onclick = () => {
                    window.location.href = `/analysis?patientId=${currentPatientId}&studyId=${studyId}`;
                };
            }

            workspaceReady = true;
            if (viewerHint) {
                viewerHint.textContent = `Study ready for AI analysis (${latestStudy.studyDescription || "CBCT volume"})`;
            }
            showNotification("CBCT workspace ready. You can proceed to AI analysis.", "success");
            return true;

        } catch (ex) {
            console.error("Failed workspace load previews:", ex);
            workspaceReady = false;
            if (viewerHint) {
                viewerHint.textContent = "Preview generation is unavailable right now.";
            }
            if (proceedBtn) {
                proceedBtn.setAttribute("disabled", "disabled");
            }
            showNotification("The workspace is still warming up. Please wait a moment and try again.", "warning");
            return false;
        }
    }

    // Initialize Workspace loads if patient is loaded
    if (currentPatientId && currentPatientId.trim() !== "") {
        loadWorkspacePreviews();
    }

    function showNotification(msg, type) {
        if (typeof CanineUI !== "undefined") {
            CanineUI.showToast(msg, type);
        } else {
            alert(msg);
        }
    }
});
