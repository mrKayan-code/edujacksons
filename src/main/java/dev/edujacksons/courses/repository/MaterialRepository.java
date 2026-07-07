package dev.edujacksons.courses.repository;

import dev.edujacksons.courses.domain.Material;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findByCourseIdOrderByOrderIndexAsc(UUID courseId);

    Optional<Material> findByIdAndCourseId(UUID id, UUID courseId);
}
