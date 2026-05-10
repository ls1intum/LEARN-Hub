package com.learnhub.usermanagement.repository;

import com.learnhub.usermanagement.entity.UserFavourites;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFavouritesRepository extends JpaRepository<UserFavourites, UUID> {

	List<UserFavourites> findByUserIdOrderByCreatedAtDesc(UUID userId);

	List<UserFavourites> findByUserIdAndFavouriteType(UUID userId, String favouriteType);

	List<UserFavourites> findByUserIdAndFavouriteTypeOrderByCreatedAtDesc(UUID userId, String favouriteType);

	List<UserFavourites> findByUserIdAndFavouriteTypeAndActivityId(UUID userId, String favouriteType, UUID activityId);

	List<UserFavourites> findByUserIdAndFavouriteTypeAndActivityIdOrderByCreatedAtDesc(UUID userId,
			String favouriteType, UUID activityId);

	List<UserFavourites> findByUserIdAndFavouriteTypeAndActivityIdIn(UUID userId, String favouriteType,
			List<UUID> activityIds);

	long deleteByUserIdAndFavouriteTypeAndActivityId(UUID userId, String favouriteType, UUID activityId);

	long deleteByActivityId(UUID activityId);
}
