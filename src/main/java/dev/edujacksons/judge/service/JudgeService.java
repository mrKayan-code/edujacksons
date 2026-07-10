package dev.edujacksons.judge.service;

import dev.edujacksons.judge.api.JudgeRequest;
import dev.edujacksons.judge.api.JudgeResult;
import dev.edujacksons.judge.api.Verdict;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ядро проверки: гоняет решение через {@link CodeExecutor} по каждому тесту, сверяет вывод и
 * агрегирует вердикт. Логика вердикта живёт здесь (а не в бэкенде исполнения), поэтому
 * тестируется с fake-исполнителем без Judge0. Ошибка исполнителя на тесте → {@code INTERNAL_ERROR}
 * этого теста, проверка продолжается — один сбой не роняет весь конвейер.
 */
@Service
public class JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeService.class);

    private final CodeExecutor executor;

    public JudgeService(CodeExecutor executor) {
        this.executor = executor;
    }

    public JudgeResult judge(JudgeRequest request) {
        List<JudgeResult.TestResult> results = new ArrayList<>();
        int passed = 0;
        for (JudgeRequest.JudgeTestCase test : request.tests()) {
            JudgeResult.TestResult result = runOne(request, test);
            results.add(result);
            if (result.verdict() == Verdict.ACCEPTED) {
                passed++;
            }
        }
        Verdict overall = overallVerdict(results);
        return new JudgeResult(request.submissionId(), overall, passed, request.tests().size(), results);
    }

    private JudgeResult.TestResult runOne(JudgeRequest request, JudgeRequest.JudgeTestCase test) {
        ExecutionResult execution;
        try {
            execution = executor.execute(new ExecutionRequest(request.language(), request.sourceCode(),
                    test.input(), request.timeLimitMs(), request.memoryLimitKb()));
        } catch (RuntimeException ex) {
            log.warn("Исполнитель упал на тесте {} решения {}: {}",
                    test.testCaseId(), request.submissionId(), ex.toString());
            execution = ExecutionResult.failed(ExecutionStatus.INTERNAL_ERROR, ex.getMessage());
        }
        Verdict verdict = verdictFor(execution, test.expectedOutput());
        return new JudgeResult.TestResult(test.testCaseId(), verdict,
                execution.timeMs(), execution.memoryKb());
    }

    private Verdict verdictFor(ExecutionResult execution, String expectedOutput) {
        return switch (execution.status()) {
            case OK -> normalize(execution.stdout()).equals(normalize(expectedOutput))
                    ? Verdict.ACCEPTED
                    : Verdict.WRONG_ANSWER;
            case TIME_LIMIT -> Verdict.TIME_LIMIT_EXCEEDED;
            case RUNTIME_ERROR -> Verdict.RUNTIME_ERROR;
            case COMPILE_ERROR -> Verdict.COMPILE_ERROR;
            case INTERNAL_ERROR -> Verdict.INTERNAL_ERROR;
        };
    }

    /** Итог: ACCEPTED, если все тесты прошли; иначе вердикт первого непройденного (тесты по порядку). */
    private Verdict overallVerdict(List<JudgeResult.TestResult> results) {
        return results.stream()
                .map(JudgeResult.TestResult::verdict)
                .filter(v -> v != Verdict.ACCEPTED)
                .findFirst()
                .orElse(Verdict.ACCEPTED);
    }

    /**
     * Нормализация вывода перед сравнением: убираем хвостовые пробелы в каждой строке и лишние
     * переводы строк в конце. Пробелы внутри строки значимы (важны для форматированного вывода).
     */
    private String normalize(String output) {
        if (output == null) {
            return "";
        }
        String[] lines = output.stripTrailing().split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(stripTrailing(lines[i]));
        }
        return sb.toString();
    }

    private String stripTrailing(String line) {
        int end = line.length();
        while (end > 0 && (line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\r'
                || line.charAt(end - 1) == '\t')) {
            end--;
        }
        return line.substring(0, end);
    }
}
