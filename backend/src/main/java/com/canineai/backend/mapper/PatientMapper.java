package com.canineai.backend.mapper;

import com.canineai.backend.dto.PatientRequestDto;
import com.canineai.backend.dto.PatientResponseDto;
import com.canineai.backend.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface PatientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hospitalPatientId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "scheduledDeletionTime", ignore = true)
    Patient toEntity(PatientRequestDto dto);

    @Mapping(target = "age", expression = "java(patient.getAge())")
    PatientResponseDto toDto(Patient patient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hospitalPatientId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "scheduledDeletionTime", ignore = true)
    void updateEntityFromDto(PatientRequestDto dto, @MappingTarget Patient patient);
}
