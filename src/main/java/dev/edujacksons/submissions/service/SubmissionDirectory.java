package dev.edujacksons.submissions.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Публичный порт модуля submissions для чтения данных о решениях.
 * Реализуется тонким адаптером над репозиторием для обеспечения ацикличности графа бинов.
 */
public interface SubmissionDirectory {
    Optional<SubmissionView> find(UUID submissionId);
    List<SubmissionView> listByProblem(UUID problemId);
}
