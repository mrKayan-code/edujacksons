package dev.edujacksons.grading.domain;

import dev.edujacksons.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ручная проверка решения учителем.
 * Ссылки submissionId, problemId, studentId, reviewerId — межмодульные, без FK.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {

    @Column(name = "submission_id", nullable = false, unique = true, updatable = false)
    private UUID submissionId;

    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewStatus status;

    @Column
    private Integer score;

    @Column(columnDefinition = "text")
    private String feedback;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    public Review(UUID submissionId, UUID problemId, UUID studentId, UUID reviewerId, 
                  ReviewStatus status, Integer score, String feedback, OffsetDateTime publishedAt) {
        this.submissionId = submissionId;
        this.problemId = problemId;
        this.studentId = studentId;
        this.reviewerId = reviewerId;
        this.status = status;
        this.score = score;
        this.feedback = feedback;
        this.publishedAt = publishedAt;
    }
}
