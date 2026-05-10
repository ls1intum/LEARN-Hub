package com.learnhub.usermanagement.repository;

import com.learnhub.usermanagement.entity.UserFavourites;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFavouritesRepository extends JpaRepository<UserFavourites, UUID> {

	List<UserFavourites> findByUserIdOrderByCreatedAtDesc(UUID userId);

	List<UserFavourites> findByUserIdAndFavouriteType(UUID userId, String favouriteType);

	List<UserFavourites> findByUserIdAndFavouriteTypeOrderByCreatedAtDesc(UUID userId, String favouriteType);

	Page<UserFavourites> findByUserIdAndFavouriteTypeOrderByCreatedAtDesc(UUID userId, String favouriteType,
			Pageable pageable);

	List<UserFavourites> findByUserIdAndFavouriteTypeAndActivityId(UUID userId, String favouriteType, UUID activityId);

	List<UserFavourites> findByUserIdAndFavouriteTypeAndActivityIdOrderByCreatedAtDesc(UUID userId,
			String favouriteType, UUID activityId);

	List<UserFavourites> findByUserIdAndFavouriteTypeAndActivityIdIn(UUID userId, String favouriteType,
			List<UUID> activityIds);

	Page<UserFavourites> findByUserIdAndFavouriteTypeAndActivityIdInOrderByCreatedAtDesc(UUID userId,
			String favouriteType, List<UUID> activityIds, Pageable pageable);

	boolean existsByUserIdAndFavouriteTypeAndActivityId(UUID userId, String favouriteType, UUID activityId);

	@Query("SELECT fav.activityId FROM UserFavourites fav "
			+ "WHERE fav.userId = :userId AND fav.favouriteType = :favouriteType AND fav.activityId IS NOT NULL")
	List<UUID> findActivityIdsByUserIdAndFavouriteType(@Param("userId") UUID userId,
			@Param("favouriteType") String favouriteType);

	long deleteByUserIdAndFavouriteTypeAndActivityId(UUID userId, String favouriteType, UUID activityId);

	long deleteByActivityId(UUID activityId);
}
