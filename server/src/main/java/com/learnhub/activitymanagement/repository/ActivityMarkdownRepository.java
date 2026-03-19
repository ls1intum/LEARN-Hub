package com.learnhub.activitymanagement.repository;

import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.entity.enums.MarkdownType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityMarkdownRepository extends JpaRepository<ActivityMarkdown, UUID> {

	@EntityGraph(attributePaths = "activity")
	Optional<ActivityMarkdown> findWithActivityById(UUID markdownId);

	List<ActivityMarkdown> findByActivityId(UUID activityId);

	List<ActivityMarkdown> findByActivityIdAndType(UUID activityId, MarkdownType type);
}
