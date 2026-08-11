package com.canineai.backend.controller;

import com.canineai.backend.common.ApiResponse;
import com.canineai.backend.dto.StudyResponseDto;
import com.canineai.backend.entity.Series;
import com.canineai.backend.entity.Study;
import com.canineai.backend.mapper.StudyMapper;
import com.canineai.backend.repository.SeriesRepository;
import com.canineai.backend.repository.StudyRepository;
import com.canineai.backend.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}/studies")
@RequiredArgsConstructor
@Tag(name = "Patient Studies", description = "Query uploads and studies associated with a patient EMR")
public class PatientStudyController {

    private final StudyRepository studyRepository;
    private final SeriesRepository seriesRepository;
    private final StudyMapper studyMapper;
    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "Get studies for a patient", description = "Retrieves all CBCT studies uploaded for the specified patient.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('patient:read')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPatientStudies(
            @PathVariable("patientId") String patientIdStr,
            Principal principal) {
        
        String currentUser = principal != null ? principal.getName() : "System";
        UUID patientId;
        try {
            patientId = UUID.fromString(patientIdStr);
        } catch (IllegalArgumentException e) {
            patientId = patientService.getPatientByHospitalId(patientIdStr, currentUser).getId();
        }

        // Enforce Patient Ownership Check
        patientService.getPatient(patientId, currentUser);

        List<Study> studies = studyRepository.findByPatientIdAndDeletedFalse(patientId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Study study : studies) {
            StudyResponseDto dto = studyMapper.toDto(study);
            List<Series> seriesList = seriesRepository.findByStudyId(study.getId());
            int totalSlices = seriesList.stream().mapToInt(Series::getSliceCount).sum();

            Map<String, Object> studyData = new HashMap<>();
            studyData.put("id", dto.getId());
            studyData.put("patientId", dto.getPatientId());
            studyData.put("studyInstanceUid", dto.getStudyInstanceUid());
            studyData.put("studyDate", dto.getStudyDate());
            studyData.put("studyTime", dto.getStudyTime());
            studyData.put("modality", dto.getModality());
            studyData.put("studyDescription", dto.getStudyDescription());
            studyData.put("manufacturer", dto.getManufacturer());
            studyData.put("deviceModel", dto.getDeviceModel());
            studyData.put("voxelSize", dto.getVoxelSize());
            studyData.put("pixelSpacing", dto.getPixelSpacing());
            studyData.put("sliceThickness", dto.getSliceThickness());
            studyData.put("rows", dto.getRows());
            studyData.put("columns", dto.getColumns());
            studyData.put("status", dto.getStatus());
            studyData.put("createdAt", dto.getCreatedAt());
            studyData.put("sliceCount", totalSlices);
            response.add(studyData);
        }

        return ResponseEntity.ok(ApiResponse.success(response, "Studies retrieved successfully"));
    }
}
