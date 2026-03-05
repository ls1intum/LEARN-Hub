package com.learnhub.usermanagement.repository;

import com.learnhub.usermanagement.entity.VerificationCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

	Optional<VerificationCode> findByUserIdAndCodeAndUsedAndExpiresAtAfter(UUID userId, String code, String used,
			LocalDateTime currentTime);

	List<VerificationCode> findByUserId(UUID userId);
}
