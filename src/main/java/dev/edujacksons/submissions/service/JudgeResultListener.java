package dev.edujacksons.submissions.service;

import dev.edujacksons.judge.api.JudgeResult;
import dev.edujacksons.judge.api.JudgeTopics;
import dev.edujacksons.judge.api.Verdict;
import dev.edujacksons.submissions.domain.Submission;
import dev.edujacksons.submissions.domain.SubmissionResult;
import dev.edujacksons.submissions.domain.SubmissionStatus;
import dev.edujacksons.submissions.repository.SubmissionRepository;
import dev.edujacksons.submissions.repository.SubmissionResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Слушает итоги проверки из {@link JudgeTopics#RESULTS} и обновляет решение: вердикт, счёт и
 * по-тестовые результаты. {@code INTERNAL_ERROR} → статус FAILED (сбой конвейера), иначе FINISHED
 * (проверка отработала, даже если решение неверно).
 */
@Component
public class JudgeResultListener {

    private static final Logger log = LoggerFactory.getLogger(JudgeResultListener.class);

    private final SubmissionRepository submissionRepository;
    private final SubmissionResultRepository resultRepository;

    public JudgeResultListener(SubmissionRepository submissionRepository,
                               SubmissionResultRepository resultRepository) {
        this.submissionRepository = submissionRepository;
        this.resultRepository = resultRepository;
    }

    @KafkaListener(topics = JudgeTopics.RESULTS, groupId = "submissions")
    @Transactional
    public void onResult(JudgeResult result) {
        Submission submission = submissionRepository.findById(result.submissionId()).orElse(null);
        if (submission == null) {
            log.warn("Итог проверки для неизвестного решения {} — пропускаю", result.submissionId());
            return;
        }
        submission.setVerdict(result.verdict());
        submission.setScore(result.passed());
        submission.setTotalTests(result.total());
        submission.setStatus(result.verdict() == Verdict.INTERNAL_ERROR
                ? SubmissionStatus.FAILED
                : SubmissionStatus.FINISHED);

        int index = 0;
        for (JudgeResult.TestResult tr : result.testResults()) {
            resultRepository.save(new SubmissionResult(submission.getId(), tr.testCaseId(),
                    tr.verdict(), tr.timeMs(), tr.memoryKb(), index++));
        }
    }
}
