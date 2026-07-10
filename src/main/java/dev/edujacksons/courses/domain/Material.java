package dev.edujacksons.courses.domain;

import dev.edujacksons.common.domain.BaseEntity;
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
 * Материал курса. В Фазе 1 — лекция ({@link MaterialType#LECTURE}): {@code body} — markdown,
 * слайды разделяются {@code ---}. {@code courseId} — FK внутри модуля (задаётся в БД).
 */
@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
public class Material extends BaseEntity {

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MaterialType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public Material(UUID courseId, MaterialType type, String title, String body, int orderIndex) {
        this.courseId = courseId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.orderIndex = orderIndex;
    }
}
