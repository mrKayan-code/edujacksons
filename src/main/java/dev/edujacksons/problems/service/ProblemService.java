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
     * Курс должен существовать и принадлежать вызывающему учителю.
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

    @Transactional
    public void delete(UUID problemId, UUID actorId) {
        problemRepository.delete(requireOwned(problemId, actorId));
    }

    @Transactional(readOnly = true)
    public List<Problem> listByCourse(UUID courseId, UUID actorId) {
        requireCourseReadAccess(courseId, actorId);
        return problemRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    @Transactional(readOnly = true)
    public Problem getReadable(UUID problemId, UUID actorId) {
        Problem problem = requireProblem(problemId);
        requireReadAccess(problem, actorId);
        return problem;
    }

    // ── тесты ──────────────────────────────────────────────────────────────────

    @Transactional
    public TestCase addTestCase(UUID problemId, UUID actorId, CreateTestCaseRequest request) {
        requireOwned(problemId, actorId);
        int orderIndex = request.orderIndex() != null ? request.orderIndex() : nextOrderIndex(problemId);
        return testCaseRepository.save(new TestCase(problemId, request.input(),
                request.expectedOutput(), Boolean.TRUE.equals(request.sample()), orderIndex));
    }

    @Transactional
    public void deleteTestCase(UUID problemId, UUID testCaseId, UUID actorId) {
        requireOwned(problemId, actorId);
        testCaseRepository.delete(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)
                .orElseThrow(() -> new TestCaseNotFoundException(testCaseId)));
    }

    /** Все тесты задачи — только для владельца (скрытые тесты наружу не отдаём). */
    @Transactional(readOnly = true)
    public List<TestCase> listAllTests(UUID problemId, UUID actorId) {
        requireOwned(problemId, actorId);
        return testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId);
    }

    /** Открытые (sample) тесты задачи — видны любому, у кого есть доступ к задаче. */
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
