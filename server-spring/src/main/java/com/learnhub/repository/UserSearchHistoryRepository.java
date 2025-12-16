package com.learnhub.repository;

import com.learnhub.model.UserSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, Long> {

    List<UserSearchHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
