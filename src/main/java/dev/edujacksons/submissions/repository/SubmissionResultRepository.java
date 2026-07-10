package dev.edujacksons.submissions.repository;

import dev.edujacksons.submissions.domain.SubmissionResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionResultRepository extends JpaRepository<SubmissionResult, UUID> {

    List<SubmissionResult> findBySubmissionIdOrderByOrderIndexAsc(UUID submissionId);
}
