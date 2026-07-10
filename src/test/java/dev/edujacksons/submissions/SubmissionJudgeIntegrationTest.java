package dev.edujacksons.submissions;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.edujacksons.judge.service.CodeExecutor;
import dev.edujacksons.judge.service.ExecutionRequest;
import dev.edujacksons.judge.service.ExecutionResult;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Сквозной тест Фазы 2 против реального PostgreSQL + Kafka (Testcontainers): учитель заводит задачу
 * с тестами, ученик отправляет решение, конвейер submissions → judge → submissions асинхронно
 * проставляет вердикт. CodeExecutor подменяется fake-эхом (Judge0/Docker-исполнение не нужны).
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionJudgeIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.7.0");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    /** Fake-исполнитель: «эхо» stdin. Так решение проходит тесты, где expected == input, и валит остальные. */
    @TestConfiguration
    static class FakeExecutorConfig {
        @Bean
        @Primary
        CodeExecutor fakeCodeExecutor() {
            return (ExecutionRequest r) -> ExecutionResult.ok(r.stdin(), 1, 100);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void studentSubmitsAndPipelineJudgesAcceptedAndWrongAnswer() throws Exception {
        String teacher = register("t.judge1@example.com", "TEACHER");
        String student = register("s.judge1@example.com", "STUDENT");

        String courseId = createCourse(teacher, "Python: ввод-вывод");
        assignStudentToCourseGroup(teacher, courseId, "s.judge1@example.com");

        // Задача-«эхо»: печатает то, что подано. Один открытый тест + один скрытый.
        String echoProblem = idOf(mockMvc.perform(authed(post("/api/problems"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"%s","title":"Эхо","statement":"Выведи ввод",
                                 "language":"PYTHON","timeLimitMs":1000,"memoryLimitKb":65536,
                                 "tests":[
                                   {"input":"5","expectedOutput":"5","sample":true},
                                   {"input":"42","expectedOutput":"42","sample":false}]}"""
                                .formatted(courseId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        // Ученик видит только открытый тест; учитель — оба.
        mockMvc.perform(authed(get("/api/problems/" + echoProblem), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tests", hasSize(1)))
                .andExpect(jsonPath("$.statement", is("Выведи ввод")));
        mockMvc.perform(authed(get("/api/problems/" + echoProblem), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tests", hasSize(2)));

        // Отправка корректного решения → асинхронно ACCEPTED по всем тестам.
        String accepted = submit(student, echoProblem, "print(input())");
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() ->
                mockMvc.perform(authed(get("/api/submissions/" + accepted), student))
                        .andExpect(jsonPath("$.status", is("FINISHED")))
                        .andExpect(jsonPath("$.verdict", is("ACCEPTED")))
                        .andExpect(jsonPath("$.score", is(2)))
                        .andExpect(jsonPath("$.totalTests", is(2)))
                        .andExpect(jsonPath("$.results", hasSize(2))));

        // Задача, где ответ ≠ ввод: «эхо»-решение получит WRONG_ANSWER.
        String shoutProblem = idOf(mockMvc.perform(authed(post("/api/problems"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"%s","title":"Плюс восклицание","statement":"Добавь !",
                                 "language":"PYTHON","timeLimitMs":1000,"memoryLimitKb":65536,
                                 "tests":[{"input":"5","expectedOutput":"5!","sample":true}]}"""
                                .formatted(courseId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String wrong = submit(student, shoutProblem, "print(input())");
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() ->
                mockMvc.perform(authed(get("/api/submissions/" + wrong), student))
                        .andExpect(jsonPath("$.status", is("FINISHED")))
                        .andExpect(jsonPath("$.verdict", is("WRONG_ANSWER")))
                        .andExpect(jsonPath("$.score", is(0))));
    }

    @Test
    void accessRules() throws Exception {
        String teacher = register("t.judge2@example.com", "TEACHER");
        String student = register("s.judge2@example.com", "STUDENT");
        String outsider = register("s.judge3@example.com", "STUDENT");

        String courseId = createCourse(teacher, "Курс с задачей");
        assignStudentToCourseGroup(teacher, courseId, "s.judge2@example.com");

        String problem = idOf(mockMvc.perform(authed(post("/api/problems"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"%s","title":"Задача","statement":"...",
                                 "language":"PYTHON","timeLimitMs":1000,"memoryLimitKb":65536,
                                 "tests":[{"input":"1","expectedOutput":"1","sample":true}]}"""
                                .formatted(courseId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        // Ученик не может создавать задачи.
        mockMvc.perform(authed(post("/api/problems"), student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"%s","title":"Чужая","statement":"x","language":"PYTHON",
                                 "timeLimitMs":1000,"memoryLimitKb":1024}""".formatted(courseId)))
                .andExpect(status().isForbidden());

        // Ученик не из группы не видит задачу и не может отправить решение.
        mockMvc.perform(authed(get("/api/problems/" + problem), outsider))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(post("/api/problems/" + problem + "/submissions"), outsider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"PYTHON","sourceCode":"print(1)"}"""))
                .andExpect(status().isForbidden());

        // Решение по несуществующей задаче → 404.
        mockMvc.perform(authed(post("/api/problems/" + java.util.UUID.randomUUID() + "/submissions"), student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"PYTHON","sourceCode":"print(1)"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("problem_not_found")));

        // Чужой ученик не видит чужое решение.
        String submissionId = submit(student, problem, "print(input())");
        mockMvc.perform(authed(get("/api/submissions/" + submissionId), outsider))
                .andExpect(status().isForbidden());
        // Владелец задачи (учитель) — видит.
        mockMvc.perform(authed(get("/api/submissions/" + submissionId), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(submissionId)));
    }

    @Test
    void submissionHistoryGrowsPerStudent() throws Exception {
        String teacher = register("t.judge4@example.com", "TEACHER");
        String student = register("s.judge4@example.com", "STUDENT");
        String courseId = createCourse(teacher, "История");
        assignStudentToCourseGroup(teacher, courseId, "s.judge4@example.com");
        String problem = idOf(mockMvc.perform(authed(post("/api/problems"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"%s","title":"H","statement":"x","language":"PYTHON",
                                 "timeLimitMs":1000,"memoryLimitKb":65536,
                                 "tests":[{"input":"1","expectedOutput":"1","sample":true}]}"""
                                .formatted(courseId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        submit(student, problem, "print(input())");
        submit(student, problem, "print(input())");

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() ->
                mockMvc.perform(authed(get("/api/problems/" + problem + "/submissions"), student))
                        .andExpect(jsonPath("$", hasSize(greaterThan(1)))));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String submit(String token, String problemId, String code) throws Exception {
        return idOf(mockMvc.perform(authed(post("/api/problems/" + problemId + "/submissions"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"PYTHON\",\"sourceCode\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("QUEUED")))
                .andReturn().getResponse().getContentAsString());
    }

    private String createCourse(String teacher, String title) throws Exception {
        return idOf(mockMvc.perform(authed(post("/api/courses"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    /** Заводит группу, добавляет ученика по email и привязывает к ней курс — так ученик получает доступ. */
    private void assignStudentToCourseGroup(String teacher, String courseId, String email) throws Exception {
        String groupId = idOf(mockMvc.perform(authed(post("/api/groups"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Группа\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(authed(post("/api/groups/" + groupId + "/members"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(authed(post("/api/groups/" + groupId + "/courses"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + courseId + "\"}"))
                .andExpect(status().isNoContent());
    }

    private String register(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","displayName":"%s","role":"%s"}"""
                                .formatted(email, email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private String idOf(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asText();
    }
}
