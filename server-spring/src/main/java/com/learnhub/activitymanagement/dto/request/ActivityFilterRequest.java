package com.learnhub.activitymanagement.dto.request;

import org.springframework.web.bind.annotation.BindParam;

import java.util.List;

public record ActivityFilterRequest(
        String name,
        @BindParam("age_min") Integer ageMin,
        @BindParam("age_max") Integer ageMax,
        List<String> format,
        @BindParam("bloom_level") List<String> bloomLevel,
        @BindParam("mental_load") String mentalLoad,
        @BindParam("physical_energy") String physicalEnergy,
        @BindParam("resources_needed") List<String> resourcesNeeded,
        List<String> topics,
        Integer limit,
        Integer offset) {

    public ActivityFilterRequest {
        if (limit == null) limit = 100;
        if (offset == null) offset = 0;
    }
}
