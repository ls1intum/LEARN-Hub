package com.learnhub.usermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.usermanagement.entity.UserFavourites;
import com.learnhub.usermanagement.entity.UserSearchHistory;
import com.learnhub.usermanagement.service.UserFavouritesService;
import com.learnhub.usermanagement.service.UserSearchHistoryService;
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
import java.util.UUID;
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
    public ResponseEntity<?> getSearchHistory(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            List<UserSearchHistory> history = searchHistoryService.getUserSearchHistory(userId, limit, offset);

            List<Map<String, Object>> historyData = history.stream()
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to retrieve search history: " + e.getMessage()));
        }
    }

    @DeleteMapping("/search/{historyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete search history entry", description = "Delete a specific search history entry")
    public ResponseEntity<?> deleteSearchHistory(
            @PathVariable UUID historyId,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            boolean deleted = searchHistoryService.deleteSearchHistory(historyId, userId);
            if (!deleted) {
                return ResponseEntity.status(404).body(ErrorResponse.of("Search history entry not found"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Search history entry deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to delete search history: " + e.getMessage()));
        }
    }

    @GetMapping("/favourites/activities")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get activity favourites", description = "Get user's favourite activities")
    public ResponseEntity<?> getActivityFavourites(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            List<UserFavourites> favourites = favouritesService.getUserFavourites(userId, "activity");

            List<Map<String, Object>> favouritesData = favourites.stream()
                    .map(fav -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", fav.getId());
                        data.put("favourite_type", fav.getFavouriteType());
                        data.put("activity_id", fav.getActivityId());
                        data.put("name", fav.getName());
                        data.put("created_at", fav.getCreatedAt().toString());
                        return data;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("favourites", favouritesData);
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("limit", limit);
            pagination.put("offset", offset);
            pagination.put("count", favouritesData.size());
            response.put("pagination", pagination);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to retrieve activity favourites: " + e.getMessage()));
        }
    }

    @GetMapping("/favourites/lesson-plans")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get lesson plan favourites", description = "Get user's favourite lesson plans")
    public ResponseEntity<?> getLessonPlanFavourites(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            List<UserFavourites> favourites = favouritesService.getUserFavourites(userId, "lesson_plan");

            List<Map<String, Object>> favouritesData = favourites.stream()
                    .map(fav -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", fav.getId());
                        data.put("favourite_type", fav.getFavouriteType());
                        data.put("name", fav.getName());
                        data.put("created_at", fav.getCreatedAt().toString());
                        
                        try {
                            data.put("activity_ids", objectMapper.readValue(fav.getActivityIds(), List.class));
                            if (fav.getLessonPlanSnapshot() != null) {
                                data.put("lesson_plan", objectMapper.readValue(fav.getLessonPlanSnapshot(), Object.class));
                            }
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                        return data;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("favourites", favouritesData);
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("limit", limit);
            pagination.put("offset", offset);
            pagination.put("count", favouritesData.size());
            response.put("pagination", pagination);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to retrieve lesson plan favourites: " + e.getMessage()));
        }
    }

    @PostMapping("/favourites/activities")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Save activity favourite", description = "Save an activity as favourite")
    public ResponseEntity<?> saveActivityFavourite(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            UUID activityId;
            try {
                activityId = UUID.fromString(requestBody.get("activity_id").toString());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("Invalid activity_id format: must be a valid UUID"));
            }
            String name = (String) requestBody.get("name");

            UserFavourites favourite = favouritesService.saveActivityFavourite(userId, activityId, name);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Activity favourite saved successfully");
            response.put("favourite_id", favourite.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to save activity favourite: " + e.getMessage()));
        }
    }

    @PostMapping("/favourites/lesson-plans")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Save lesson plan favourite", description = "Save a lesson plan as favourite")
    public ResponseEntity<?> saveLessonPlanFavourite(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            @SuppressWarnings("unchecked")
            List<UUID> activityIds;
            try {
                activityIds = ((List<Object>) requestBody.get("activity_ids")).stream()
                        .map(id -> {
                            try {
                                return UUID.fromString(id.toString());
                            } catch (IllegalArgumentException e) {
                                throw new RuntimeException("Invalid activity_id in list: " + id + ". Must be a valid UUID");
                            }
                        })
                        .collect(Collectors.toList());
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
            }
            String name = (String) requestBody.get("name");
            String lessonPlanSnapshot = objectMapper.writeValueAsString(requestBody.get("lesson_plan_snapshot"));

            UserFavourites favourite = favouritesService.saveLessonPlanFavourite(userId, activityIds,
                    lessonPlanSnapshot, name);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lesson plan favourite saved successfully");
            response.put("favourite_id", favourite.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to save lesson plan favourite: " + e.getMessage()));
        }
    }

    @DeleteMapping("/favourites/{favouriteId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete favourite", description = "Delete a favourite (activity or lesson plan)")
    public ResponseEntity<?> deleteFavourite(
            @PathVariable UUID favouriteId,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            boolean deleted = favouritesService.deleteFavourite(favouriteId, userId);
            if (!deleted) {
                return ResponseEntity.status(404).body(ErrorResponse.of("Favourite not found"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Favourite deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of("Failed to delete favourite: " + e.getMessage()));
        }
    }

    @DeleteMapping("/favourites/activities/{activityId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove activity favourite", description = "Remove an activity from favourites")
    public ResponseEntity<?> removeActivityFavourite(
            @PathVariable UUID activityId,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            boolean deleted = favouritesService.deleteActivityFavourite(userId, activityId);
            if (!deleted) {
                return ResponseEntity.status(404).body(ErrorResponse.of("Activity favourite not found"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Activity favourite removed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to remove activity favourite: " + e.getMessage()));
        }
    }

    @GetMapping("/favourites/activities/{activityId}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check activity favourite status", description = "Check if an activity is favourited by the user")
    public ResponseEntity<?> checkActivityFavouriteStatus(
            @PathVariable UUID activityId,
            HttpServletRequest request) {
        try {
            UUID userId = (UUID) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }

            boolean isFavourited = favouritesService.isActivityFavourited(userId, activityId);

            Map<String, Boolean> response = new HashMap<>();
            response.put("is_favourited", isFavourited);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to check activity favourite status: " + e.getMessage()));
        }
    }
}
