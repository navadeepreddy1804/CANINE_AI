/**
 * CanineAI Patient EMR Validation, Auto-calculation, & Sorting logic
 */

document.addEventListener("DOMContentLoaded", () => {
    // 1. Toggle filter drawer liveness
    const filterToggleBtn = document.getElementById("patients-filter-toggle");
    const filterDrawer = document.getElementById("patients-filter-drawer");
    if (filterToggleBtn && filterDrawer) {
        filterToggleBtn.addEventListener("click", () => {
            filterDrawer.classList.toggle("d-none");
        });
    }

    // 2. Client-side Real-time Sorting Matrix
    const sortBySelect = document.getElementById("patients-sort-by");
    if (sortBySelect) {
        sortBySelect.addEventListener("change", () => {
            const criteria = sortBySelect.value;
            sortEMRTable(criteria);
        });
    }

    function sortEMRTable(criteria) {
        const table = document.querySelector("table");
        if (!table) return;

        const tbody = table.querySelector("tbody");
        if (!tbody) return;

        const rows = Array.from(tbody.querySelectorAll("tr"));
        if (rows.length === 0 || rows[0].id === "empty-uploads-row") return;

        rows.sort((rowA, rowB) => {
            let valA, valB;

            if (criteria.startsWith("id")) {
                valA = rowA.cells[0].textContent.trim();
                valB = rowB.cells[0].textContent.trim();
                return criteria.endsWith("asc") ? valA.localeCompare(valB) : valB.localeCompare(valA);
            } 
            
            if (criteria.startsWith("name")) {
                valA = rowA.cells[1].querySelector("span").textContent.trim().toLowerCase();
                valB = rowB.cells[1].querySelector("span").textContent.trim().toLowerCase();
                return criteria.endsWith("asc") ? valA.localeCompare(valB) : valB.localeCompare(valA);
            } 
            
            if (criteria.startsWith("age")) {
                // cells[2] contains "42 yrs / Male"
                const textA = rowA.cells[2].textContent.trim().split(" ")[0];
                const textB = rowB.cells[2].textContent.trim().split(" ")[0];
                valA = parseInt(textA) || 0;
                valB = parseInt(textB) || 0;
                return criteria.endsWith("asc") ? valA - valB : valB - valA;
            }

            return 0;
        });

        // Re-append rows in sorted order
        rows.forEach(row => tbody.appendChild(row));
    }

    // 3. Form fields input validation & DOB -> Age auto-calculation
    const patientForm = document.getElementById("patient-form");
    const nameInput = document.getElementById("fullName");
    const ageInput = document.getElementById("age");
    const dobInput = document.getElementById("dob");
    const phoneInput = document.getElementById("phone");
    const emailInput = document.getElementById("email");
    const bloodGroupInput = document.getElementById("bloodGroup");

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    // Enforces internationally accepted digits format with 7 to 15 numbers
    const phoneRegex = /^\+?[0-9\s-]{7,15}$/;

    if (dobInput && ageInput) {
        dobInput.addEventListener("input", () => {
            const dobVal = dobInput.value;
            if (dobVal) {
                const calculatedAge = calculateAgeFromDOB(dobVal);
                ageInput.value = calculatedAge;
                ageInput.classList.remove("is-invalid");
                const ageError = document.getElementById("age-error");
                if (ageError) ageError.style.display = "none";
            }
        });
    }

    function calculateAgeFromDOB(dobString) {
        const dob = new Date(dobString);
        const today = new Date();
        let age = today.getFullYear() - dob.getFullYear();
        const m = today.getMonth() - dob.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
            age--;
        }
        return age >= 0 ? age : 0;
    }

    if (patientForm) {
        patientForm.addEventListener("submit", (e) => {
            let isValid = true;

            // Clear previous errors
            document.querySelectorAll(".invalid-feedback").forEach(el => el.style.display = "none");
            document.querySelectorAll(".form-control").forEach(el => el.classList.remove("is-invalid"));

            // Full Name Validation
            if (!nameInput || nameInput.value.trim() === "") {
                showError(nameInput, "fullName-error", "Patient full name is required.");
                isValid = false;
            }

            // Age Validation
            if (!ageInput || ageInput.value.trim() === "" || isNaN(ageInput.value)) {
                showError(ageInput, "age-error", "Age must be a valid numeric value.");
                isValid = false;
            } else {
                const ageValue = parseInt(ageInput.value, 10);
                if (ageValue < 0 || ageValue > 120) {
                    showError(ageInput, "age-error", "Age must be between allowed limits.");
                    isValid = false;
                }
            }

            // DOB Validation
            if (!dobInput || dobInput.value === "") {
                showError(dobInput, "dob-error", "Date of birth is required.");
                isValid = false;
            } else {
                const chosenDate = new Date(dobInput.value);
                const today = new Date();
                if (chosenDate > today) {
                    showError(dobInput, "dob-error", "Date of birth cannot be in the future.");
                    isValid = false;
                }
            }

            // Phone Validation
            if (!phoneInput || phoneInput.value.trim() === "") {
                showError(phoneInput, "phone-error", "Phone number is required for clinician communications.");
                isValid = false;
            } else if (!phoneRegex.test(phoneInput.value.trim())) {
                showError(phoneInput, "phone-error", "Invalid format. Use international numeric format (e.g. +1 555-0199).");
                isValid = false;
            }

            // Blood Group Validation
            if (!bloodGroupInput || bloodGroupInput.value === "") {
                showError(bloodGroupInput, "bloodGroup-error", "Blood group selection is required for clinical records.");
                isValid = false;
            }

            // Email Validation
            if (emailInput && emailInput.value.trim() !== "" && !emailRegex.test(emailInput.value.trim())) {
                showError(emailInput, "email-error", "Invalid email address structure (e.g. name@domain.com).");
                isValid = false;
            }

            if (!isValid) {
                e.preventDefault();
                patientForm.classList.add("invalid-animation");
                setTimeout(() => {
                    patientForm.classList.remove("invalid-animation");
                }, 300);
            } else {
                if (typeof CanineUI !== "undefined") {
                    CanineUI.showLoadingOverlay();
                }
            }
        });
    }

    function showError(inputElement, errorId, message) {
        if (inputElement) {
            inputElement.classList.add("is-invalid");
        }
        const errorText = document.getElementById(errorId);
        if (errorText) {
            errorText.textContent = message;
            errorText.style.display = "block";
        }
    }
});
