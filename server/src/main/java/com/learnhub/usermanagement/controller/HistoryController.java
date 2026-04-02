package com.learnhub.usermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.dto.response.MessageResponse;
import com.learnhub.usermanagement.dto.request.ActivityFavouriteRequest;
import com.learnhub.usermanagement.dto.request.LessonPlanFavouriteRequest;
import com.learnhub.usermanagement.dto.response.ActivityFavouriteItemResponse;
import com.learnhub.usermanagement.dto.response.ActivityFavouritesListResponse;
import com.learnhub.usermanagement.dto.response.FavouriteSaveResponse;
import com.learnhub.usermanagement.dto.response.FavouriteStatusResponse;
import com.learnhub.usermanagement.dto.response.LessonPlanDataResponse;
import com.learnhub.usermanagement.dto.response.LessonPlanFavouriteItemResponse;
import com.learnhub.usermanagement.dto.response.LessonPlanFavouritesListResponse;
import com.learnhub.usermanagement.dto.response.PaginationResponse;
import com.learnhub.usermanagement.dto.response.SearchHistoryEntryResponse;
import com.learnhub.usermanagement.dto.response.SearchHistoryListResponse;
import com.learnhub.usermanagement.entity.UserFavourites;
import com.learnhub.usermanagement.entity.UserSearchHistory;
import com.learnhub.usermanagement.service.UserFavouritesService;
import com.learnhub.usermanagement.service.UserSearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@Tag(name = "History", description = "User history and favourites management")
@SecurityRequirement(name = "BearerAuth")
public class HistoryController {

	private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

	@Autowired
	private UserSearchHistoryService searchHistoryService;

	@Autowired
	private UserFavouritesService favouritesService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@GetMapping("/search")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get search history", description = "Get user's search history")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Search history", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SearchHistoryListResponse.class))) })
	public ResponseEntity<?> getSearchHistory(@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "0") Integer offset, HttpServletRequest request) {
		logger.info("GET /api/history/search - Get search history called with limit={}, offset={}", limit, offset);
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("GET /api/history/search - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			List<UserSearchHistory> history = searchHistoryService.getUserSearchHistory(userId, limit, offset);

			List<SearchHistoryEntryResponse> historyData = history.stream().map(entry -> {
				try {
					return new SearchHistoryEntryResponse(entry.getId(),
							objectMapper.readValue(entry.getSearchCriteria(), Map.class), entry.getCreatedAt().toString());
				} catch (Exception e) {
					return new SearchHistoryEntryResponse(entry.getId(), Collections.emptyMap(),
							entry.getCreatedAt().toString());
				}
			}).collect(Collectors.toList());

			return ResponseEntity.ok(
					new SearchHistoryListResponse(historyData, new PaginationResponse(limit, offset, historyData.size())));
		} catch (Exception e) {
			logger.error("GET /api/history/search - Failed to retrieve search history: {}", e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to retrieve search history: " + e.getMessage()));
		}
	}

	@DeleteMapping("/search/{historyId}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Delete search history entry", description = "Delete a specific search history entry")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Delete confirmation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))) })
	public ResponseEntity<?> deleteSearchHistory(@PathVariable UUID historyId, HttpServletRequest request) {
		logger.info("DELETE /api/history/search/{} - Delete search history entry called", historyId);
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("DELETE /api/history/search/{} - Unauthorized: userId not found in request", historyId);
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			boolean deleted = searchHistoryService.deleteSearchHistory(historyId, userId);
			if (!deleted) {
				logger.error("DELETE /api/history/search/{} - Search history entry not found", historyId);
				return ResponseEntity.status(404).body(ErrorResponse.of("Search history entry not found"));
			}

			logger.info("DELETE /api/history/search/{} - Search history entry deleted successfully", historyId);
			return ResponseEntity.ok(MessageResponse.of("Search history entry deleted successfully"));
		} catch (Exception e) {
			logger.error("DELETE /api/history/search/{} - Failed to delete search history: {}", historyId,
					e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to delete search history: " + e.getMessage()));
		}
	}

	@GetMapping("/favourites/activities")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get activity favourites", description = "Get user's favourite activities")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Activity favourites", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActivityFavouritesListResponse.class))) })
	public ResponseEntity<?> getActivityFavourites(@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "0") Integer offset, HttpServletRequest request) {
		logger.info("GET /api/history/favourites/activities - Get activity favourites called with limit={}, offset={}",
				limit, offset);
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("GET /api/history/favourites/activities - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			List<UserFavourites> favourites = favouritesService.getUserFavourites(userId, "activity");

			List<ActivityFavouriteItemResponse> favouritesData = favourites.stream()
					.map(fav -> new ActivityFavouriteItemResponse(fav.getId(), fav.getFavouriteType(), fav.getActivityId(),
							fav.getName(), fav.getCreatedAt().toString()))
					.collect(Collectors.toList());

			return ResponseEntity.ok(new ActivityFavouritesListResponse(favouritesData,
					new PaginationResponse(limit, offset, favouritesData.size())));
		} catch (Exception e) {
			logger.error("GET /api/history/favourites/activities - Failed to retrieve activity favourites: {}",
					e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to retrieve activity favourites: " + e.getMessage()));
		}
	}

	@GetMapping("/favourites/lesson-plans")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get lesson plan favourites", description = "Get user's favourite lesson plans")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lesson plan favourites", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LessonPlanFavouritesListResponse.class))) })
	public ResponseEntity<?> getLessonPlanFavourites(@RequestParam(required = false, defaultValue = "50") Integer limit,
			@RequestParam(required = false, defaultValue = "0") Integer offset, HttpServletRequest request) {
		logger.info(
				"GET /api/history/favourites/lesson-plans - Get lesson plan favourites called with limit={}, offset={}",
				limit, offset);
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("GET /api/history/favourites/lesson-plans - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			List<UserFavourites> favourites = favouritesService.getUserFavourites(userId, "lesson_plan");

			List<LessonPlanFavouriteItemResponse> favouritesData = favourites.stream().map(fav -> {
				List<UUID> activityIds = Collections.emptyList();
				LessonPlanDataResponse lessonPlan = null;
				try {
					if (fav.getActivityIds() != null) {
						activityIds = objectMapper.readValue(fav.getActivityIds(),
								objectMapper.getTypeFactory().constructCollectionType(List.class, UUID.class));
					}
					if (fav.getLessonPlanSnapshot() != null) {
						lessonPlan = objectMapper.readValue(fav.getLessonPlanSnapshot(), LessonPlanDataResponse.class);
					}
				} catch (Exception e) {
					logger.warn("Failed to parse lesson plan favourite payload {}: {}", fav.getId(), e.getMessage());
				}
				return new LessonPlanFavouriteItemResponse(fav.getId(), fav.getFavouriteType(), fav.getName(),
						activityIds, lessonPlan, fav.getCreatedAt().toString());
			}).collect(Collectors.toList());

			return ResponseEntity.ok(new LessonPlanFavouritesListResponse(favouritesData,
					new PaginationResponse(limit, offset, favouritesData.size())));
		} catch (Exception e) {
			logger.error("GET /api/history/favourites/lesson-plans - Failed to retrieve lesson plan favourites: {}",
					e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to retrieve lesson plan favourites: " + e.getMessage()));
		}
	}

	@PostMapping("/favourites/activities")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Save activity favourite", description = "Save an activity as favourite")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Favourite saved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FavouriteSaveResponse.class))) })
	public ResponseEntity<?> saveActivityFavourite(@RequestBody ActivityFavouriteRequest requestBody,
			HttpServletRequest request) {
		logger.info("POST /api/history/favourites/activities - Save activity favourite called");
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("POST /api/history/favourites/activities - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}
			UUID activityId = requestBody.getActivityId();
			if (activityId == null) {
				logger.error("POST /api/history/favourites/activities - Missing activityId");
				return ResponseEntity.badRequest().body(ErrorResponse.of("activityId is required"));
			}
			String name = requestBody.getName();

			UserFavourites favourite = favouritesService.saveActivityFavourite(userId, activityId, name);

			logger.info("POST /api/history/favourites/activities - Activity favourite saved with id={}",
					favourite.getId());
			return ResponseEntity.ok(new FavouriteSaveResponse("Activity favourite saved successfully", favourite.getId()));
		} catch (Exception e) {
			logger.error("POST /api/history/favourites/activities - Failed to save activity favourite: {}",
					e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to save activity favourite: " + e.getMessage()));
		}
	}

	@PostMapping("/favourites/lesson-plans")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Save lesson plan favourite", description = "Save a lesson plan as favourite")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Favourite saved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FavouriteSaveResponse.class))) })
	public ResponseEntity<?> saveLessonPlanFavourite(@RequestBody LessonPlanFavouriteRequest requestBody,
			HttpServletRequest request) {
		logger.info("POST /api/history/favourites/lesson-plans - Save lesson plan favourite called");
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("POST /api/history/favourites/lesson-plans - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			List<UUID> activityIds = requestBody.getActivityIds();
			if (activityIds == null || activityIds.isEmpty()) {
				logger.error("POST /api/history/favourites/lesson-plans - Missing activityIds");
				return ResponseEntity.badRequest().body(ErrorResponse.of("activityIds is required"));
			}
			String name = requestBody.getName();
			String lessonPlanSnapshot = objectMapper.writeValueAsString(requestBody.getLessonPlan());

			UserFavourites favourite = favouritesService.saveLessonPlanFavourite(userId, activityIds,
					lessonPlanSnapshot, name);

			logger.info("POST /api/history/favourites/lesson-plans - Lesson plan favourite saved with id={}",
					favourite.getId());
			return ResponseEntity
					.ok(new FavouriteSaveResponse("Lesson plan favourite saved successfully", favourite.getId()));
		} catch (Exception e) {
			logger.error("POST /api/history/favourites/lesson-plans - Failed to save lesson plan favourite: {}",
					e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to save lesson plan favourite: " + e.getMessage()));
		}
	}

	@DeleteMapping("/favourites/{favouriteId}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Delete favourite", description = "Delete a favourite (activity or lesson plan)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Delete confirmation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))) })
	public ResponseEntity<?> deleteFavourite(@PathVariable UUID favouriteId, HttpServletRequest request) {
		logger.info("DELETE /api/history/favourites/{} - Delete favourite called", favouriteId);
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("DELETE /api/history/favourites/{} - Unauthorized: userId not found in request",
						favouriteId);
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			boolean deleted = favouritesService.deleteFavourite(favouriteId, userId);
			if (!deleted) {
				logger.error("DELETE /api/history/favourites/{} - Favourite not found", favouriteId);
				return ResponseEntity.status(404).body(ErrorResponse.of("Favourite not found"));
			}

			logger.info("DELETE /api/history/favourites/{} - Favourite deleted successfully", favouriteId);
			return ResponseEntity.ok(MessageResponse.of("Favourite deleted successfully"));
		} catch (Exception e) {
			logger.error("DELETE /api/history/favourites/{} - Failed to delete favourite: {}", favouriteId,
					e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to delete favourite: " + e.getMessage()));
		}
	}

	@DeleteMapping("/favourites/activities/{activityId}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Remove activity favourite", description = "Remove an activity from favourites")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Delete confirmation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))) })
	public ResponseEntity<?> removeActivityFavourite(@PathVariable UUID activityId, HttpServletRequest request) {
		logger.info("DELETE /api/history/favourites/activities/{} - Remove activity favourite called", activityId);
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error("DELETE /api/history/favourites/activities/{} - Unauthorized: userId not found in request",
						activityId);
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			boolean deleted = favouritesService.deleteActivityFavourite(userId, activityId);
			if (!deleted) {
				logger.error("DELETE /api/history/favourites/activities/{} - Activity favourite not found", activityId);
				return ResponseEntity.status(404).body(ErrorResponse.of("Activity favourite not found"));
			}

			logger.info("DELETE /api/history/favourites/activities/{} - Activity favourite removed successfully",
					activityId);
			return ResponseEntity.ok(MessageResponse.of("Activity favourite removed successfully"));
		} catch (Exception e) {
			logger.error("DELETE /api/history/favourites/activities/{} - Failed to remove activity favourite: {}",
					activityId, e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to remove activity favourite: " + e.getMessage()));
		}
	}

	@GetMapping("/favourites/activities/{activityId}/status")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Check activity favourite status", description = "Check if an activity is favourited by the user")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Favourite status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FavouriteStatusResponse.class))) })
	public ResponseEntity<?> checkActivityFavouriteStatus(@PathVariable UUID activityId, HttpServletRequest request) {
		logger.info("GET /api/history/favourites/activities/{}/status - Check activity favourite status called",
				activityId);
		try {
			UUID userId = (UUID) request.getAttribute("userId");
			if (userId == null) {
				logger.error(
						"GET /api/history/favourites/activities/{}/status - Unauthorized: userId not found in request",
						activityId);
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}

			boolean isFavourited = favouritesService.isActivityFavourited(userId, activityId);

			return ResponseEntity.ok(new FavouriteStatusResponse(isFavourited));
		} catch (Exception e) {
			logger.error(
					"GET /api/history/favourites/activities/{}/status - Failed to check activity favourite status: {}",
					activityId, e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to check activity favourite status: " + e.getMessage()));
		}
	}
}
