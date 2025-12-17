package com.learnhub.service;

import com.learnhub.model.UserFavourites;
import com.learnhub.repository.UserFavouritesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserFavouritesService {

    @Autowired
    private UserFavouritesRepository userFavouritesRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<UserFavourites> getUserFavourites(Long userId) {
        return userFavouritesRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<UserFavourites> getUserFavourites(Long userId, String type) {
        return userFavouritesRepository.findByUserIdAndFavouriteType(userId, type);
    }

    @Transactional
    public UserFavourites saveActivityFavourite(Long userId, Long activityId, String name) {
        UserFavourites favourite = new UserFavourites();
        favourite.setUserId(userId);
        favourite.setFavouriteType("activity");
        favourite.setActivityId(activityId);
        favourite.setName(name);
        favourite.setCreatedAt(LocalDateTime.now());
        return userFavouritesRepository.save(favourite);
    }

    @Transactional
    public UserFavourites saveLessonPlanFavourite(Long userId, List<Long> activityIds, 
                                                    String lessonPlanSnapshot, String name) {
        try {
            UserFavourites favourite = new UserFavourites();
            favourite.setUserId(userId);
            favourite.setFavouriteType("lesson_plan");
            favourite.setActivityIds(objectMapper.writeValueAsString(activityIds));
            favourite.setLessonPlanSnapshot(lessonPlanSnapshot);
            favourite.setName(name);
            favourite.setCreatedAt(LocalDateTime.now());
            return userFavouritesRepository.save(favourite);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save lesson plan favourite", e);
        }
    }

    @Transactional
    public boolean deleteFavourite(Long favouriteId, Long userId) {
        return userFavouritesRepository.findById(favouriteId)
            .filter(fav -> fav.getUserId().equals(userId))
            .map(fav -> {
                userFavouritesRepository.delete(fav);
                return true;
            })
            .orElse(false);
    }
}
