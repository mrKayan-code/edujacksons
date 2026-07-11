package dev.edujacksons.submissions.service;

import dev.edujacksons.submissions.domain.Submission;
import dev.edujacksons.submissions.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реализация порта SubmissionDirectory.
 * Является тонким адаптером над репозиторием, чтобы избежать циклических зависимостей с сервисами.
 */
@Component
@RequiredArgsConstructor
public class SubmissionDirectoryImpl implements SubmissionDirectory {

    private final SubmissionRepository repository;

    @Override
    public Optional<SubmissionView> find(UUID submissionId) {
        return repository.findById(submissionId)
                .map(this::mapToView);
    }

    @Override
    public List<SubmissionView> listByProblem(UUID problemId) {
        return repository.findByProblemIdOrderByCreatedAtDesc(problemId)
                .stream()
                .map(this::mapToView)
                .collect(Collectors.toList());
    }

    private SubmissionView mapToView(Submission submission) {
        return new SubmissionView(
                submission.getId(),
                submission.getProblemId(),
                submission.getStudentId(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getCreatedAt()
        );
    }
}
