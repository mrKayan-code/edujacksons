package dev.edujacksons.submissions.service;

import dev.edujacksons.common.api.ForbiddenException;
import dev.edujacksons.judge.api.JudgeRequest;
import dev.edujacksons.judge.api.JudgeTopics;
import dev.edujacksons.problems.service.ProblemDirectory;
import dev.edujacksons.problems.service.ProblemExecutionSpec;
import dev.edujacksons.problems.service.ProblemNotFoundException;
import dev.edujacksons.submissions.api.dto.CreateSubmissionRequest;
import dev.edujacksons.submissions.domain.Submission;
import dev.edujacksons.submissions.domain.SubmissionResult;
import dev.edujacksons.submissions.repository.SubmissionRepository;
import dev.edujacksons.submissions.repository.SubmissionResultRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Публичный сервис модуля submissions. Принимает решение (гейт доступа — через
 * {@link ProblemDirectory}), сохраняет его в статусе QUEUED и шлёт самодостаточную заявку в Kafka.
 * {@code save} коммитит сразу, поэтому заявку публикуем уже после коммита — судья и слушатель
 * результата не гоняются с ещё не видимой строкой. Чтение решения — автору или владельцу задачи.
 */
@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionResultRepository resultRepository;
    private final ProblemDirectory problemDirectory;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SubmissionService(SubmissionRepository submissionRepository,
                             SubmissionResultRepository resultRepository,
                             ProblemDirectory problemDirectory,
                             KafkaTemplate<String, Object> kafkaTemplate) {
        this.submissionRepository = submissionRepository;
        this.resultRepository = resultRepository;
        this.problemDirectory = problemDirectory;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Принимает решение на проверку и инициирует асинхронный конвейер проверки.
     * <p>
     * Процесс включает:
     * 1. Валидацию существования задачи и прав доступа ученика через {@link ProblemDirectory}.
     * 2. Проверку соответствия выбранного языка программирования спецификации задачи.
     * 3. Сохранение попытки в БД со статусом {@link SubmissionStatus#QUEUED}.
     * 4. Публикацию самодостаточного сообщения {@link JudgeRequest} в Kafka (топик {@code judge.requests}).
     * <p>
     * <b>Важно:</b> Сохранение в БД происходит в отдельной транзакции до публикации в Kafka. 
     * Это гарантирует, что когда судья вернет результат, запись о решении уже будет видна в БД.
     *
     * @param studentId  id ученика, отправляющего решение
     * @param problemId  id задачи
     * @param request    данные решения (исходный код, язык)
     * @return созданный объект {@link Submission} со статусом QUEUED
     * @throws ProblemNotFoundException если задача не найдена
     * @throws ForbiddenException       если у ученика нет доступа к этой задаче
     * @throws InvalidLanguageException если язык решения не совпадает с требуемым для задачи
     */
    public Submission submit(UUID studentId, UUID problemId, CreateSubmissionRequest request) {
        ProblemExecutionSpec spec = problemDirectory.executionSpec(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));
        if (!problemDirectory.canAccess(studentId, problemId)) {
            throw new ForbiddenException("Нет доступа к задаче");
        }
        if (request.language() != spec.language()) {
            throw new InvalidLanguageException(
                    "Задача принимает решения на " + spec.language() + ", а не " + request.language());
        }

        // save() коммитит в своей транзакции → строка видна до публикации заявки.
        Submission submission = submissionRepository.save(
                new Submission(problemId, studentId, request.language(), request.sourceCode()));
        publishJudgeRequest(submission, spec);
        return submission;
    }

    private void publishJudgeRequest(Submission submission, ProblemExecutionSpec spec) {
        List<JudgeRequest.JudgeTestCase> tests = spec.tests().stream()
                .map(t -> new JudgeRequest.JudgeTestCase(t.testCaseId(), t.input(), t.expectedOutput()))
                .toList();
        JudgeRequest request = new JudgeRequest(submission.getId(), spec.language(),
                submission.getSourceCode(), spec.timeLimitMs(), spec.memoryLimitKb(), tests);
        kafkaTemplate.send(JudgeTopics.REQUESTS, submission.getId().toString(), request);
    }

    /**
     * Возвращает данные попытки решения, если у пользователя есть права на её чтение.
     * <p>
     * Доступ разрешен:
     * - Автору решения (ученику).
     * - Владельцу задачи, к которой относится решение.
     *
     * @param submissionId id попытки
     * @param actorId      id пользователя
     * @return объект {@link Submission}
     * @throws SubmissionNotFoundException если решение не найдено
     * @throws ForbiddenException          если доступ запрещен
     */
    @Transactional(readOnly = true)
    public Submission getReadable(UUID submissionId, UUID actorId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        boolean allowed = submission.getStudentId().equals(actorId)
                || problemDirectory.ownsProblem(actorId, submission.getProblemId());
        if (!allowed) {
            throw new ForbiddenException("Нет доступа к решению");
        }
        return submission;
    }

    /**
     * Возвращает детальные результаты проверки по каждому тестовому случаю.
     * <p>
     * Метод сначала проверяет права доступа к решению через {@link #getReadable}.
     *
     * @param submissionId id попытки
     * @param actorId      id пользователя
     * @return список результатов {@link SubmissionResult}, отсортированный по индексу теста
     * @throws ForbiddenException если доступ запрещен
     */
    @Transactional(readOnly = true)
    public List<SubmissionResult> results(UUID submissionId, UUID actorId) {
        getReadable(submissionId, actorId);
        return resultRepository.findBySubmissionIdOrderByOrderIndexAsc(submissionId);
    }

    /**
     * Возвращает историю всех попыток конкретного ученика по конкретной задаче.
     * Сортировка: от новых к старым.
     *
     * @param problemId id задачи
     * @param studentId id ученика
     * @return список попыток {@link Submission}
     */
    @Transactional(readOnly = true)
    public List<Submission> listForProblem(UUID problemId, UUID studentId) {
        return submissionRepository.findByProblemIdAndStudentIdOrderByCreatedAtDesc(problemId, studentId);
    }
}
