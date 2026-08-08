package com.canineai.backend.mapper;

import com.canineai.backend.dto.UserDto;
import com.canineai.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for User → UserDto.
 *
 * Role names are mapped via RoleNameMapper, which is a plain @Component
 * helper class. This avoids the default-method + @Named + Builder combination
 * that causes MapStruct 1.6.x to emit a stub (implements 0 interfaces) instead
 * of a full implementation, preventing Spring from finding the bean.
 */
@Mapper(componentModel = "spring", uses = RoleNameMapper.class, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface UserMapper {

    @Mapping(target = "roles", source = "roles")
    UserDto toDto(User user);
}
