package dev.edujacksons.submissions.domain;

/**
 * Жизненный цикл решения. {@code QUEUED} — принято, ждёт судью; {@code FINISHED} — проверено
 * (вердикт выставлен, даже если WRONG_ANSWER); {@code FAILED} — сбой конвейера/исполнителя.
 */
public enum SubmissionStatus {
    QUEUED,
    FINISHED,
    FAILED
}
