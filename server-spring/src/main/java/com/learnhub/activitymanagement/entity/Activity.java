package com.learnhub.activitymanagement.entity;

import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.EnergyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "activities", indexes = {
    @Index(name = "ix_activities_age_range", columnList = "age_min,age_max"),
    @Index(name = "ix_activities_format", columnList = "format"),
    @Index(name = "ix_activities_bloom_level", columnList = "bloom_level")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 255)
    private String source;

    @Column(name = "age_min", nullable = false)
    private Integer ageMin;

    @Column(name = "age_max", nullable = false)
    private Integer ageMax;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ActivityFormat format;

    @Column(name = "bloom_level", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private BloomLevel bloomLevel;

    @Column(name = "duration_min_minutes", nullable = false)
    private Integer durationMinMinutes;

    @Column(name = "duration_max_minutes")
    private Integer durationMaxMinutes;

    @Column(name = "mental_load", nullable = false, columnDefinition = "energylevel")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EnergyLevel mentalLoad = EnergyLevel.MEDIUM;

    @Column(name = "physical_energy", nullable = false, columnDefinition = "energylevel")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EnergyLevel physicalEnergy = EnergyLevel.MEDIUM;

    @Column(name = "prep_time_minutes", nullable = false)
    private Integer prepTimeMinutes = 5;

    @Column(name = "cleanup_time_minutes", nullable = false)
    private Integer cleanupTimeMinutes = 5;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resources_needed", columnDefinition = "jsonb")
    private List<String> resourcesNeeded;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> topics;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Transient field for break information (not persisted to database)
    @Transient
    private Break breakAfter;
}
