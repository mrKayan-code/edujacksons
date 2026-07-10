package dev.edujacksons.judge;

import static org.assertj.core.api.Assertions.assertThat;

import dev.edujacksons.common.domain.Language;
import dev.edujacksons.judge.api.JudgeRequest;
import dev.edujacksons.judge.api.JudgeResult;
import dev.edujacksons.judge.api.Verdict;
import dev.edujacksons.judge.service.CodeExecutor;
import dev.edujacksons.judge.service.ExecutionRequest;
import dev.edujacksons.judge.service.ExecutionResult;
import dev.edujacksons.judge.service.ExecutionStatus;
import dev.edujacksons.judge.service.JudgeService;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Юнит-тест логики вердикта: сверка вывода и агрегация. CodeExecutor подменяется функцией,
 * поэтому Judge0/Docker не нужны — проверяется только доменная логика JudgeService.
 */
class JudgeServiceTest {

    /** Исполнитель, отвечающий по stdin (эмулирует поведение решения без реального запуска). */
    private JudgeService judgeWith(Function<String, ExecutionResult> byStdin) {
        CodeExecutor executor = new CodeExecutor() {
            @Override
            public ExecutionResult execute(ExecutionRequest request) {
                return byStdin.apply(request.stdin());
            }
        };
        return new JudgeService(executor);
    }

    private JudgeRequest requestWith(JudgeRequest.JudgeTestCase... tests) {
        return new JudgeRequest(UUID.randomUUID(), Language.PYTHON, "print(input())",
                1000, 65536, List.of(tests));
    }

    private JudgeRequest.JudgeTestCase test(String input, String expected) {
        return new JudgeRequest.JudgeTestCase(UUID.randomUUID(), input, expected);
    }

    @Test
    void allTestsPass_verdictAccepted() {
        JudgeService judge = judgeWith(stdin -> ExecutionResult.ok(stdin, 5, 1000));
        JudgeResult result = judge.judge(requestWith(test("1", "1"), test("2", "2")));

        assertThat(result.verdict()).isEqualTo(Verdict.ACCEPTED);
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.testResults()).allMatch(t -> t.verdict() == Verdict.ACCEPTED);
    }

    @Test
    void oneWrongAnswer_overallIsFirstFailure_countsPassed() {
        JudgeService judge = judgeWith(stdin -> ExecutionResult.ok("42", 5, 1000));
        JudgeResult result = judge.judge(requestWith(test("42", "42"), test("7", "7")));

        assertThat(result.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
        assertThat(result.passed()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void trailingWhitespaceAndNewlinesIgnoredInComparison() {
        JudgeService judge = judgeWith(stdin -> ExecutionResult.ok("hello \nworld\n\n", 1, 1));
        JudgeResult result = judge.judge(requestWith(test("x", "hello\nworld")));

        assertThat(result.verdict()).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void internalWhitespaceIsSignificant() {
        JudgeService judge = judgeWith(stdin -> ExecutionResult.ok("a  b", 1, 1));
        JudgeResult result = judge.judge(requestWith(test("x", "a b")));

        assertThat(result.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
    }

    @Test
    void executionStatusMapsToVerdict() {
        JudgeService tle = judgeWith(stdin -> ExecutionResult.failed(ExecutionStatus.TIME_LIMIT, null));
        assertThat(tle.judge(requestWith(test("x", "y"))).verdict())
                .isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);

        JudgeService re = judgeWith(stdin -> ExecutionResult.failed(ExecutionStatus.RUNTIME_ERROR, "boom"));
        assertThat(re.judge(requestWith(test("x", "y"))).verdict())
                .isEqualTo(Verdict.RUNTIME_ERROR);
    }

    @Test
    void executorThrows_testBecomesInternalError_pipelineSurvives() {
        JudgeService judge = judgeWith(stdin -> {
            throw new IllegalStateException("executor down");
        });
        JudgeResult result = judge.judge(requestWith(test("x", "y")));

        assertThat(result.verdict()).isEqualTo(Verdict.INTERNAL_ERROR);
        assertThat(result.passed()).isZero();
    }
}
