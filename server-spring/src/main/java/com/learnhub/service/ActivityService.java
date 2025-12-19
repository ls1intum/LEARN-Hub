package com.learnhub.service;

import com.learnhub.dto.response.ActivityResponse;
import com.learnhub.model.Activity;
import com.learnhub.model.enums.*;
import com.learnhub.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
            List<String> formats, List<String> bloomLevels, Integer mentalLoad, Integer physicalEnergy,
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
        
        if (mentalLoad != null) {
            // Convert Integer (1, 2, 3) to EnergyLevel enum (LOW, MEDIUM, HIGH)
            EnergyLevel energyLevel = convertIntegerToEnergyLevel(mentalLoad);
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("mentalLoad"), energyLevel));
        }
        
        if (physicalEnergy != null) {
            // Convert Integer (1, 2, 3) to EnergyLevel enum (LOW, MEDIUM, HIGH)
            EnergyLevel energyLevel = convertIntegerToEnergyLevel(physicalEnergy);
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("physicalEnergy"), energyLevel));
        }
        
        int pageSize = (limit != null && limit > 0) ? limit : Integer.MAX_VALUE;
        int pageNumber = (offset != null && offset > 0) ? offset / pageSize : 0;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        Page<Activity> page = activityRepository.findAll(spec, pageable);
        return page.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private EnergyLevel convertIntegerToEnergyLevel(Integer value) {
        switch (value) {
            case 1:
                return EnergyLevel.LOW;
            case 2:
                return EnergyLevel.MEDIUM;
            case 3:
                return EnergyLevel.HIGH;
            default:
                throw new IllegalArgumentException("Invalid energy level: " + value + ". Must be 1 (low), 2 (medium), or 3 (high)");
        }
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

    public Activity createActivityFromMap(Map<String, Object> data) {
        Activity activity = new Activity();
        
        if (data.get("name") != null) activity.setName(data.get("name").toString());
        if (data.get("description") != null) activity.setDescription(data.get("description").toString());
        if (data.get("source") != null) activity.setSource(data.get("source").toString());
        
        if (data.get("age_min") != null) {
            activity.setAgeMin(Integer.parseInt(data.get("age_min").toString()));
        }
        if (data.get("age_max") != null) {
            activity.setAgeMax(Integer.parseInt(data.get("age_max").toString()));
        }
        
        if (data.get("format") != null) {
            activity.setFormat(ActivityFormat.fromValue(data.get("format").toString()));
        }
        if (data.get("bloom_level") != null) {
            activity.setBloomLevel(BloomLevel.fromValue(data.get("bloom_level").toString()));
        }
        
        if (data.get("duration_min_minutes") != null) {
            activity.setDurationMinMinutes(Integer.parseInt(data.get("duration_min_minutes").toString()));
        }
        if (data.get("duration_max_minutes") != null) {
            activity.setDurationMaxMinutes(Integer.parseInt(data.get("duration_max_minutes").toString()));
        }
        
        if (data.get("mental_load") != null) {
            activity.setMentalLoad(EnergyLevel.fromValue(data.get("mental_load").toString()));
        }
        if (data.get("physical_energy") != null) {
            activity.setPhysicalEnergy(EnergyLevel.fromValue(data.get("physical_energy").toString()));
        }
        
        if (data.get("prep_time_minutes") != null) {
            activity.setPrepTimeMinutes(Integer.parseInt(data.get("prep_time_minutes").toString()));
        }
        if (data.get("cleanup_time_minutes") != null) {
            activity.setCleanupTimeMinutes(Integer.parseInt(data.get("cleanup_time_minutes").toString()));
        }
        
        if (data.get("resources_needed") != null) {
            activity.setResourcesNeeded((List<String>) data.get("resources_needed"));
        }
        if (data.get("topics") != null) {
            activity.setTopics((List<String>) data.get("topics"));
        }
        
        if (data.get("document_id") != null) {
            activity.setDocumentId(Long.parseLong(data.get("document_id").toString()));
        }
        
        return activity;
    }

    public ActivityResponse convertToResponse(Activity activity) {
        return mapToResponse(activity);
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
