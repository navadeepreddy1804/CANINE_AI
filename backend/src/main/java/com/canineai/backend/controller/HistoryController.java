package com.canineai.backend.controller;

import com.canineai.backend.common.ApiResponse;
import com.canineai.backend.entity.AnalysisHistory;
import com.canineai.backend.repository.AnalysisHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@Tag(name = "Analysis History", description = "Endpoints for fetching chronological analysis execution history")
public class HistoryController {

    private final AnalysisHistoryRepository historyRepository;

    @GetMapping
    @Operation(summary = "List analysis history", description = "Returns chronological history logs of completed AI diagnostic runs, newest first.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('reports:read')")
    public ResponseEntity<ApiResponse<List<AnalysisHistory>>> getHistory(Principal principal) {
        String currentUser = principal != null ? principal.getName() : "System";
        List<AnalysisHistory> list = historyRepository.findByCreatedByOrderByCompletedAtDesc(currentUser);
        if (list.isEmpty()) {
            list = historyRepository.findAllByOrderByCompletedAtDesc();
        }
        return ResponseEntity.ok(ApiResponse.success(list, "Analysis history retrieved successfully"));
    }
}
