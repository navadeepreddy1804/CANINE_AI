package com.canineai.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BoundingBox {
    private Integer sliceIndex;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
}
