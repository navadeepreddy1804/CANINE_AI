package com.canineai.backend.repository;

import com.canineai.backend.entity.ClinicalReport;
import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.Patient;
import com.canineai.backend.entity.PatientStatus;
import com.canineai.backend.entity.ReportStatus;
import com.canineai.backend.entity.ReportStyle;
import com.canineai.backend.entity.Study;
import com.canineai.backend.entity.StudyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:clinical-report-repository;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClinicalReportRepositoryTest {

    private static final String DOCTOR_A = "doctor-a@example.com";
    private static final String DOCTOR_B = "doctor-b@example.com";

    @Autowired
    private ClinicalReportRepository clinicalReportRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Test
    void ownerScopedQueriesExcludeAnotherDoctorsReport() {
        ClinicalReport reportA = createReportFor(DOCTOR_A, "A");
        ClinicalReport reportB = createReportFor(DOCTOR_B, "B");

        assertThat(clinicalReportRepository.findAllOwned(DOCTOR_A))
                .extracting(ClinicalReport::getId)
                .containsExactly(reportA.getId());

        assertThat(clinicalReportRepository.findByIdOwned(reportA.getId(), DOCTOR_A))
                .contains(reportA);
        assertThat(clinicalReportRepository.findByIdOwned(reportB.getId(), DOCTOR_A))
                .isEmpty();

        assertThat(clinicalReportRepository.findByStudyIdOwned(reportA.getStudyId(), DOCTOR_A))
                .contains(reportA);
        assertThat(clinicalReportRepository.findByStudyIdOwned(reportB.getStudyId(), DOCTOR_A))
                .isEmpty();
    }

    private ClinicalReport createReportFor(String owner, String suffix) {
        LocalDateTime now = LocalDateTime.now();

        Patient patient = Patient.builder()
                .hospitalPatientId("PT-" + suffix)
                .fullName("Patient " + suffix)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.OTHER)
                .phone("900000000" + suffix)
                .email("patient-" + suffix + "@example.com")
                .orthodontist("Dr. " + suffix)
                .hospital("CanineAI Clinic")
                .registrationDate(LocalDate.now())
                .status(PatientStatus.ACTIVE)
                .build();
        patient.setCreatedBy(owner);
        patient.setCreatedAt(now);
        patient = patientRepository.saveAndFlush(patient);

        Study study = Study.builder()
                .patient(patient)
                .studyInstanceUid("1.2.840.10008." + suffix)
                .modality("CT")
                .status(StudyStatus.COMPLETED)
                .build();
        study.setCreatedBy(owner);
        study.setCreatedAt(now);
        study = studyRepository.saveAndFlush(study);

        ClinicalReport report = ClinicalReport.builder()
                .studyId(study.getId())
                .status(ReportStatus.COMPLETED)
                .reportStyle(ReportStyle.CLINICAL)
                .reportMarkdown("Persisted report " + suffix)
                .activeProvider("persisted")
                .templateVersion("1")
                .promptVersion("1")
                .promptTemplateKey("clinical")
                .build();
        report.setCreatedBy(owner);
        report.setCreatedAt(now);
        return clinicalReportRepository.saveAndFlush(report);
    }
}
