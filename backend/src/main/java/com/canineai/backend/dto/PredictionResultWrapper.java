package com.canineai.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PredictionResultWrapper {
    private PredictionResult prediction;
}
