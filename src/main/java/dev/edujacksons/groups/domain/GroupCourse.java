package dev.edujacksons.groups.domain;

import dev.edujacksons.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Привязка курса к группе («назначение» Фазы 1). Суррогатный id, пара (group_id, course_id)
 * уникальна. {@code courseId} → {@code courses.id} без FK (межмодульно).
 */
@Entity
@Table(name = "group_courses")
@Getter
@NoArgsConstructor
public class GroupCourse extends BaseEntity {

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    public GroupCourse(UUID groupId, UUID courseId) {
        this.groupId = groupId;
        this.courseId = courseId;
    }
}
