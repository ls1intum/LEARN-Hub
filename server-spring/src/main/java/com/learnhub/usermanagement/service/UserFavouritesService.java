package com.learnhub.usermanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.exception.BadRequestException;
import com.learnhub.usermanagement.entity.UserFavourites;
import com.learnhub.usermanagement.repository.UserFavouritesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserFavouritesService {

	private static final Logger logger = LoggerFactory.getLogger(UserFavouritesService.class);

	private final UserFavouritesRepository userFavouritesRepository;
	private final ObjectMapper objectMapper;

	public UserFavouritesService(UserFavouritesRepository userFavouritesRepository, ObjectMapper objectMapper) {
		this.userFavouritesRepository = userFavouritesRepository;
		this.objectMapper = objectMapper;
	}

	public List<UserFavourites> getUserFavourites(UUID userId) {
		return userFavouritesRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	public List<UserFavourites> getUserFavourites(UUID userId, String type) {
		return userFavouritesRepository.findByUserIdAndFavouriteType(userId, type);
	}

	@Transactional
	public UserFavourites saveActivityFavourite(UUID userId, UUID activityId, String name) {
		UserFavourites favourite = new UserFavourites();
		favourite.setUserId(userId);
		favourite.setFavouriteType("activity");
		favourite.setActivityId(activityId);
		favourite.setName(name);
		favourite.setCreatedAt(LocalDateTime.now());
		return userFavouritesRepository.save(favourite);
	}

	@Transactional
	public UserFavourites saveLessonPlanFavourite(UUID userId, List<UUID> activityIds, String lessonPlanSnapshot,
			String name) {
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
			logger.warn("Failed to save lesson plan favourite: {}", e.getMessage(), e);
			throw new BadRequestException("Failed to save lesson plan favourite");
		}
	}

	@Transactional
	public boolean deleteFavourite(UUID favouriteId, UUID userId) {
		return userFavouritesRepository.findById(favouriteId).filter(fav -> fav.getUserId().equals(userId)).map(fav -> {
			userFavouritesRepository.delete(fav);
			return true;
		}).orElse(false);
	}

	@Transactional
	public boolean deleteActivityFavourite(UUID userId, UUID activityId) {
		List<UserFavourites> favourites = userFavouritesRepository.findByUserIdAndFavouriteTypeAndActivityId(userId,
				"activity", activityId);
		if (!favourites.isEmpty()) {
			userFavouritesRepository.delete(favourites.get(0));
			return true;
		}
		return false;
	}

	public boolean isActivityFavourited(UUID userId, UUID activityId) {
		List<UserFavourites> favourites = userFavouritesRepository.findByUserIdAndFavouriteTypeAndActivityId(userId,
				"activity", activityId);
		return !favourites.isEmpty();
	}
}
