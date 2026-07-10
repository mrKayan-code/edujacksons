package dev.edujacksons.judge.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.edujacksons.common.domain.Language;
import dev.edujacksons.judge.config.Judge0Properties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Реализация исполнителя кода на базе self-hosted Judge0 (через REST API).
 * <p>
 * Особенности реализации:
 * 1. **Синхронный режим**: Использует параметр {@code wait=true}, чтобы получить результат
 *    исполнения в одном HTTP-ответе. Это упрощает обработку одного теста.
 * 2. **Отделение от проверки**: Не передает ожидаемый вывод в Judge0. Сверка выполняется
 *    в {@link JudgeService}, что делает систему независимой от реализации Judge0.
 * 3. **Маппинг статусов**: Преобразует числовые ID статусов Judge0 в внутренний {@link ExecutionStatus}.
 * 4. **Изоляция**: В тестах этот компонент подменяется fake-бином для исключения зависимости от Docker.
 */
@Component
public class Judge0CodeExecutor implements CodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(Judge0CodeExecutor.class);

    /** id языков Judge0. На старте только Python 3 (=71). */
    private static final Map<Language, Integer> LANGUAGE_IDS = Map.of(Language.PYTHON, 71);

    private final RestClient client;

    public Judge0CodeExecutor(Judge0Properties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        this.client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        Integer languageId = LANGUAGE_IDS.get(request.language());
        if (languageId == null) {
            return ExecutionResult.failed(ExecutionStatus.INTERNAL_ERROR,
                    "Язык не поддерживается Judge0: " + request.language());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("source_code", request.sourceCode());
        body.put("language_id", languageId);
        body.put("stdin", request.stdin());
        body.put("cpu_time_limit", request.timeLimitMs() / 1000.0);
        body.put("memory_limit", request.memoryLimitKb());

        Judge0Response response;
        try {
            response = client.post()
                    .uri(uri -> uri.path("/submissions")
                            .queryParam("base64_encoded", "false")
                            .queryParam("wait", "true")
                            .build())
                    .body(body)
                    .retrieve()
                    .body(Judge0Response.class);
        } catch (RestClientException ex) {
            log.warn("Judge0 недоступен/ошибка запроса: {}", ex.toString());
            return ExecutionResult.failed(ExecutionStatus.INTERNAL_ERROR, ex.getMessage());
        }
        if (response == null || response.status() == null || response.status().id() == null) {
            return ExecutionResult.failed(ExecutionStatus.INTERNAL_ERROR, "Пустой ответ Judge0");
        }
        return toExecutionResult(response);
    }

    private ExecutionResult toExecutionResult(Judge0Response r) {
        int statusId = r.status().id();
        ExecutionStatus status = switch (statusId) {
            case 3 -> ExecutionStatus.OK;                 // Accepted (процесс завершился нормально)
            case 5 -> ExecutionStatus.TIME_LIMIT;         // Time Limit Exceeded
            case 6 -> ExecutionStatus.COMPILE_ERROR;      // Compilation Error
            case 7, 8, 9, 10, 11, 12 -> ExecutionStatus.RUNTIME_ERROR;  // разные Runtime Error
            default -> ExecutionStatus.INTERNAL_ERROR;    // 1/2 (queued/processing), 13/14, прочее
        };
        if (status == ExecutionStatus.OK) {
            return ExecutionResult.ok(r.stdout() == null ? "" : r.stdout(),
                    parseTimeMs(r.time()), r.memory());
        }
        String message = status == ExecutionStatus.COMPILE_ERROR ? r.compileOutput() : r.stderr();
        return ExecutionResult.failed(status, message);
    }

    private Integer parseTimeMs(String seconds) {
        if (seconds == null) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(seconds) * 1000);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Judge0Response(
            String stdout,
            String stderr,
            @JsonProperty("compile_output") String compileOutput,
            String message,
            String time,
            Integer memory,
            Status status
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Status(Integer id, String description) {
        }
    }
}
