package com.learnhub.activitymanagement.repository;

import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    List<Activity> findByNameContainingIgnoreCase(String name);

    List<Activity> findByFormat(ActivityFormat format);

    List<Activity> findByBloomLevel(BloomLevel bloomLevel);

    @Query("SELECT a FROM Activity a WHERE a.ageMin >= :ageMin AND a.ageMax <= :ageMax")
    List<Activity> findByAgeRange(@Param("ageMin") Integer ageMin, @Param("ageMax") Integer ageMax);

    @Query("SELECT a FROM Activity a WHERE " +
           "(:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:ageMin IS NULL OR a.ageMin >= :ageMin) AND " +
           "(:ageMax IS NULL OR a.ageMax <= :ageMax)")
    List<Activity> findWithFilters(
        @Param("name") String name,
        @Param("ageMin") Integer ageMin,
        @Param("ageMax") Integer ageMax
    );
}
