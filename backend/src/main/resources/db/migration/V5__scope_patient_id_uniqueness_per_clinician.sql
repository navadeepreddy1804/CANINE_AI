-- V5: Scope hospital_patient_id uniqueness per clinician
--
-- Problem: hospital_patient_id had a GLOBAL unique constraint, meaning two
-- different clinicians could not both have a patient "PT-00001". This caused
-- "Patient ID already exists." for every second clinician who registered.
--
-- Fix: Replace the global unique index with a composite unique index on
-- (hospital_patient_id, created_by). Each clinician now has their own
-- independent PT-XXXXX sequence that never conflicts with another clinician.

-- 1. Drop the old global unique constraint
ALTER TABLE patients
    DROP INDEX hospital_patient_id;

-- 2. Add the new per-clinician composite unique index
ALTER TABLE patients
    ADD CONSTRAINT uq_patient_id_per_clinician
    UNIQUE (hospital_patient_id, created_by);
