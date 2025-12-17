package com.learnhub.service;

import com.learnhub.model.UserSearchHistory;
import com.learnhub.repository.UserSearchHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserSearchHistoryService {

    @Autowired
    private UserSearchHistoryRepository userSearchHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void saveSearchQuery(Long userId, Map<String, Object> searchCriteria) {
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

    public List<UserSearchHistory> getUserSearchHistory(Long userId, Integer limit, Integer offset) {
        return userSearchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public boolean deleteSearchHistory(Long historyId, Long userId) {
        return userSearchHistoryRepository.findById(historyId)
            .filter(history -> history.getUserId().equals(userId))
            .map(history -> {
                userSearchHistoryRepository.delete(history);
                return true;
            })
            .orElse(false);
    }
}
