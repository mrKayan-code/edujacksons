package dev.edujacksons.problems.api;

import dev.edujacksons.problems.api.dto.CreateProblemRequest;
import dev.edujacksons.problems.api.dto.CreateTestCaseRequest;
import dev.edujacksons.problems.api.dto.ProblemDetailResponse;
import dev.edujacksons.problems.api.dto.ProblemResponse;
import dev.edujacksons.problems.api.dto.TestCaseResponse;
import dev.edujacksons.problems.api.dto.UpdateProblemRequest;
import dev.edujacksons.problems.domain.Problem;
import dev.edujacksons.problems.service.ProblemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST модуля problems. Записи — только TEACHER-владелец курса ({@code @PreAuthorize} + проверка
 * владения в сервисе). Чтение — владельцу или ученику с доступом к курсу задачи. Ученику видны
 * только открытые (sample) тесты; полный набор — лишь владельцу.
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ProblemResponse> create(@AuthenticationPrincipal UUID userId,
                                                  @Valid @RequestBody CreateProblemRequest request) {
        ProblemResponse body = ProblemResponse.from(problemService.create(userId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<ProblemResponse> listByCourse(@AuthenticationPrincipal UUID userId,
                                              @RequestParam UUID courseId) {
        return problemService.listByCourse(courseId, userId).stream()
                .map(ProblemResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProblemDetailResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        Problem problem = problemService.getReadable(id, userId);
        boolean owner = problem.getOwnerId().equals(userId);
        var tests = owner
                ? problemService.listAllTests(id, userId)
                : problemService.listSampleTests(id, userId);
        return ProblemDetailResponse.from(problem, tests);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ProblemResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                  @Valid @RequestBody UpdateProblemRequest request) {
        return ProblemResponse.from(problemService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        problemService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── тесты ──────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/tests")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TestCaseResponse> addTest(@AuthenticationPrincipal UUID userId,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody CreateTestCaseRequest request) {
        TestCaseResponse body = TestCaseResponse.from(problemService.addTestCase(id, userId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}/tests")
    @PreAuthorize("hasRole('TEACHER')")
    public List<TestCaseResponse> listTests(@AuthenticationPrincipal UUID userId,
                                            @PathVariable UUID id) {
        return problemService.listAllTests(id, userId).stream().map(TestCaseResponse::from).toList();
    }

    @DeleteMapping("/{id}/tests/{testId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteTest(@AuthenticationPrincipal UUID userId,
                                           @PathVariable UUID id, @PathVariable UUID testId) {
        problemService.deleteTestCase(id, testId, userId);
        return ResponseEntity.noContent().build();
    }
}
