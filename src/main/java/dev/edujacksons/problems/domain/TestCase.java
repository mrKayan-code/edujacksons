package dev.edujacksons.problems.domain;

import dev.edujacksons.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Один тест задачи: вход (stdin) и ожидаемый вывод (stdout). {@code sample=true} — открытый
 * пример (виден ученику в условии), иначе скрытый. {@code problemId} — FK внутри модуля.
 */
@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
public class TestCase extends BaseEntity {

    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    @Column(nullable = false, columnDefinition = "text")
    private String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "text")
    private String expectedOutput;

    @Column(name = "is_sample", nullable = false)
    private boolean sample;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public TestCase(UUID problemId, String input, String expectedOutput,
                    boolean sample, int orderIndex) {
        this.problemId = problemId;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.sample = sample;
        this.orderIndex = orderIndex;
    }
}
