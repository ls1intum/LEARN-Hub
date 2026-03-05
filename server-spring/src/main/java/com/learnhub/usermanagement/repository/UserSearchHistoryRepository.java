package com.learnhub.usermanagement.repository;

import com.learnhub.usermanagement.entity.UserSearchHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, UUID> {

	@Query("SELECT h FROM UserSearchHistory h WHERE h.userId = :userId ORDER BY h.createdAt DESC")
	List<UserSearchHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
}
