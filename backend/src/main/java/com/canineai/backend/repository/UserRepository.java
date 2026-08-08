package com.canineai.backend.repository;

import com.canineai.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailActive(@Param("email") String email);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByUsernameAndDeletedFalse(String username);

    boolean existsByPhoneAndDeletedFalse(String phone);

    boolean existsByMedicalRegistrationNumberAndDeletedFalse(String medicalRegistrationNumber);
}
