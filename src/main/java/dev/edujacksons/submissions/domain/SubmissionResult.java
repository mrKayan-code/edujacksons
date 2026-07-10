package dev.edujacksons.submissions.domain;

import dev.edujacksons.common.domain.BaseEntity;
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
 * Результат решения по одному тесту. {@code testCaseId} — ссылка на тест модуля problems без FK.
 * Вход/выход теста не храним (приватность скрытых тестов + размер): только вердикт и метрики.
 */
@Entity
@Table(name = "submission_results")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionResult extends BaseEntity {

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "test_case_id", nullable = false, updatable = false)
    private UUID testCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Verdict verdict;

    @Column(name = "time_ms")
    private Integer timeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public SubmissionResult(UUID submissionId, UUID testCaseId, Verdict verdict,
                            Integer timeMs, Integer memoryKb, int orderIndex) {
        this.submissionId = submissionId;
        this.testCaseId = testCaseId;
        this.verdict = verdict;
        this.timeMs = timeMs;
        this.memoryKb = memoryKb;
        this.orderIndex = orderIndex;
    }
}
