package dev.edujacksons.grading;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Интеграционный тест модуля grading.
 * Проверяет полный цикл: отправка решения -> ручная проверка -> публикация -> правка -> журнал.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class GradingIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void gradingFullLifecycleTest() throws Exception {
        // Setup: Регистрация и создание инфраструктуры
        String teacher = register("t.grading@example.com", "TEACHER");
        String student = register("s.grading@example.com", "STUDENT");
        String courseId = createCourse(teacher, "Тест Grading");
        assignStudentToCourseGroup(teacher, courseId, "s.grading@example.com");
        String problemId = createProblem(teacher, courseId);

        // --- ШАГ 1: Ученик отправляет решение ---
        String submissionId = submit(student, problemId, "print('hello')");

        // --- ШАГ 2: Учитель ставит оценку и публикует (POST review, publish=true) ---
        String reviewJson = mockMvc.perform(authed(post("/api/submissions/" + submissionId + "/review"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score": 85, "feedback": "Хорошая работа!", "publish": true}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.score", is(85)))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andReturn().getResponse().getContentAsString();
        
        String reviewId = idOf(reviewJson);
        String firstPublishedAt = jsonPathValue(reviewJson, "$.publishedAt");

        // --- ШАГ 3: Ученик видит PUBLISHED-проверку (GET, роль STUDENT) ---
        mockMvc.perform(authed(get("/api/submissions/" + submissionId + "/review"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(85)))
                .andExpect(jsonPath("$.feedback", is("Хорошая работа!")));

        // --- ШАГ 4: Учитель правит оценку задним числом (PATCH на уже PUBLISHED) ---
        String updatedReviewJson = mockMvc.perform(authed(patch("/api/reviews/" + reviewId), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score": 95}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(95)))
                .andReturn().getResponse().getContentAsString();

        // --- ШАГ 5: Проверка таймстампов (published_at не меняется, updated_at меняется) ---
        OffsetDateTime firstTime = OffsetDateTime.parse(jsonPathValue(reviewJson, "$.publishedAt")).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        OffsetDateTime secondTime = OffsetDateTime.parse(jsonPathValue(updatedReviewJson, "$.publishedAt")).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/grading_debug.log"), 
                "firstTime: " + firstTime + "\nsecondTime: " + secondTime + "\n", 
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) { e.printStackTrace(); }
        
        org.junit.jupiter.api.Assertions.assertTrue(firstTime.isEqual(secondTime), "publishedAt should not change after update");
        String updatedAt = jsonPathValue(updatedReviewJson, "$.updatedAt");
        org.junit.jupiter.api.Assertions.assertNotNull(updatedAt, "updatedAt should be set");

        // --- ШАГ 6: Ученик отправляет НОВОЕ решение той же задачи ---
        String newSubmissionId = submit(student, problemId, "print('hello v2')");

        // --- ШАГ 7: Журнал (GET gradebook) — новая строка NOT_REVIEWED, старая PUBLISHED ---
        mockMvc.perform(authed(get("/api/problems/" + problemId + "/gradebook"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Новая запись (сверху)
                .andExpect(jsonPath("[0].submissionId", is(newSubmissionId)))
                .andExpect(jsonPath("[0].reviewStatus", is("NOT_REVIEWED")))
                .andExpect(jsonPath("[0].score").isEmpty())
                // Старая запись (снизу)
                .andExpect(jsonPath("[1].submissionId", is(submissionId)))
                .andExpect(jsonPath("[1].reviewStatus", is("PUBLISHED")))
                .andExpect(jsonPath("[1].score", is(95)));

        // --- ШАГ 8: Проверка приватности DRAFT-проверки ---
        // Учитель создает черновик для нового решения
        mockMvc.perform(authed(post("/api/submissions/" + newSubmissionId + "/review"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score": 50, "feedback": "Пока не публикуем", "publish": false}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")));

        // Ученик пытается увидеть этот черновик -> 404
        mockMvc.perform(authed(get("/api/submissions/" + newSubmissionId + "/review"), student))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("review_not_found")));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

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

    private String createCourse(String teacher, String title) throws Exception {
        return idOf(mockMvc.perform(authed(post("/api/courses"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

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

    private String createProblem(String teacher, String courseId) throws Exception {
        return idOf(mockMvc.perform(authed(post("/api/problems"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"%s","title":"Проблема","statement":"x","language":"PYTHON",
                                 "timeLimitMs":1000,"memoryLimitKb":65536,
                                 "tests":[{"input":"1","expectedOutput":"1","sample":true}]}"""
                                .formatted(courseId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private String submit(String token, String problemId, String code) throws Exception {
        return idOf(mockMvc.perform(authed(post("/api/problems/" + problemId + "/submissions"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"PYTHON\",\"sourceCode\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private String idOf(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asText();
    }

    private String jsonPathValue(String json, String path) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        // Very simple path resolver for $.field
        String field = path.substring(2);
        return node.get(field).asText();
    }
}
