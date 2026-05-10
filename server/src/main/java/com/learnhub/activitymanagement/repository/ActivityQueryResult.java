package com.learnhub.activitymanagement.repository;

import com.learnhub.activitymanagement.entity.Activity;
import java.util.List;

public record ActivityQueryResult(List<Activity> activities, long total) {
}
