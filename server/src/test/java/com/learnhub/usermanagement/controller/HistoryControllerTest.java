package com.learnhub.usermanagement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.activitymanagement.service.ActivityService;
import com.learnhub.security.AuthenticatedUser;
import com.learnhub.usermanagement.entity.UserFavourites;
import com.learnhub.usermanagement.service.UserFavouritesService;
import com.learnhub.usermanagement.service.UserSearchHistoryService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HistoryControllerTest {

	private MockMvc mockMvc;
	private HistoryController controller;
	private UserSearchHistoryService searchHistoryService;
	private UserFavouritesService favouritesService;
	private ActivityService activityService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		controller = new HistoryController();
		searchHistoryService = mock(UserSearchHistoryService.class);
		favouritesService = mock(UserFavouritesService.class);
		activityService = mock(ActivityService.class);
		ReflectionTestUtils.setField(controller, "searchHistoryService", searchHistoryService);
		ReflectionTestUtils.setField(controller, "favouritesService", favouritesService);
		ReflectionTestUtils.setField(controller, "activityService", activityService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	private Authentication principal(UUID userId) {
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "teacher@example.com", "TEACHER");
		return new UsernamePasswordAuthenticationToken(authenticatedUser, null,
				List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
	}

	// ─── search history ─────────────────────────────────────────────

	@Test
	void getSearchHistoryReturns200WithPagination() throws Exception {
		UUID userId = UUID.randomUUID();
		Page<com.learnhub.usermanagement.entity.UserSearchHistory> page = new PageImpl<>(List.of());
		when(searchHistoryService.getUserSearchHistory(eq(userId), anyInt(), anyInt())).thenReturn(page);

		mockMvc.perform(
				get("/api/history/search").param("limit", "10").param("offset", "0").principal(principal(userId)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.searchHistory").isArray());
	}

	@Test
	void getSearchHistoryReturns401WhenNoPrincipal() throws Exception {
		mockMvc.perform(get("/api/history/search")).andExpect(status().isUnauthorized());
	}

	@Test
	void deleteSearchHistoryReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID historyId = UUID.randomUUID();
		when(searchHistoryService.deleteSearchHistory(historyId, userId)).thenReturn(true);

		mockMvc.perform(delete("/api/history/search/" + historyId).principal(principal(userId)))
				.andExpect(status().isOk());
	}

	@Test
	void deleteSearchHistoryReturns404WhenMissing() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID historyId = UUID.randomUUID();
		when(searchHistoryService.deleteSearchHistory(historyId, userId)).thenReturn(false);

		mockMvc.perform(delete("/api/history/search/" + historyId).principal(principal(userId)))
				.andExpect(status().isNotFound());
	}

	// ─── activity favourites ────────────────────────────────────────

	@Test
	void getActivityFavouritesReturns200WithoutFilters() throws Exception {
		UUID userId = UUID.randomUUID();
		when(favouritesService.getActivityFavouritesPage(eq(userId), anyInt(), anyInt()))
				.thenReturn(new PageImpl<>(List.of()));

		mockMvc.perform(get("/api/history/favourites/activities").principal(principal(userId)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.favourites").isArray());
	}

	@Test
	void getActivityFavouritesReturns401WhenNoPrincipal() throws Exception {
		mockMvc.perform(get("/api/history/favourites/activities")).andExpect(status().isUnauthorized());
	}

	@Test
	void getLessonPlanFavouritesReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		when(favouritesService.getLessonPlanFavouritesPage(eq(userId), anyInt(), anyInt()))
				.thenReturn(new PageImpl<>(List.of()));

		mockMvc.perform(get("/api/history/favourites/lesson-plans").principal(principal(userId)))
				.andExpect(status().isOk());
	}

	// ─── save favourites ────────────────────────────────────────────

	@Test
	void saveActivityFavouriteReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		UserFavourites saved = mock(UserFavourites.class);
		when(saved.getId()).thenReturn(UUID.randomUUID());
		when(favouritesService.saveActivityFavourite(eq(userId), eq(activityId), any())).thenReturn(saved);

		mockMvc.perform(
				post("/api/history/favourites/activities").principal(principal(userId)).contentType("application/json")
						.content(objectMapper.writeValueAsString(Map.of("activityId", activityId.toString()))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.message").exists());
	}

	@Test
	void saveActivityFavouriteReturns400WhenActivityIdMissing() throws Exception {
		UUID userId = UUID.randomUUID();

		mockMvc.perform(post("/api/history/favourites/activities").principal(principal(userId))
				.contentType("application/json").content("{}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("activityId is required"));
	}

	@Test
	void saveActivityFavouriteReturns401WhenNoPrincipal() throws Exception {
		mockMvc.perform(post("/api/history/favourites/activities").contentType("application/json").content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void saveLessonPlanFavouriteReturns400WhenActivityIdsMissing() throws Exception {
		UUID userId = UUID.randomUUID();

		mockMvc.perform(post("/api/history/favourites/lesson-plans").principal(principal(userId))
				.contentType("application/json").content("{}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("activityIds is required"));
	}

	@Test
	void saveLessonPlanFavouriteReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		UserFavourites saved = mock(UserFavourites.class);
		when(saved.getId()).thenReturn(UUID.randomUUID());
		when(favouritesService.saveLessonPlanFavourite(eq(userId), any(), any(), any())).thenReturn(saved);

		mockMvc.perform(post("/api/history/favourites/lesson-plans").principal(principal(userId))
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("activityIds", List.of(activityId.toString())))))
				.andExpect(status().isOk());
	}

	// ─── delete favourites ──────────────────────────────────────────

	@Test
	void deleteFavouriteReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID favouriteId = UUID.randomUUID();
		when(favouritesService.deleteFavourite(favouriteId, userId)).thenReturn(true);

		mockMvc.perform(delete("/api/history/favourites/" + favouriteId).principal(principal(userId)))
				.andExpect(status().isOk());
	}

	@Test
	void deleteFavouriteReturns404WhenMissing() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID favouriteId = UUID.randomUUID();
		when(favouritesService.deleteFavourite(favouriteId, userId)).thenReturn(false);

		mockMvc.perform(delete("/api/history/favourites/" + favouriteId).principal(principal(userId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void removeActivityFavouriteReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		when(favouritesService.deleteActivityFavourite(userId, activityId)).thenReturn(true);

		mockMvc.perform(delete("/api/history/favourites/activities/" + activityId).principal(principal(userId)))
				.andExpect(status().isOk());
	}

	@Test
	void removeActivityFavouriteReturns404WhenMissing() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID activityId = UUID.randomUUID();
		when(favouritesService.deleteActivityFavourite(userId, activityId)).thenReturn(false);

		mockMvc.perform(delete("/api/history/favourites/activities/" + activityId).principal(principal(userId)))
				.andExpect(status().isNotFound());
	}
}
