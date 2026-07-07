package dev.edujacksons.groups.domain;

import dev.edujacksons.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Членство ученика в группе. Суррогатный id (ради BaseEntity + created_at/updated_at),
 * пара (group_id, student_id) уникальна. {@code studentId} → {@code users.id} без FK.
 */
@Entity
@Table(name = "group_members")
@Getter
@NoArgsConstructor
public class GroupMember extends BaseEntity {

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    public GroupMember(UUID groupId, UUID studentId) {
        this.groupId = groupId;
        this.studentId = studentId;
    }
}
