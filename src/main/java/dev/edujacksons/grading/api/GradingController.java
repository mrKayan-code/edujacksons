package dev.edujacksons.grading.api;

import dev.edujacksons.grading.api.dto.*;
import dev.edujacksons.grading.service.GradingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST модуля grading. Ручная проверка решений учителем.
 * Пути без /v1, как в остальных модулях.
 */
/**
 * REST-контроллер модуля grading.
 * 
 * Предоставляет API для управления ручными проверками решений.
 * Особенностью контроллера является полиморфизм ответов для методов получения проверки:
 * - Для пользователей с ролью TEACHER возвращается полный {@link ReviewResponse}.
 * - Для пользователей с ролью STUDENT возвращается усечённый {@link StudentReviewResponse}.
 * 
 * При этом черновики (DRAFT) для учеников выглядят как отсутствующие ресурсы (404 Not Found),
 * чтобы скрыть внутренний процесс подготовки оценки учителем.
 */
@RestController
public class GradingController {

    private final GradingService gradingService;

    public GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @PostMapping("/api/submissions/{submissionId}/review")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID submissionId,
            @RequestBody CreateReviewRequest request) {
        ReviewResponse body = gradingService.createReview(submissionId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PatchMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID reviewId,
            @RequestBody UpdateReviewRequest request) {
        ReviewResponse body = gradingService.updateReview(reviewId, request, userId);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/reviews/{reviewId}")
    public ResponseEntity<?> getReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID reviewId,
            Authentication authentication) {
        if (hasRole(authentication, "ROLE_TEACHER")) {
            return ResponseEntity.ok(gradingService.getReviewForTeacher(reviewId));
        }
        return ResponseEntity.ok(gradingService.getReviewForStudent(reviewId, userId));
    }

    @GetMapping("/api/submissions/{submissionId}/review")
    public ResponseEntity<?> getReviewBySubmission(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID submissionId,
            Authentication authentication) {
        if (hasRole(authentication, "ROLE_TEACHER")) {
            return ResponseEntity.ok(gradingService.getReviewForTeacherBySubmission(submissionId));
        }
        return ResponseEntity.ok(gradingService.getReviewForStudentBySubmission(submissionId, userId));
    }

    @GetMapping("/api/problems/{problemId}/gradebook")
    public ResponseEntity<List<GradebookEntryResponse>> getGradebook(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID problemId,
            @RequestParam(required = false) Boolean reviewed) {
        return ResponseEntity.ok(gradingService.getGradebook(problemId, userId, reviewed));
    }

    @GetMapping("/api/problems/{problemId}/reviews/mine")
    public ResponseEntity<List<StudentReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID problemId) {
        return ResponseEntity.ok(gradingService.getMyReviews(problemId, userId));
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
