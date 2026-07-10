package dev.edujacksons.courses.api;

import dev.edujacksons.courses.api.dto.CourseDetailResponse;
import dev.edujacksons.courses.api.dto.CourseResponse;
import dev.edujacksons.courses.api.dto.CreateCourseRequest;
import dev.edujacksons.courses.api.dto.CreateMaterialRequest;
import dev.edujacksons.courses.api.dto.MaterialResponse;
import dev.edujacksons.courses.api.dto.UpdateCourseRequest;
import dev.edujacksons.courses.api.dto.UpdateMaterialRequest;
import dev.edujacksons.courses.service.CourseService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST модуля courses. Записи — только TEACHER-владелец ({@code @PreAuthorize} + проверка владения
 * в сервисе). Чтение — владельцу или ученику с доступом через группу (гейт в сервисе).
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseResponse> create(@AuthenticationPrincipal UUID userId,
                                                 @Valid @RequestBody CreateCourseRequest request) {
        CourseResponse body = CourseResponse.from(courseService.create(userId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<CourseResponse> list(@AuthenticationPrincipal UUID userId) {
        return courseService.listVisible(userId).stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CourseDetailResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return CourseDetailResponse.from(
                courseService.getReadable(id, userId), courseService.listMaterials(id, userId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateCourseRequest request) {
        return CourseResponse.from(courseService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        courseService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── материалы ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/materials")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MaterialResponse> addMaterial(@AuthenticationPrincipal UUID userId,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody CreateMaterialRequest request) {
        MaterialResponse body = MaterialResponse.from(courseService.addMaterial(id, userId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}/materials")
    public List<MaterialResponse> listMaterials(@AuthenticationPrincipal UUID userId,
                                                @PathVariable UUID id) {
        return courseService.listMaterials(id, userId).stream().map(MaterialResponse::from).toList();
    }

    @GetMapping("/{id}/materials/{materialId}")
    public MaterialResponse getMaterial(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                        @PathVariable UUID materialId) {
        return MaterialResponse.from(courseService.getMaterial(id, materialId, userId));
    }

    @PatchMapping("/{id}/materials/{materialId}")
    @PreAuthorize("hasRole('TEACHER')")
    public MaterialResponse updateMaterial(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                           @PathVariable UUID materialId,
                                           @Valid @RequestBody UpdateMaterialRequest request) {
        return MaterialResponse.from(courseService.updateMaterial(id, materialId, userId, request));
    }

    @DeleteMapping("/{id}/materials/{materialId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteMaterial(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                               @PathVariable UUID materialId) {
        courseService.deleteMaterial(id, materialId, userId);
        return ResponseEntity.noContent().build();
    }
}
