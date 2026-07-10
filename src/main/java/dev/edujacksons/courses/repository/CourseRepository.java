package dev.edujacksons.courses.repository;

import dev.edujacksons.courses.domain.Course;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Course> findByIdInOrderByCreatedAtDesc(Collection<UUID> ids);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
