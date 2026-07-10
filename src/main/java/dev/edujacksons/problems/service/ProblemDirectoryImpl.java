package dev.edujacksons.problems.service;

import dev.edujacksons.courses.service.CourseAccessQuery;
import dev.edujacksons.problems.domain.Problem;
import dev.edujacksons.problems.repository.ProblemRepository;
import dev.edujacksons.problems.repository.TestCaseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация границы {@link ProblemDirectory}. Тонкий адаптер над репозиториями problems и
 * портом {@link CourseAccessQuery} (courses) — не зависит от {@code ProblemService},
 * поэтому граф бинов ацикличен. Доступ ученика к задаче сводится к доступу к её курсу.
 */
@Service
class ProblemDirectoryImpl implements ProblemDirectory {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final CourseAccessQuery courseAccessQuery;

    ProblemDirectoryImpl(ProblemRepository problemRepository,
                         TestCaseRepository testCaseRepository,
                         CourseAccessQuery courseAccessQuery) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.courseAccessQuery = courseAccessQuery;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccess(UUID userId, UUID problemId) {
        return problemRepository.findById(problemId)
                .map(p -> p.getOwnerId().equals(userId)
                        || courseAccessQuery.canAccess(userId, p.getCourseId()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ownsProblem(UUID userId, UUID problemId) {
        return problemRepository.findById(problemId)
                .map(p -> p.getOwnerId().equals(userId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProblemExecutionSpec> executionSpec(UUID problemId) {
        return problemRepository.findById(problemId).map(this::toSpec);
    }

    private ProblemExecutionSpec toSpec(Problem problem) {
        List<TestCaseData> tests = testCaseRepository
                .findByProblemIdOrderByOrderIndexAsc(problem.getId())
                .stream()
                .map(tc -> new TestCaseData(tc.getId(), tc.getInput(), tc.getExpectedOutput()))
                .toList();
        return new ProblemExecutionSpec(problem.getId(), problem.getLanguage(),
                problem.getTimeLimitMs(), problem.getMemoryLimitKb(), tests);
    }
}
