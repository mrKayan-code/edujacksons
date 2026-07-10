package dev.edujacksons.submissions.repository;

import dev.edujacksons.submissions.domain.Submission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByProblemIdAndStudentIdOrderByCreatedAtDesc(UUID problemId, UUID studentId);

    List<Submission> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
