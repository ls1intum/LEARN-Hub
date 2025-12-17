package com.learnhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.dto.response.ApiResponse;
import com.learnhub.model.UserFavourites;
import com.learnhub.model.UserSearchHistory;
import com.learnhub.service.UserFavouritesService;
import com.learnhub.service.UserSearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
@Tag(name = "History", description = "User history and favourites management")
@SecurityRequirement(name = "BearerAuth")
public class HistoryController {

    @Autowired
    private UserSearchHistoryService searchHistoryService;

    @Autowired
    private UserFavouritesService favouritesService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get search history", description = "Get user's search history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSearchHistory(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }

            List<UserSearchHistory> history = searchHistoryService.getUserSearchHistory(userId, limit, offset);

            List<Map<String, Object>> historyData = history.stream()
                .limit(limit)
                .skip(offset)
                .map(entry -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", entry.getId());
                    try {
                        data.put("search_criteria", objectMapper.readValue(entry.getSearchCriteria(), Map.class));
                    } catch (Exception e) {
                        data.put("search_criteria", new HashMap<>());
                    }
                    data.put("created_at", entry.getCreatedAt().toString());
                    return data;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("search_history", historyData);
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("limit", limit);
            pagination.put("offset", offset);
            pagination.put("count", historyData.size());
            response.put("pagination", pagination);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve search history: " + e.getMessage()));
        }
    }

    @DeleteMapping("/search/{historyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete search history entry", description = "Delete a specific search history entry")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteSearchHistory(
            @PathVariable Long historyId,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }

            boolean deleted = searchHistoryService.deleteSearchHistory(historyId, userId);
            if (!deleted) {
                return ResponseEntity.status(404).body(ApiResponse.error("Search history entry not found"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Search history entry deleted successfully");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete search history: " + e.getMessage()));
        }
    }

    @GetMapping("/favourites")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get favourites", description = "Get user's favourites (activities and lesson plans)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFavourites(
            @RequestParam(required = false) String type,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }

            List<UserFavourites> favourites = type != null ? 
                favouritesService.getUserFavourites(userId, type) :
                favouritesService.getUserFavourites(userId);

            List<Map<String, Object>> favouritesData = favourites.stream()
                .map(fav -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", fav.getId());
                    data.put("favourite_type", fav.getFavouriteType());
                    data.put("name", fav.getName());
                    data.put("created_at", fav.getCreatedAt().toString());
                    
                    if ("activity".equals(fav.getFavouriteType())) {
                        data.put("activity_id", fav.getActivityId());
                    } else if ("lesson_plan".equals(fav.getFavouriteType())) {
                        try {
                            data.put("activity_ids", objectMapper.readValue(fav.getActivityIds(), List.class));
                            if (fav.getLessonPlanSnapshot() != null) {
                                data.put("lesson_plan_snapshot", objectMapper.readValue(fav.getLessonPlanSnapshot(), Object.class));
                            }
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                    }
                    return data;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(favouritesData));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve favourites: " + e.getMessage()));
        }
    }

    @PostMapping("/favourites/activities")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Save activity favourite", description = "Save an activity as favourite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveActivityFavourite(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }

            Long activityId = Long.valueOf(requestBody.get("activity_id").toString());
            String name = (String) requestBody.get("name");

            UserFavourites favourite = favouritesService.saveActivityFavourite(userId, activityId, name);

            Map<String, Object> response = new HashMap<>();
            response.put("id", favourite.getId());
            response.put("message", "Activity favourite saved successfully");

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to save activity favourite: " + e.getMessage()));
        }
    }

    @PostMapping("/favourites/lesson-plans")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Save lesson plan favourite", description = "Save a lesson plan as favourite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveLessonPlanFavourite(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }

            @SuppressWarnings("unchecked")
            List<Long> activityIds = ((List<Object>) requestBody.get("activity_ids")).stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
            String name = (String) requestBody.get("name");
            String lessonPlanSnapshot = objectMapper.writeValueAsString(requestBody.get("lesson_plan_snapshot"));

            UserFavourites favourite = favouritesService.saveLessonPlanFavourite(userId, activityIds, lessonPlanSnapshot, name);

            Map<String, Object> response = new HashMap<>();
            response.put("id", favourite.getId());
            response.put("message", "Lesson plan favourite saved successfully");

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to save lesson plan favourite: " + e.getMessage()));
        }
    }

    @DeleteMapping("/favourites/{favouriteId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete favourite", description = "Delete a favourite (activity or lesson plan)")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteFavourite(
            @PathVariable Long favouriteId,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }

            boolean deleted = favouritesService.deleteFavourite(favouriteId, userId);
            if (!deleted) {
                return ResponseEntity.status(404).body(ApiResponse.error("Favourite not found"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Favourite deleted successfully");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete favourite: " + e.getMessage()));
        }
    }
}
