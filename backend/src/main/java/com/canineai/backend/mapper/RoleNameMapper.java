package com.canineai.backend.mapper;

import com.canineai.backend.entity.Role;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper component used by UserMapper to convert a Set<Role> to Set<String>.
 *
 * Extracted from UserMapper to avoid the MapStruct 1.6.x stub-generation bug
 * that occurs when a default @Named method is combined with
 * @Builder(disableBuilder=true) on the same mapper interface.
 */
@Component
public class RoleNameMapper {

    public Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
