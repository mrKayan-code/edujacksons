package dev.edujacksons.judge.service;

import dev.edujacksons.judge.api.JudgeRequest;
import dev.edujacksons.judge.api.JudgeResult;
import dev.edujacksons.judge.api.JudgeTopics;
import dev.edujacksons.judge.api.Verdict;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Входной адаптер судьи: получает заявку из {@link JudgeTopics#REQUESTS}, прогоняет её через
 * {@link JudgeService} и публикует итог в {@link JudgeTopics#RESULTS}. Даже при неожиданном сбое
 * шлём результат с {@code INTERNAL_ERROR}, чтобы решение не «зависло» в статусе QUEUED.
 */
@Component
public class JudgeRequestListener {

    private static final Logger log = LoggerFactory.getLogger(JudgeRequestListener.class);

    private final JudgeService judgeService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public JudgeRequestListener(JudgeService judgeService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.judgeService = judgeService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = JudgeTopics.REQUESTS, groupId = "judge")
    public void onRequest(JudgeRequest request) {
        JudgeResult result;
        try {
            result = judgeService.judge(request);
        } catch (RuntimeException ex) {
            log.error("Проверка решения {} упала: {}", request.submissionId(), ex.toString(), ex);
            result = new JudgeResult(request.submissionId(), Verdict.INTERNAL_ERROR,
                    0, request.tests() == null ? 0 : request.tests().size(), List.of());
        }
        kafkaTemplate.send(JudgeTopics.RESULTS, request.submissionId().toString(), result);
    }
}
