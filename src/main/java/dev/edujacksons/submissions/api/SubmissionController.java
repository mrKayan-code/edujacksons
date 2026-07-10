package dev.edujacksons.submissions.api;

import dev.edujacksons.submissions.api.dto.CreateSubmissionRequest;
import dev.edujacksons.submissions.api.dto.SubmissionDetailResponse;
import dev.edujacksons.submissions.api.dto.SubmissionResponse;
import dev.edujacksons.submissions.service.SubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST модуля submissions. Отправлять решение может любой аутентифицированный с доступом к задаче
 * (гейт в сервисе через ProblemDirectory); проверка асинхронна — сразу возвращаем QUEUED.
 * Читать решение может автор или владелец задачи. Пути к задаче/решению — без {@code /v1} (как auth).
 */
@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/api/problems/{problemId}/submissions")
    public ResponseEntity<SubmissionResponse> submit(@AuthenticationPrincipal UUID userId,
                                                     @PathVariable UUID problemId,
                                                     @Valid @RequestBody CreateSubmissionRequest request) {
        SubmissionResponse body = SubmissionResponse.from(
                submissionService.submit(userId, problemId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/api/problems/{problemId}/submissions")
    public List<SubmissionResponse> listMine(@AuthenticationPrincipal UUID userId,
                                             @PathVariable UUID problemId) {
        return submissionService.listForProblem(problemId, userId).stream()
                .map(SubmissionResponse::from).toList();
    }

    @GetMapping("/api/submissions/{id}")
    public SubmissionDetailResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return SubmissionDetailResponse.from(
                submissionService.getReadable(id, userId), submissionService.results(id, userId));
    }
}
