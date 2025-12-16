package com.learnhub.repository;

import com.learnhub.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByUserIdAndCodeAndUsedAndExpiresAtAfter(
        Long userId, String code, String used, LocalDateTime currentTime
    );

    void deleteByUserId(Long userId);
}
