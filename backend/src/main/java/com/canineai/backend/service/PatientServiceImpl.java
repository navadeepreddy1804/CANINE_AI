package com.canineai.backend.service;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.common.PagedResponse;
import com.canineai.backend.dto.PatientRequestDto;
import com.canineai.backend.dto.PatientResponseDto;
import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.Patient;
import com.canineai.backend.entity.PatientStatus;
import com.canineai.backend.mapper.PatientMapper;
import com.canineai.backend.repository.PatientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public PatientResponseDto createPatient(PatientRequestDto request, String currentUser) {
        log.info("Admitting new patient: Email={}, Creator={}", request.getEmail(), currentUser);

        validatePatientRequest(request);
        String hospitalPatientId = generateUniqueHospitalPatientId(currentUser);
        if (patientRepository.existsByPhoneAndCreatedByAndDeletedFalse(request.getPhone(), currentUser)) {
            throw new BusinessException.ConflictException("Phone number already exists.");
        }
        if (patientRepository.existsByEmailAndCreatedByAndDeletedFalse(request.getEmail(), currentUser)) {
            throw new BusinessException.ConflictException("Email already exists.");
        }

        Patient patient = patientMapper.toEntity(request);
        patient.setHospitalPatientId(hospitalPatientId);
        patient.setRegistrationDate(LocalDate.now());
        patient.setCreatedBy(currentUser);
        patient.setCreatedAt(LocalDateTime.now());

        Patient saved = patientRepository.save(patient);
        return patientMapper.toDto(saved);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "patients", key = "#id")
    public PatientResponseDto updatePatient(UUID id, PatientRequestDto request, String currentUser) {
        log.info("Updating patient profile: {}, Operator={}", id, currentUser);

        Patient patient = patientRepository.findByIdActive(id)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Patient EMR not found"));

        // Enforce ownership check
        if (currentUser != null && !currentUser.equals("System") && !patient.getCreatedBy().equals(currentUser)) {
            throw new BusinessException.UnauthorizedException("Access Denied: You do not have ownership of this EMR patient record.");
        }

        validatePatientRequest(request);
        if (!patient.getPhone().equals(request.getPhone()) &&
                patientRepository.existsByPhoneAndCreatedByAndDeletedFalse(request.getPhone(), currentUser)) {
            throw new BusinessException.ConflictException("Phone number already exists.");
        }
        if (!patient.getEmail().equals(request.getEmail()) &&
                patientRepository.existsByEmailAndCreatedByAndDeletedFalse(request.getEmail(), currentUser)) {
            throw new BusinessException.ConflictException("Email already exists.");
        }

        patientMapper.updateEntityFromDto(request, patient);
        patient.setUpdatedBy(currentUser);
        patient.setUpdatedAt(LocalDateTime.now());

        Patient saved = patientRepository.save(patient);
        return patientMapper.toDto(saved);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "patients", key = "#id")
    public PatientResponseDto getPatient(UUID id, String currentUser) {
        Patient patient = patientRepository.findByIdActive(id)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Patient EMR not found"));

        // Enforce ownership check
        if (currentUser != null && !currentUser.equals("System") && !patient.getCreatedBy().equals(currentUser)) {
            throw new BusinessException.UnauthorizedException("Access Denied: You do not have ownership of this EMR patient record.");
        }
        return patientMapper.toDto(patient);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "patients", key = "#id")
    public void deletePatient(UUID id, String currentUser) {
        log.info("Soft-deleting patient EMR file: {}, Operator={}", id, currentUser);
        Patient patient = patientRepository.findByIdActive(id)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Patient EMR not found"));

        // Enforce ownership check
        if (currentUser != null && !currentUser.equals("System") && !patient.getCreatedBy().equals(currentUser)) {
            throw new BusinessException.UnauthorizedException("Access Denied: You do not have ownership of this EMR patient record.");
        }

        patient.setStatus(PatientStatus.PENDING_DELETION);
        patient.setScheduledDeletionTime(LocalDateTime.now().plusDays(3));
        patient.setDeletedBy(currentUser);
        patient.setDeletedAt(LocalDateTime.now());
        patient.setDeleted(false);

        patientRepository.save(patient);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "patients", key = "#id")
    public PatientResponseDto restorePatient(UUID id, String currentUser) {
        log.info("Restoring patient EMR file: {}, Operator={}", id, currentUser);
        Patient patient = patientRepository.findByIdActive(id)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Patient EMR not found"));

        // Enforce ownership check
        if (currentUser != null && !currentUser.equals("System") && !patient.getCreatedBy().equals(currentUser)) {
            throw new BusinessException.UnauthorizedException("Access Denied: You do not have ownership of this EMR patient record.");
        }

        patient.setStatus(PatientStatus.ACTIVE);
        patient.setScheduledDeletionTime(null);
        patient.setDeletedBy(null);
        patient.setDeletedAt(null);
        patient.setDeleted(false);

        Patient saved = patientRepository.save(patient);
        return patientMapper.toDto(saved);
    }

    private void validatePatientRequest(PatientRequestDto request) {
        if (request.getFullName() == null || request.getFullName().isBlank()
                || request.getPhone() == null || request.getPhone().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getOrthodontist() == null || request.getOrthodontist().isBlank()
                || request.getHospital() == null || request.getHospital().isBlank()) {
            throw new BusinessException.ValidationException("Required fields are missing.");
        }

        if (request.getDateOfBirth() == null || request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BusinessException.ValidationException("Invalid date of birth.");
        }

        int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < 0 || age > 120) {
            throw new BusinessException.ValidationException("Age must be between allowed limits.");
        }

        if (!request.getPhone().matches("^\\+?[0-9\\-\\s()]{7,20}$")) {
            throw new BusinessException.ValidationException("Invalid phone number format.");
        }

        if (!request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException.ValidationException("Invalid email address.");
        }
    }

    private String generateUniqueHospitalPatientId(String currentUser) {
        int nextSequence = getNextPatientSequence(currentUser);
        return String.format("PT-%05d", nextSequence);
    }

    private int getNextPatientSequence(String currentUser) {
        String owner = currentUser == null || currentUser.isBlank() ? "System" : currentUser;
        entityManager.createNativeQuery("""
                INSERT INTO patient_id_sequences (created_by, next_value)
                VALUES (:owner, 0)
                ON DUPLICATE KEY UPDATE created_by = VALUES(created_by)
                """)
                .setParameter("owner", owner)
                .executeUpdate();

        Number currentValue = (Number) entityManager.createNativeQuery("""
                SELECT next_value FROM patient_id_sequences
                WHERE created_by = :owner FOR UPDATE
                """)
                .setParameter("owner", owner)
                .getSingleResult();
        int nextValue = currentValue.intValue() + 1;

        entityManager.createNativeQuery("""
                UPDATE patient_id_sequences SET next_value = :nextValue
                WHERE created_by = :owner
                """)
                .setParameter("nextValue", nextValue)
                .setParameter("owner", owner)
                .executeUpdate();
        return nextValue;
    }

    @Override
    public PagedResponse<PatientResponseDto> listPatients(
            Gender gender,
            LocalDate startDate,
            LocalDate endDate,
            String orthodontist,
            String hospital,
            PatientStatus status,
            Pageable pageable,
            String currentUser) {

        Specification<Patient> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registrationDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("registrationDate"), endDate));
            }
            if (orthodontist != null && !orthodontist.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("orthodontist")), "%" + orthodontist.toLowerCase() + "%"));
            }
            if (hospital != null && !hospital.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("hospital")), "%" + hospital.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), PatientStatus.PENDING_DELETION));
            }

            // Enforce Patient Ownership
            if (currentUser != null && !currentUser.equals("System")) {
                predicates.add(cb.equal(root.get("createdBy"), currentUser));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Patient> page = patientRepository.findAllActive(spec, pageable);
        return mapToPagedResponse(page);
    }

    @Override
    public PagedResponse<PatientResponseDto> searchPatients(String query, Pageable pageable, String currentUser) {
        if (query == null || query.isBlank()) {
            return listPatients(null, null, null, null, null, null, pageable, currentUser);
        }

        Specification<Patient> spec = (root, q, cb) -> {
            String pattern = "%" + query.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(cb.lower(root.get("hospitalPatientId")), pattern));
            predicates.add(cb.like(cb.lower(root.get("fullName")), pattern));
            predicates.add(cb.like(cb.lower(root.get("phone")), pattern));
            predicates.add(cb.like(cb.lower(root.get("email")), pattern));
            predicates.add(cb.like(cb.lower(root.get("orthodontist")), pattern));
            predicates.add(cb.like(cb.lower(root.get("hospital")), pattern));

            // Try to match UUID if pattern is valid UUID shape
            try {
                UUID uuid = UUID.fromString(query);
                predicates.add(cb.equal(root.get("id"), uuid));
            } catch (IllegalArgumentException e) {
                // Ignore mismatch
            }

            Predicate searchPred = cb.or(predicates.toArray(new Predicate[0]));
            Predicate notPendingDeletion = cb.notEqual(root.get("status"), PatientStatus.PENDING_DELETION);

            // Enforce Patient Ownership
            if (currentUser != null && !currentUser.equals("System")) {
                return cb.and(searchPred, notPendingDeletion, cb.equal(root.get("createdBy"), currentUser));
            }

            return cb.and(searchPred, notPendingDeletion);
        };

        Page<Patient> page = patientRepository.findAllActive(spec, pageable);
        return mapToPagedResponse(page);
    }

    private PagedResponse<PatientResponseDto> mapToPagedResponse(Page<Patient> page) {
        List<PatientResponseDto> content = page.getContent().stream()
                .map(patientMapper::toDto)
                .collect(Collectors.toList());

        return PagedResponse.<PatientResponseDto>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .lastPage(page.isLast())
                .build();
    }
}
