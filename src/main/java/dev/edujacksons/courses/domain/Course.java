package dev.edujacksons.courses.domain;

import dev.edujacksons.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Курс, созданный учителем. {@code ownerId} ссылается на {@code users.id} без FK (межмодульно). */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course extends BaseEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    public Course(UUID ownerId, String title, String description) {
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
    }
}
