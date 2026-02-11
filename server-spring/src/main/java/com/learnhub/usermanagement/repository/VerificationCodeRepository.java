package com.learnhub.usermanagement.repository;

import com.learnhub.usermanagement.entity.VerificationCode;
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
