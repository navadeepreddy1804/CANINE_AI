package com.canineai.backend.controller;

import com.canineai.backend.common.ApiResponse;
import com.canineai.backend.common.PagedResponse;
import com.canineai.backend.dto.PatientRequestDto;
import com.canineai.backend.dto.PatientResponseDto;
import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.PatientStatus;
import com.canineai.backend.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "EMR operations for admitting, listing, updating and archiving patients")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Admit new patient", description = "Creates a new patient profile inside the EMR database.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('patient:create')")
    public ResponseEntity<ApiResponse<PatientResponseDto>> createPatient(
            @Valid @RequestBody PatientRequestDto request,
            Principal principal) {
        
        String currentUser = principal != null ? principal.getName() : "System";
        PatientResponseDto response = patientService.createPatient(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient admitted successfully"));
    }

    @GetMapping
    @Operation(summary = "List patient EMR records", description = "Retrieves paginated, filterable patient records with multi-column sorting.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('patient:read')")
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponseDto>>> listPatients(
            @RequestParam(name = "gender", required = false) Gender gender,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "orthodontist", required = false) String orthodontist,
            @RequestParam(name = "hospital", required = false) String hospital,
            @RequestParam(name = "status", required = false) PatientStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "fullName,asc") String[] sort,
            Principal principal) {

        Pageable pageable = parsePageable(page, size, sort);
        String currentUser = principal != null ? principal.getName() : "System";
        PagedResponse<PatientResponseDto> response = patientService.listPatients(
                gender, startDate, endDate, orthodontist, hospital, status, pageable, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Patients list retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch patient profile", description = "Retrieves a single patient EMR file by UUID.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('patient:read')")
    public ResponseEntity<ApiResponse<PatientResponseDto>> getPatient(
            @PathVariable("id") UUID id,
            Principal principal) {
        String currentUser = principal != null ? principal.getName() : "System";
        PatientResponseDto response = patientService.getPatient(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient EMR profile", description = "Updates attributes. Performs optimistic lock checks using @Version.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('patient:write')")
    public ResponseEntity<ApiResponse<PatientResponseDto>> updatePatient(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PatientRequestDto request,
            Principal principal) {
        
        String currentUser = principal != null ? principal.getName() : "System";
        PatientResponseDto response = patientService.updatePatient(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete patient EMR file", description = "Flags the record as deleted and tracks operator and timestamps.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('patient:delete')")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @PathVariable("id") UUID id,
            Principal principal) {
        
        String currentUser = principal != null ? principal.getName() : "System";
        patientService.deletePatient(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Patient soft-deleted successfully"));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore soft-deleted patient EMR", description = "Resets deletion flags and brings the patient back to active status.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('patient:delete')")
    public ResponseEntity<ApiResponse<PatientResponseDto>> restorePatient(
            @PathVariable("id") UUID id,
            Principal principal) {
        
        String currentUser = principal != null ? principal.getName() : "System";
        PatientResponseDto response = patientService.restorePatient(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient restored successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search EMR profiles", description = "Performs dynamic text queries matching names, phone, email, and orthodontist.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('patient:read')")
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponseDto>>> searchPatients(
            @RequestParam("query") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "fullName,asc") String[] sort,
            Principal principal) {

        Pageable pageable = parsePageable(page, size, sort);
        String currentUser = principal != null ? principal.getName() : "System";
        PagedResponse<PatientResponseDto> response = patientService.searchPatients(query, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Search results retrieved successfully"));
    }

    private Pageable parsePageable(int page, int size, String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort != null && sort.length > 0) {
            if (sort[0].contains(",")) {
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    orders.add(new Sort.Order(Sort.Direction.fromString(_sort[1]), _sort[0]));
                }
            } else {
                orders.add(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
            }
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }
}
