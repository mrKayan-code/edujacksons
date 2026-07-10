package dev.edujacksons.problems.service;

import dev.edujacksons.common.api.ForbiddenException;
import dev.edujacksons.courses.service.CourseAccessQuery;
import dev.edujacksons.courses.service.CourseDirectory;
import dev.edujacksons.courses.service.CourseNotFoundException;
import dev.edujacksons.problems.api.dto.CreateProblemRequest;
import dev.edujacksons.problems.api.dto.CreateTestCaseRequest;
import dev.edujacksons.problems.api.dto.UpdateProblemRequest;
import dev.edujacksons.problems.domain.Problem;
import dev.edujacksons.problems.domain.TestCase;
import dev.edujacksons.problems.repository.ProblemRepository;
import dev.edujacksons.problems.repository.TestCaseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Публичный сервис модуля problems. Записи owner-scoped (создаёт/правит только учитель-владелец
 * курса); чтение — владельцу или ученику с доступом к курсу задачи ({@link CourseAccessQuery}).
 * Существование/владение курса при создании проверяется через {@link CourseDirectory}.
 * Границу {@link ProblemDirectory} для модуля submissions реализует отдельный тонкий бин.
 */
@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final CourseDirectory courseDirectory;
    private final CourseAccessQuery courseAccessQuery;

    public ProblemService(ProblemRepository problemRepository,
                          TestCaseRepository testCaseRepository,
                          CourseDirectory courseDirectory,
                          CourseAccessQuery courseAccessQuery) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.courseDirectory = courseDirectory;
        this.courseAccessQuery = courseAccessQuery;
    }

    // ── задачи ─────────────────────────────────────────────────────────────────

    /**
     * Создаёт задачу в курсе учителя вместе с начальным набором тестов.
     * <p>
     * Процесс включает:
     * 1. Проверку существования курса и владения им пользователем через {@link CourseDirectory}.
     * 2. Сохранение основной сущности {@link Problem}.
     * 3. Массовое создание тестовых случаев, если они переданы в запросе.
     *
     * @param ownerId  id преподавателя-владельца
     * @param request  данные задачи (заголовок, условие, лимиты, тесты)
     * @return созданный объект {@link Problem}
     * @throws CourseNotFoundException если курс не найден или не принадлежит пользователю
     */
    @Transactional
    public Problem create(UUID ownerId, CreateProblemRequest request) {
        requireOwnedCourse(request.courseId(), ownerId);
        Problem problem = problemRepository.save(new Problem(ownerId, request.courseId(),
                request.title(), request.statement(), request.language(),
                request.timeLimitMs(), request.memoryLimitKb()));
        if (request.tests() != null) {
            int index = 0;
            for (CreateTestCaseRequest t : request.tests()) {
                testCaseRepository.save(new TestCase(problem.getId(), t.input(), t.expectedOutput(),
                        Boolean.TRUE.equals(t.sample()), index++));
            }
        }
        return problem;
    }

    /**
     * Обновляет параметры существующей задачи.
     * <p>
     * Доступно только владельцу задачи. Если задача принадлежит другому пользователю,
     * выбрасывается {@link ForbiddenException}.
     *
     * @param problemId id обновляемой задачи
     * @param actorId   id пользователя, совершающего действие
     * @param request   новые данные задачи (заголовок, условие, лимиты)
     * @return обновленный объект {@link Problem}
     * @throws ForbiddenException если пользователь не является владельцем задачи
     */
    @Transactional
    public Problem update(UUID problemId, UUID actorId, UpdateProblemRequest request) {
        Problem problem = requireOwned(problemId, actorId);
        problem.setTitle(request.title());
        problem.setStatement(request.statement());
        problem.setLanguage(request.language());
        problem.setTimeLimitMs(request.timeLimitMs());
        problem.setMemoryLimitKb(request.memoryLimitKb());
        return problem;
    }

    /**
     * Удаляет задачу из системы.
     * <p>
     * Доступно только владельцу задачи.
     *
     * @param problemId id удаляемой задачи
     * @param actorId   id пользователя, совершающего действие
     * @throws ForbiddenException если пользователь не является владельцем задачи
     */
    @Transactional
    public void delete(UUID problemId, UUID actorId) {
        problemRepository.delete(requireOwned(problemId, actorId));
    }

    /**
     * Возвращает список всех задач курса, доступных пользователю.
     * <p>
     * Доступ предоставляется, если:
     * - Пользователь владеет курсом.
     * - Пользователь является учеником, имеющим доступ к курсу через группу.
     *
     * @param courseId id курса
     * @param actorId  id пользователя
     * @return список задач, отсортированный по дате создания (DESC)
     * @throws ForbiddenException если доступ к курсу запрещен
     */
    @Transactional(readOnly = true)
    public List<Problem> listByCourse(UUID courseId, UUID actorId) {
        requireCourseReadAccess(courseId, actorId);
        return problemRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    /**
     * Возвращает данные конкретной задачи, если у пользователя есть права на её чтение.
     * <p>
     * Доступ разрешен владельцу или ученику с доступом к курсу задачи.
     *
     * @param problemId id задачи
     * @param actorId   id пользователя
     * @return объект {@link Problem}
     * @throws ProblemNotFoundException если задача не найдена
     * @throws ForbiddenException       если доступ запрещен
     */
    @Transactional(readOnly = true)
    public Problem getReadable(UUID problemId, UUID actorId) {
        Problem problem = requireProblem(problemId);
        requireReadAccess(problem, actorId);
        return problem;
    }

    // ── тесты ──────────────────────────────────────────────────────────────────

    /**
     * Добавляет новый тестовый случай к задаче.
     * <p>
     * Доступно только владельцу задачи. Если {@code orderIndex} не указан,
     * он будет вычислен автоматически (следующий после последнего теста).
     *
     * @param problemId  id задачи
     * @param actorId    id владельца задачи
     * @param request    данные теста (вход, ожидаемый вывод, является ли примером)
     * @return созданный {@link TestCase}
     * @throws ForbiddenException если пользователь не является владельцем задачи
     */
    @Transactional
    public TestCase addTestCase(UUID problemId, UUID actorId, CreateTestCaseRequest request) {
        requireOwned(problemId, actorId);
        int orderIndex = request.orderIndex() != null ? request.orderIndex() : nextOrderIndex(problemId);
        return testCaseRepository.save(new TestCase(problemId, request.input(),
                request.expectedOutput(), Boolean.TRUE.equals(request.sample()), orderIndex));
    }

    /**
     * Удаляет конкретный тестовый случай.
     * <p>
     * Доступно только владельцу задачи.
     *
     * @param problemId   id задачи
     * @param testCaseId  id теста
     * @param actorId     id владельца задачи
     * @throws TestCaseNotFoundException если тест с таким id не найден в данной задаче
     * @throws ForbiddenException        если пользователь не владелец задачи
     */
    @Transactional
    public void deleteTestCase(UUID problemId, UUID testCaseId, UUID actorId) {
        requireOwned(problemId, actorId);
        testCaseRepository.delete(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)
                .orElseThrow(() -> new TestCaseNotFoundException(testCaseId)));
    }

    /**
     * Возвращает ВСЕ тестовые случаи задачи (включая скрытые).
     * <p>
     * **Внимание**: Данный метод доступен ТОЛЬКО владельцу задачи. 
     * Скрытые тесты никогда не должны попадать в API для учеников.
     *
     * @param problemId id задачи
     * @param actorId   id пользователя
     * @return список всех тестов, отсортированный по индексу порядка
     * @throws ForbiddenException если пользователь не владелец задачи
     */
    @Transactional(readOnly = true)
    public List<TestCase> listAllTests(UUID problemId, UUID actorId) {
        requireOwned(problemId, actorId);
        return testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId);
    }

    /**
     * Возвращает только открытые (sample) тестовые случаи задачи.
     * <p>
     * Эти тесты доступны любому пользователю, имеющему доступ к задаче.
     * Они используются для демонстрации формата ввода-вывода в условии задачи.
     *
     * @param problemId id задачи
     * @param actorId   id пользователя
     * @return список открытых тестов, отсортированный по индексу порядка
     * @throws ForbiddenException если доступ к задаче запрещен
     */
    @Transactional(readOnly = true)
    public List<TestCase> listSampleTests(UUID problemId, UUID actorId) {
        requireReadAccess(requireProblem(problemId), actorId);
        return testCaseRepository.findByProblemIdAndSampleTrueOrderByOrderIndexAsc(problemId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private Problem requireProblem(UUID problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));
    }

    private Problem requireOwned(UUID problemId, UUID actorId) {
        Problem problem = requireProblem(problemId);
        if (!problem.getOwnerId().equals(actorId)) {
            throw new ForbiddenException("Задача принадлежит другому пользователю");
        }
        return problem;
    }

    private void requireOwnedCourse(UUID courseId, UUID ownerId) {
        if (!courseDirectory.isOwnedBy(courseId, ownerId)) {
            throw new CourseNotFoundException(courseId);
        }
    }

    private void requireReadAccess(Problem problem, UUID actorId) {
        if (!problem.getOwnerId().equals(actorId)
                && !courseAccessQuery.canAccess(actorId, problem.getCourseId())) {
            throw new ForbiddenException("Нет доступа к задаче");
        }
    }

    private void requireCourseReadAccess(UUID courseId, UUID actorId) {
        if (!courseDirectory.isOwnedBy(courseId, actorId)
                && !courseAccessQuery.canAccess(actorId, courseId)) {
            throw new ForbiddenException("Нет доступа к курсу");
        }
    }

    private int nextOrderIndex(UUID problemId) {
        List<TestCase> existing = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId);
        return existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getOrderIndex() + 1;
    }
}
