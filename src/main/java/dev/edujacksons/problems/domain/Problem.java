package dev.edujacksons.problems.domain;

import dev.edujacksons.common.domain.BaseEntity;
import dev.edujacksons.common.domain.Language;
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
 * Задача по кодингу с автопроверкой. {@code ownerId} — учитель (users.id), {@code courseId} —
 * курс (courses.id); обе ссылки без FK (межмодульно). Доступ ученика к задаче = доступ к её курсу.
 */
@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
public class Problem extends BaseEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Language language;

    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs;

    @Column(name = "memory_limit_kb", nullable = false)
    private int memoryLimitKb;

    public Problem(UUID ownerId, UUID courseId, String title, String statement,
                   Language language, int timeLimitMs, int memoryLimitKb) {
        this.ownerId = ownerId;
        this.courseId = courseId;
        this.title = title;
        this.statement = statement;
        this.language = language;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitKb = memoryLimitKb;
    }
}
