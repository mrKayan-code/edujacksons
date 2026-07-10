package dev.edujacksons.submissions.domain;

import dev.edujacksons.common.domain.BaseEntity;
import dev.edujacksons.common.domain.Language;
import dev.edujacksons.judge.api.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Решение ученика по задаче. {@code problemId}/{@code studentId} — межмодульные ссылки без FK.
 * {@code verdict}/{@code score}/{@code totalTests} заполняются после проверки судьёй.
 * {@code verdict} — тот же enum, что в контракте judge (дубля не заводим).
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission extends BaseEntity {

    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Language language;

    @Column(name = "source_code", nullable = false, columnDefinition = "text")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Verdict verdict;

    @Column
    private Integer score;

    @Column(name = "total_tests")
    private Integer totalTests;

    public Submission(UUID problemId, UUID studentId, Language language, String sourceCode) {
        this.problemId = problemId;
        this.studentId = studentId;
        this.language = language;
        this.sourceCode = sourceCode;
        this.status = SubmissionStatus.QUEUED;
    }
}
