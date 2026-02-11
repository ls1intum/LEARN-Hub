package com.learnhub.usermanagement.repository;

import com.learnhub.usermanagement.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findByUserIdAndCodeAndUsedAndExpiresAtAfter(
        UUID userId, String code, String used, LocalDateTime currentTime
    );

    void deleteByUserId(UUID userId);
}
