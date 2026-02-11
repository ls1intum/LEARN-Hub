package com.learnhub.usermanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_favourites", indexes = {
    @Index(name = "ix_user_favourites_id", columnList = "id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFavourites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "favourite_type", nullable = false, length = 20)
    private String favouriteType;

    @Column(name = "activity_id")
    private Long activityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "activity_ids", columnDefinition = "jsonb")
    private String activityIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lesson_plan_snapshot", columnDefinition = "jsonb")
    private String lessonPlanSnapshot;

    @Column(length = 255)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
}
