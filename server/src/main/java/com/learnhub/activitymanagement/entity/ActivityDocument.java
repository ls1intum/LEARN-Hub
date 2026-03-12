package com.learnhub.activitymanagement.entity;

import com.learnhub.activitymanagement.entity.enums.DocumentType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "activity_documents", indexes = {
		@Index(name = "ix_activity_documents_activity_id", columnList = "activity_id")})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "activity_id", nullable = false)
	private Activity activity;

	@Column(name = "document_id", nullable = false)
	private UUID documentId;

	@Column(nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	private DocumentType type;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
