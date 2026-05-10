package com.learnhub.usermanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.common.pagination.OffsetLimitPageRequest;
import com.learnhub.usermanagement.entity.UserFavourites;
import com.learnhub.usermanagement.repository.UserFavouritesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserFavouritesService {

	private static final Logger logger = LoggerFactory.getLogger(UserFavouritesService.class);

	@Autowired
	private UserFavouritesRepository userFavouritesRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public List<UserFavourites> getUserFavourites(UUID userId, String type) {
		return userFavouritesRepository.findByUserIdAndFavouriteType(userId, type);
	}

	public List<UserFavourites> getActivityFavouritesOrdered(UUID userId) {
		return userFavouritesRepository.findByUserIdAndFavouriteTypeOrderByCreatedAtDesc(userId, "activity");
	}

	public Page<UserFavourites> getActivityFavouritesPage(UUID userId, int limit, int offset) {
		return userFavouritesRepository.findByUserIdAndFavouriteTypeOrderByCreatedAtDesc(userId, "activity",
				buildPageRequest(limit, offset));
	}

	public Page<UserFavourites> getActivityFavouritesPage(UUID userId, Set<UUID> activityIds, int limit, int offset) {
		if (activityIds == null || activityIds.isEmpty()) {
			return new PageImpl<>(List.of(), buildPageRequest(limit, offset), 0);
		}

		return userFavouritesRepository.findByUserIdAndFavouriteTypeAndActivityIdInOrderByCreatedAtDesc(userId,
				"activity", List.copyOf(activityIds), buildPageRequest(limit, offset));
	}

	public List<UUID> getActivityFavouriteIds(UUID userId) {
		return userFavouritesRepository.findActivityIdsByUserIdAndFavouriteType(userId, "activity");
	}

	public Page<UserFavourites> getLessonPlanFavouritesPage(UUID userId, int limit, int offset) {
		return userFavouritesRepository.findByUserIdAndFavouriteTypeOrderByCreatedAtDesc(userId, "lesson_plan",
				buildPageRequest(limit, offset));
	}

	@Transactional
	public UserFavourites saveActivityFavourite(UUID userId, UUID activityId, String name) {
		List<UserFavourites> existingFavourites = userFavouritesRepository
				.findByUserIdAndFavouriteTypeAndActivityIdOrderByCreatedAtDesc(userId, "activity", activityId);
		if (!existingFavourites.isEmpty()) {
			return existingFavourites.get(0);
		}

		UserFavourites favourite = new UserFavourites();
		favourite.setUserId(userId);
		favourite.setFavouriteType("activity");
		favourite.setActivityId(activityId);
		favourite.setName(name);
		favourite.setCreatedAt(LocalDateTime.now());
		return userFavouritesRepository.save(favourite);
	}

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
			throw new RuntimeException("Failed to save lesson plan favourite", e);
		}
	}

	public boolean deleteFavourite(UUID favouriteId, UUID userId) {
		return userFavouritesRepository.findById(favouriteId).filter(fav -> fav.getUserId().equals(userId)).map(fav -> {
			userFavouritesRepository.delete(fav);
			return true;
		}).orElse(false);
	}

	@Transactional
	public boolean deleteActivityFavourite(UUID userId, UUID activityId) {
		long deletedCount = userFavouritesRepository.deleteByUserIdAndFavouriteTypeAndActivityId(userId, "activity",
				activityId);
		return deletedCount > 0;
	}

	public boolean isActivityFavourited(UUID userId, UUID activityId) {
		return userFavouritesRepository.existsByUserIdAndFavouriteTypeAndActivityId(userId, "activity", activityId);
	}

	private OffsetLimitPageRequest buildPageRequest(int limit, int offset) {
		return OffsetLimitPageRequest.of(Math.max(limit, 1), Math.max(offset, 0),
				Sort.by(Sort.Direction.DESC, "createdAt"));
	}
}
