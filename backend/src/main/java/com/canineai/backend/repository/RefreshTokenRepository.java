package com.canineai.backend.repository;

import com.canineai.backend.entity.RefreshToken;
import com.canineai.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    java.util.List<RefreshToken> findAllByUser(User user);

    @Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM RefreshToken r WHERE r.user = :user")
    void deleteByUser(@org.springframework.data.repository.query.Param("user") User user);
}
