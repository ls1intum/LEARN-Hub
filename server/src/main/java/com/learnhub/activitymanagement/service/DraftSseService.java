package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.entity.enums.ActivityStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class DraftSseService {

	private static final Logger logger = LoggerFactory.getLogger(DraftSseService.class);
	private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	public SseEmitter createEmitter() {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		emitters.add(emitter);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));
		logger.debug("SSE emitter created, total active: {}", emitters.size());
		return emitter;
	}

	public void sendDraftUpdate(UUID activityId, ActivityStatus status, String generationError) {
		Map<String, Object> payload = Map.of("id", activityId.toString(), "status", status.name(), "generationError",
				generationError != null ? generationError : "");
		List<SseEmitter> dead = new ArrayList<>();
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().name("draft-update").data(payload, MediaType.APPLICATION_JSON));
			} catch (Exception e) {
				dead.add(emitter);
			}
		}
		emitters.removeAll(dead);
		logger.debug("Sent draft-update SSE for activity {} (status={}), notified {} client(s)", activityId, status,
				emitters.size() - dead.size());
	}
}
