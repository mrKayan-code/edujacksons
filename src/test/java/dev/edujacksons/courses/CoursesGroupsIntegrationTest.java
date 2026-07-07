package dev.edujacksons.courses;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Сквозной тест Фазы 1 против реального PostgreSQL (Testcontainers): курсы, группы, доступ ученика. */
@Tag("integration")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CoursesGroupsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void teacherPublishesCourseAndStudentSeesMaterial() throws Exception {
        String teacher = register("teacher1@example.com", "TEACHER");
        String student = register("student1@example.com", "STUDENT");

        // Учитель создаёт курс + лекцию.
        String courseId = idOf(mockMvc.perform(authed(post("/api/courses"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Python с нуля","description":"Вводный курс"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Python с нуля")))
                .andReturn().getResponse().getContentAsString());

        String materialId = idOf(mockMvc.perform(
                        authed(post("/api/courses/" + courseId + "/materials"), teacher)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"type":"LECTURE","title":"Переменные",
                                         "body":"# Слайд 1\\n---\\n# Слайд 2"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("LECTURE")))
                .andExpect(jsonPath("$.orderIndex", is(0)))
                .andReturn().getResponse().getContentAsString());

        // Учитель создаёт группу, добавляет ученика по email, привязывает курс.
        String groupId = idOf(mockMvc.perform(authed(post("/api/groups"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"11-А"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(authed(post("/api/groups/" + groupId + "/members"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student1@example.com"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("student1@example.com")))
                .andExpect(jsonPath("$.studentId", is(notNullValue())));

        mockMvc.perform(authed(post("/api/groups/" + groupId + "/courses"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + courseId + "\"}"))
                .andExpect(status().isNoContent());

        // Ученик видит курс и читает лекцию.
        mockMvc.perform(authed(get("/api/courses"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(courseId)));

        mockMvc.perform(authed(get("/api/courses/" + courseId + "/materials/" + materialId), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Переменные")))
                .andExpect(jsonPath("$.body", is("# Слайд 1\n---\n# Слайд 2")));

        mockMvc.perform(authed(get("/api/courses/" + courseId), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materials", hasSize(1)));

        // Детали группы у учителя: участник + курс.
        mockMvc.perform(authed(get("/api/groups/" + groupId), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.courses", hasSize(1)));
    }

    @Test
    void authorizationAndValidationRules() throws Exception {
        String teacher = register("teacher2@example.com", "TEACHER");
        String otherTeacher = register("teacher3@example.com", "TEACHER");
        String student = register("student2@example.com", "STUDENT");
        String outsider = register("student3@example.com", "STUDENT");

        String courseId = idOf(mockMvc.perform(authed(post("/api/courses"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Алгоритмы"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        // Ученик не может создавать курсы.
        mockMvc.perform(authed(post("/api/courses"), student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Чужой курс"}"""))
                .andExpect(status().isForbidden());

        // Чужой учитель не может править курс.
        mockMvc.perform(authed(post("/api/courses/" + courseId + "/materials"), otherTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"LECTURE","title":"Взлом","body":"x"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("forbidden")));

        String groupId = idOf(mockMvc.perform(authed(post("/api/groups"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Группа"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        // Несуществующий email → 404.
        mockMvc.perform(authed(post("/api/groups/" + groupId + "/members"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("student_not_found")));

        // Учителя нельзя добавить как ученика → 409.
        mockMvc.perform(authed(post("/api/groups/" + groupId + "/members"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"teacher3@example.com"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("not_a_student")));

        // Первое добавление — ок, повторное → 409.
        mockMvc.perform(authed(post("/api/groups/" + groupId + "/members"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student2@example.com"}"""))
                .andExpect(status().isCreated());
        mockMvc.perform(authed(post("/api/groups/" + groupId + "/members"), teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student2@example.com"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("already_member")));

        // Привязка чужого курса другим учителем → 404.
        String otherGroupId = idOf(mockMvc.perform(authed(post("/api/groups"), otherTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Чужая группа"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(authed(post("/api/groups/" + otherGroupId + "/courses"), otherTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + courseId + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("course_not_found")));

        // Ученик не из группы не видит курс.
        mockMvc.perform(authed(get("/api/courses/" + courseId), outsider))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(get("/api/courses"), outsider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Удаление участника учителем-владельцем.
        mockMvc.perform(authed(delete("/api/groups/" + groupId + "/members/"
                        + studentIdOf(student)), teacher))
                .andExpect(status().isNoContent());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Регистрирует пользователя и возвращает его JWT. */
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

    /** Достаёт id пользователя из его же токена через /api/auth/me. */
    private String studentIdOf(String token) throws Exception {
        String body = mockMvc.perform(authed(get("/api/auth/me"), token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }
}
