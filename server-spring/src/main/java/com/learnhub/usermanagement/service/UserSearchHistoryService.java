package com.learnhub.usermanagement.service;

import com.learnhub.usermanagement.entity.UserSearchHistory;
import com.learnhub.usermanagement.repository.UserSearchHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserSearchHistoryService {

    @Autowired
    private UserSearchHistoryRepository userSearchHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveSearchQuery(UUID userId, Map<String, Object> searchCriteria) {
        try {
            UserSearchHistory history = new UserSearchHistory();
            history.setUserId(userId);
            history.setSearchCriteria(objectMapper.writeValueAsString(searchCriteria));
            history.setCreatedAt(LocalDateTime.now());
            userSearchHistoryRepository.save(history);
        } catch (Exception e) {
            // Log but don't fail if search history saving fails
        }
    }

    public List<UserSearchHistory> getUserSearchHistory(UUID userId, Integer limit, Integer offset) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                offset / limit, limit);
        return userSearchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public boolean deleteSearchHistory(UUID historyId, UUID userId) {
        return userSearchHistoryRepository.findById(historyId)
                .filter(history -> history.getUserId().equals(userId))
                .map(history -> {
                    userSearchHistoryRepository.delete(history);
                    return true;
                })
                .orElse(false);
    }
}
