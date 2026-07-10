package dev.edujacksons.problems.repository;

import dev.edujacksons.problems.domain.TestCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findByProblemIdOrderByOrderIndexAsc(UUID problemId);

    List<TestCase> findByProblemIdAndSampleTrueOrderByOrderIndexAsc(UUID problemId);

    Optional<TestCase> findByIdAndProblemId(UUID id, UUID problemId);
}
