package com.canineai.backend.mapper;

import com.canineai.backend.dto.StudyResponseDto;
import com.canineai.backend.entity.Study;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudyMapper {

    @Mapping(target = "patientId", source = "patient.id")
    StudyResponseDto toDto(Study study);

    java.util.List<StudyResponseDto> toDtoList(java.util.List<Study> studies);
}
