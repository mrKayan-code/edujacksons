package dev.edujacksons.problems.repository;

import dev.edujacksons.problems.domain.Problem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    List<Problem> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<Problem> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
