package com.learnhub.activitymanagement.repository;

import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.ActivityStatus;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.EnergyLevel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepositoryImpl implements ActivityRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public ActivityQueryResult findPublishedActivitiesWithFilters(String name, Integer ageMin, Integer ageMax,
			Integer durationMin, Integer durationMax, List<String> formats, List<String> bloomLevels,
			List<String> mentalLoads, List<String> physicalEnergies, List<String> resourcesNeeded, List<String> topics,
			Integer limit, Integer offset) {
		QueryParts queryParts = buildQueryParts(name, ageMin, ageMax, durationMin, durationMax, formats, bloomLevels,
				mentalLoads, physicalEnergies, resourcesNeeded, topics, null);

		String countSql = "SELECT COUNT(*) FROM activities a" + queryParts.whereClause();
		Query countQuery = entityManager.createNativeQuery(countSql);
		applyParameters(countQuery, queryParts.parameters());
		long total = ((Number) countQuery.getSingleResult()).longValue();
		if (total == 0) {
			return new ActivityQueryResult(List.of(), 0);
		}

		String idsSql = "SELECT a.id FROM activities a" + queryParts.whereClause()
				+ " ORDER BY a.created_at DESC, a.id DESC";
		Query idsQuery = entityManager.createNativeQuery(idsSql);
		applyParameters(idsQuery, queryParts.parameters());
		int resolvedOffset = Math.max(offset != null ? offset : 0, 0);
		int resolvedLimit = resolveLimit(limit);
		idsQuery.setFirstResult(resolvedOffset);
		idsQuery.setMaxResults(resolvedLimit);

		@SuppressWarnings("unchecked")
		List<UUID> ids = idsQuery.getResultList();
		if (ids.isEmpty()) {
			return new ActivityQueryResult(List.of(), total);
		}

		return new ActivityQueryResult(loadActivitiesInRequestedOrder(ids), total);
	}

	@Override
	public List<UUID> findPublishedActivityIdsByFilters(String name, Integer ageMin, Integer ageMax,
			Integer durationMin, Integer durationMax, List<String> formats, List<String> bloomLevels,
			List<String> mentalLoads, List<String> physicalEnergies, List<String> resourcesNeeded, List<String> topics,
			List<UUID> restrictedActivityIds) {
		if (restrictedActivityIds != null && restrictedActivityIds.isEmpty()) {
			return List.of();
		}

		QueryParts queryParts = buildQueryParts(name, ageMin, ageMax, durationMin, durationMax, formats, bloomLevels,
				mentalLoads, physicalEnergies, resourcesNeeded, topics, restrictedActivityIds);
		Query idsQuery = entityManager.createNativeQuery("SELECT a.id FROM activities a" + queryParts.whereClause());
		applyParameters(idsQuery, queryParts.parameters());

		@SuppressWarnings("unchecked")
		List<UUID> ids = idsQuery.getResultList();
		return ids;
	}

	private List<Activity> loadActivitiesInRequestedOrder(List<UUID> ids) {
		TypedQuery<Activity> activitiesQuery = entityManager
				.createQuery("SELECT DISTINCT a FROM Activity a WHERE a.id IN :ids", Activity.class);
		activitiesQuery.setParameter("ids", ids);

		Map<UUID, Activity> byId = activitiesQuery.getResultList().stream().collect(
				Collectors.toMap(Activity::getId, activity -> activity, (left, right) -> left, LinkedHashMap::new));

		return ids.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private QueryParts buildQueryParts(String name, Integer ageMin, Integer ageMax, Integer durationMin,
			Integer durationMax, List<String> formats, List<String> bloomLevels, List<String> mentalLoads,
			List<String> physicalEnergies, List<String> resourcesNeeded, List<String> topics,
			List<UUID> restrictedActivityIds) {
		StringBuilder whereClause = new StringBuilder(" WHERE a.status = :status");
		Map<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("status", ActivityStatus.PUBLISHED.name());

		if (restrictedActivityIds != null) {
			appendInClause(whereClause, parameters, "a.id", "activityId", restrictedActivityIds, true);
		}

		if (name != null && !name.isBlank()) {
			whereClause.append(" AND LOWER(a.name) LIKE :name");
			parameters.put("name", "%" + name.toLowerCase(Locale.ROOT) + "%");
		}

		if (ageMin != null) {
			whereClause.append(" AND a.age_min >= :ageMin");
			parameters.put("ageMin", ageMin);
		}

		if (ageMax != null) {
			whereClause.append(" AND a.age_max <= :ageMax");
			parameters.put("ageMax", ageMax);
		}

		if (durationMin != null) {
			whereClause.append(" AND a.duration_min_minutes >= :durationMin");
			parameters.put("durationMin", durationMin);
		}

		if (durationMax != null) {
			whereClause.append(" AND a.duration_max_minutes <= :durationMax");
			parameters.put("durationMax", durationMax);
		}

		appendEnumInClause(whereClause, parameters, "a.format", "format", convertFormats(formats));
		appendEnumInClause(whereClause, parameters, "a.bloom_level", "bloomLevel", convertBloomLevels(bloomLevels));
		appendEnumInClause(whereClause, parameters, "a.mental_load", "mentalLoad", toEnergyLevelNames(mentalLoads));
		appendEnumInClause(whereClause, parameters, "a.physical_energy", "physicalEnergy",
				toEnergyLevelNames(physicalEnergies));

		appendJsonArrayFilter(whereClause, parameters, "a.resources_needed", "resource", resourcesNeeded);
		appendJsonArrayFilter(whereClause, parameters, "a.topics", "topic", topics);

		return new QueryParts(whereClause.toString(), parameters);
	}

	private List<String> convertFormats(List<String> values) {
		if (values == null) {
			return List.of();
		}

		return values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).map(ActivityFormat::fromValue)
				.map(Enum::name).collect(Collectors.toList());
	}

	private List<String> convertBloomLevels(List<String> values) {
		if (values == null) {
			return List.of();
		}

		return values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).map(BloomLevel::fromValue)
				.map(Enum::name).collect(Collectors.toList());
	}

	private void appendEnumInClause(StringBuilder whereClause, Map<String, Object> parameters, String column,
			String prefix, List<String> convertedValues) {
		if (convertedValues == null || convertedValues.isEmpty()) {
			return;
		}
		appendInClause(whereClause, parameters, column, prefix, convertedValues, true);
	}

	private List<String> toEnergyLevelNames(List<String> values) {
		if (values == null) {
			return List.of();
		}

		return values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank())
				.map(this::convertToEnergyLevel).map(Enum::name).collect(Collectors.toList());
	}

	private EnergyLevel convertToEnergyLevel(String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "low" -> EnergyLevel.LOW;
			case "medium" -> EnergyLevel.MEDIUM;
			case "high" -> EnergyLevel.HIGH;
			default -> throw new IllegalArgumentException(
					"Invalid energy level: " + value + ". Must be 'low', 'medium', or 'high'");
		};
	}

	private void appendJsonArrayFilter(StringBuilder whereClause, Map<String, Object> parameters, String column,
			String prefix, List<String> values) {
		if (values == null || values.isEmpty()) {
			return;
		}

		List<String> normalizedValues = values.stream().filter(Objects::nonNull)
				.map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
		whereClause.append(" AND EXISTS (SELECT 1 FROM jsonb_array_elements_text(").append(column).append(") AS ")
				.append(prefix).append("(value) WHERE ");
		appendInClause(whereClause, parameters, "LOWER(" + prefix + ".value)", prefix, normalizedValues, false);
		whereClause.append(")");
	}

	private void appendInClause(StringBuilder whereClause, Map<String, Object> parameters, String expression,
			String prefix, Collection<?> values, boolean prependAnd) {
		List<?> filteredValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
		if (filteredValues.isEmpty()) {
			return;
		}

		if (prependAnd) {
			whereClause.append(" AND ");
		}
		whereClause.append(expression).append(" IN (");
		List<String> parameterNames = new ArrayList<>(filteredValues.size());
		int index = 0;
		for (Object value : filteredValues) {
			String parameterName = prefix + index++;
			parameterNames.add(":" + parameterName);
			parameters.put(parameterName, value);
		}
		whereClause.append(String.join(", ", parameterNames)).append(")");
	}

	private void applyParameters(Query query, Map<String, Object> parameters) {
		parameters.forEach(query::setParameter);
	}

	private int resolveLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return Integer.MAX_VALUE;
		}
		return limit;
	}

	private record QueryParts(String whereClause, Map<String, Object> parameters) {
	}
}
