package dev.edujacksons.groups.domain;

import dev.edujacksons.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Учебная группа учителя. {@code ownerId} → {@code users.id} без FK (межмодульно). */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
public class Group extends BaseEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    public Group(UUID ownerId, String title) {
        this.ownerId = ownerId;
        this.title = title;
    }
}
