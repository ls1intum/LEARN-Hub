package com.learnhub.usermanagement.repository;

import com.learnhub.usermanagement.entity.UserFavourites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavouritesRepository extends JpaRepository<UserFavourites, Long> {

    List<UserFavourites> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserFavourites> findByUserIdAndFavouriteType(Long userId, String favouriteType);

    List<UserFavourites> findByUserIdAndFavouriteTypeAndActivityId(Long userId, String favouriteType, Long activityId);
}
