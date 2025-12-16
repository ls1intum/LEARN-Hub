package com.learnhub.service;

import com.learnhub.dto.response.ActivityResponse;
import com.learnhub.model.Activity;
import com.learnhub.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    public List<ActivityResponse> getAllActivities() {
        return activityRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<ActivityResponse> getActivitiesWithFilters(
            String name, Integer ageMin, Integer ageMax, 
            List<String> formats, List<String> bloomLevels,
            Integer limit, Integer offset) {
        
        Specification<Activity> spec = Specification.where(null);
        
        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        
        if (ageMin != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("ageMin"), ageMin));
        }
        
        if (ageMax != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("ageMax"), ageMax));
        }
        
        if (formats != null && !formats.isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                root.get("format").as(String.class).in(formats));
        }
        
        if (bloomLevels != null && !bloomLevels.isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                root.get("bloomLevel").as(String.class).in(bloomLevels));
        }
        
        int pageSize = (limit != null && limit > 0) ? limit : Integer.MAX_VALUE;
        int pageNumber = (offset != null && offset > 0) ? offset / pageSize : 0;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        Page<Activity> page = activityRepository.findAll(spec, pageable);
        return page.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public ActivityResponse getActivityById(Long id) {
        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Activity not found"));
        return mapToResponse(activity);
    }

    public ActivityResponse createActivity(Activity activity) {
        Activity saved = activityRepository.save(activity);
        return mapToResponse(saved);
    }

    public ActivityResponse updateActivity(Long id, Activity activityUpdate) {
        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Activity not found"));
        
        // Update fields
        activity.setName(activityUpdate.getName());
        activity.setDescription(activityUpdate.getDescription());
        activity.setSource(activityUpdate.getSource());
        activity.setAgeMin(activityUpdate.getAgeMin());
        activity.setAgeMax(activityUpdate.getAgeMax());
        activity.setFormat(activityUpdate.getFormat());
        activity.setBloomLevel(activityUpdate.getBloomLevel());
        activity.setDurationMinMinutes(activityUpdate.getDurationMinMinutes());
        activity.setDurationMaxMinutes(activityUpdate.getDurationMaxMinutes());
        activity.setMentalLoad(activityUpdate.getMentalLoad());
        activity.setPhysicalEnergy(activityUpdate.getPhysicalEnergy());
        activity.setPrepTimeMinutes(activityUpdate.getPrepTimeMinutes());
        activity.setCleanupTimeMinutes(activityUpdate.getCleanupTimeMinutes());
        activity.setResourcesNeeded(activityUpdate.getResourcesNeeded());
        activity.setTopics(activityUpdate.getTopics());
        
        Activity saved = activityRepository.save(activity);
        return mapToResponse(saved);
    }

    public void deleteActivity(Long id) {
        activityRepository.deleteById(id);
    }

    private ActivityResponse mapToResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setName(activity.getName());
        response.setDescription(activity.getDescription());
        response.setSource(activity.getSource());
        response.setAgeMin(activity.getAgeMin());
        response.setAgeMax(activity.getAgeMax());
        response.setFormat(activity.getFormat() != null ? activity.getFormat().getValue() : null);
        response.setBloomLevel(activity.getBloomLevel() != null ? activity.getBloomLevel().getValue() : null);
        response.setDurationMinMinutes(activity.getDurationMinMinutes());
        response.setDurationMaxMinutes(activity.getDurationMaxMinutes());
        response.setMentalLoad(activity.getMentalLoad() != null ? activity.getMentalLoad().getValue() : null);
        response.setPhysicalEnergy(activity.getPhysicalEnergy() != null ? activity.getPhysicalEnergy().getValue() : null);
        response.setPrepTimeMinutes(activity.getPrepTimeMinutes());
        response.setCleanupTimeMinutes(activity.getCleanupTimeMinutes());
        response.setResourcesNeeded(activity.getResourcesNeeded());
        response.setTopics(activity.getTopics());
        response.setDocumentId(activity.getDocumentId());
        return response;
    }
}
