package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column
    private String phone;

    @Column(nullable = true, unique = true)
    private String username;

    @Column(nullable = true)
    private String roleTitle; // e.g. Chief Orthodontist

    @Column(nullable = true)
    private String hospital; // e.g. Metro Dental Diagnostics

    @Column(nullable = true)
    private String department;

    @Column(name = "medical_registration_number", nullable = true, unique = true)
    private String medicalRegistrationNumber;

    @Column(name = "security_question")
    private String securityQuestion;

    @Column(name = "security_answer")
    private String securityAnswer;

    @Column(name = "years_of_experience", nullable = true)
    private Integer yearsOfExperience;

    @Column(name = "blood_group", nullable = true)
    private String bloodGroup;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Builder.Default
    @Column(name = "account_expired", nullable = false)
    private boolean accountExpired = false;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
