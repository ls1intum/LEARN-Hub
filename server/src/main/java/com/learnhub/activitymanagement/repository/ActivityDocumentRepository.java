package com.learnhub.activitymanagement.repository;

import com.learnhub.activitymanagement.entity.ActivityDocument;
import com.learnhub.activitymanagement.entity.enums.DocumentType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityDocumentRepository extends JpaRepository<ActivityDocument, UUID> {

	List<ActivityDocument> findByActivityId(UUID activityId);

	List<ActivityDocument> findByActivityIdAndType(UUID activityId, DocumentType type);
}
