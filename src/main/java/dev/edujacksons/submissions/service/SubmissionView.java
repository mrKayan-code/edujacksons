package dev.edujacksons.submissions.service;

import dev.edujacksons.submissions.domain.SubmissionStatus;
import dev.edujacksons.judge.api.Verdict;
import java.time.Instant;
import java.util.UUID;

/**
 * Представление решения для внешних модулей.
 * Сознательно не содержит исходного кода (sourceCode), чтобы не перегружать
 * передачу данных при формировании журналов и списков.
 */
public record SubmissionView(
    UUID id,
    UUID problemId,
    UUID studentId,
    SubmissionStatus status,
    Verdict verdict,
    Instant submittedAt
) {}
