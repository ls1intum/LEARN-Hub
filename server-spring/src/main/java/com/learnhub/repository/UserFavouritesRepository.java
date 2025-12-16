package com.learnhub.repository;

import com.learnhub.model.UserFavourites;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavouritesRepository extends JpaRepository<UserFavourites, Long> {

    List<UserFavourites> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserFavourites> findByUserIdAndFavouriteType(Long userId, String favouriteType);
}
