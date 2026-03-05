package com.learnhub.activitymanagement.dto.request;

import java.util.List;
import org.springframework.web.bind.annotation.BindParam;

public record ActivityFilterRequest(
        String name,
        @BindParam("age_min") Integer ageMin,
        @BindParam("age_max") Integer ageMax,
        @BindParam("duration_min") Integer durationMin,
        @BindParam("duration_max") Integer durationMax,
        List<String> format,
        @BindParam("bloom_level") List<String> bloomLevel,
        @BindParam("mental_load") String mentalLoad,
        @BindParam("physical_energy") String physicalEnergy,
        @BindParam("resources_needed") List<String> resourcesNeeded,
        List<String> topics,
        Integer limit,
        Integer offset) {

    public ActivityFilterRequest {
        if (limit == null) limit = 10;
        if (offset == null) offset = 0;
    }
}
