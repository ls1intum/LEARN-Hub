package com.learnhub.controller;

import com.learnhub.dto.response.ActivityResponse;
import com.learnhub.dto.response.ApiResponse;
import com.learnhub.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activities", description = "Activity management and recommendations endpoints")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/")
    @Operation(summary = "Get activities", description = "Get a list of activities with optional filtering and pagination")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivities(
            @RequestParam(required = false) String name,
            @RequestParam(name = "age_min", required = false) Integer ageMin,
            @RequestParam(name = "age_max", required = false) Integer ageMax,
            @RequestParam(required = false) List<String> format,
            @RequestParam(name = "bloom_level", required = false) List<String> bloomLevel,
            @RequestParam(required = false) List<String> resources_needed,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        try {
            List<ActivityResponse> activities = activityService.getActivitiesWithFilters(
                name, ageMin, ageMax, format, bloomLevel, limit, offset
            );
            return ResponseEntity.ok(ApiResponse.success(activities));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID", description = "Get a single activity by its ID")
    public ResponseEntity<ApiResponse<ActivityResponse>> getActivity(@PathVariable Long id) {
        try {
            ActivityResponse activity = activityService.getActivityById(id);
            return ResponseEntity.ok(ApiResponse.success(activity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete activity", description = "Delete an activity by its ID")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Long id) {
        try {
            activityService.deleteActivity(id);
            return ResponseEntity.ok(ApiResponse.success("Activity deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
