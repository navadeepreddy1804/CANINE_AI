package com.canineai.backend.service;

import com.canineai.backend.dto.PatientRequestDto;
import com.canineai.backend.dto.PatientResponseDto;
import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.Patient;
import com.canineai.backend.entity.PatientStatus;
import com.canineai.backend.mapper.PatientMapper;
import com.canineai.backend.repository.PatientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PatientServiceImpl patientService;

    @Test
    void createPatient_generatesHospitalPatientId_and_ignoresClientProvidedId() {
        PatientRequestDto request = new PatientRequestDto();
        request.setHospitalPatientId("PT-00001");
        request.setFullName("Jane Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setGender(Gender.FEMALE);
        request.setPhone("+1234567890");
        request.setEmail("jane@example.com");
        request.setOrthodontist("Dr. Smith");
        request.setHospital("Metro Dental");
        request.setStatus(PatientStatus.ACTIVE);

        Patient patientEntity = new Patient();
        patientEntity.setId(UUID.randomUUID());
        patientEntity.setHospitalPatientId("PT-00001");

        Query nativeQuery = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);
        ReflectionTestUtils.setField(patientService, "entityManager", entityManager);

        when(patientMapper.toEntity(any(PatientRequestDto.class))).thenReturn(patientEntity);
        when(patientRepository.existsByPhoneAndCreatedByAndDeletedFalse(anyString(), anyString())).thenReturn(false);
        when(patientRepository.existsByEmailAndCreatedByAndDeletedFalse(anyString(), anyString())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(patientMapper.toDto(any(Patient.class))).thenAnswer(invocation -> {
            Patient savedPatient = invocation.getArgument(0);
            PatientResponseDto response = new PatientResponseDto();
            response.setId(savedPatient.getId());
            response.setHospitalPatientId(savedPatient.getHospitalPatientId());
            return response;
        });

        PatientResponseDto response = patientService.createPatient(request, "doctor@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getHospitalPatientId()).isEqualTo("PT-00002");
    }

    @Test
    void createPatient_allocatesDifferentIds_and_owners_for_different_users() {
        PatientRequestDto firstRequest = validRequest("shared@example.com", "+1234567890");
        PatientRequestDto secondRequest = validRequest("shared@example.com", "+1234567890");

        Query nativeQuery = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(41, 42);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);
        ReflectionTestUtils.setField(patientService, "entityManager", entityManager);
        when(patientRepository.existsByPhoneAndCreatedByAndDeletedFalse(anyString(), anyString())).thenReturn(false);
        when(patientRepository.existsByEmailAndCreatedByAndDeletedFalse(anyString(), anyString())).thenReturn(false);
        when(patientMapper.toEntity(any(PatientRequestDto.class))).thenAnswer(invocation -> new Patient());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(patientMapper.toDto(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            PatientResponseDto response = new PatientResponseDto();
            response.setHospitalPatientId(patient.getHospitalPatientId());
            return response;
        });

        PatientResponseDto first = patientService.createPatient(firstRequest, "first@example.com");
        PatientResponseDto second = patientService.createPatient(secondRequest, "second@example.com");

        assertThat(first.getHospitalPatientId()).isEqualTo("PT-00042");
        assertThat(second.getHospitalPatientId()).isEqualTo("PT-00043");
        org.mockito.Mockito.verify(patientRepository, org.mockito.Mockito.times(2)).save(
                org.mockito.ArgumentMatchers.<Patient>argThat(patient -> patient.getCreatedBy().endsWith("@example.com")));
    }

    private PatientRequestDto validRequest(String email, String phone) {
        PatientRequestDto request = new PatientRequestDto();
        request.setHospitalPatientId("client-supplied-id");
        request.setFullName("Jane Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setGender(Gender.FEMALE);
        request.setPhone(phone);
        request.setEmail(email);
        request.setOrthodontist("Dr. Smith");
        request.setHospital("Metro Dental");
        request.setStatus(PatientStatus.ACTIVE);
        return request;
    }
}
