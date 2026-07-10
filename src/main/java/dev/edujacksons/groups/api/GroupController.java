package dev.edujacksons.groups.api;

import dev.edujacksons.groups.api.dto.AddMemberRequest;
import dev.edujacksons.groups.api.dto.AssignCourseRequest;
import dev.edujacksons.groups.api.dto.AssignedCourseResponse;
import dev.edujacksons.groups.api.dto.CreateGroupRequest;
import dev.edujacksons.groups.api.dto.GroupDetailResponse;
import dev.edujacksons.groups.api.dto.GroupResponse;
import dev.edujacksons.groups.api.dto.MemberResponse;
import dev.edujacksons.groups.service.GroupDetail;
import dev.edujacksons.groups.service.GroupService;
import dev.edujacksons.groups.service.MemberView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST модуля groups. Всё — только для роли TEACHER и только над своими группами. */
@RestController
@RequestMapping("/api/groups")
@PreAuthorize("hasRole('TEACHER')")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(@AuthenticationPrincipal UUID userId,
                                                @Valid @RequestBody CreateGroupRequest request) {
        GroupResponse body = GroupResponse.from(groupService.create(userId, request.title()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<GroupResponse> list(@AuthenticationPrincipal UUID userId) {
        return groupService.listOwned(userId).stream().map(GroupResponse::from).toList();
    }

    @GetMapping("/{id}")
    public GroupDetailResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return toDetailResponse(groupService.getDetail(id, userId));
    }

    // ── участники ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(@AuthenticationPrincipal UUID userId,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody AddMemberRequest request) {
        MemberResponse body = toMemberResponse(groupService.addMember(id, userId, request.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @DeleteMapping("/{id}/members/{studentId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal UUID userId,
                                             @PathVariable UUID id, @PathVariable UUID studentId) {
        groupService.removeMember(id, userId, studentId);
        return ResponseEntity.noContent().build();
    }

    // ── назначение курсов ──────────────────────────────────────────────────────

    @PostMapping("/{id}/courses")
    public ResponseEntity<Void> assignCourse(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                             @Valid @RequestBody AssignCourseRequest request) {
        groupService.assignCourse(id, userId, request.courseId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    public ResponseEntity<Void> unassignCourse(@AuthenticationPrincipal UUID userId,
                                               @PathVariable UUID id, @PathVariable UUID courseId) {
        groupService.unassignCourse(id, userId, courseId);
        return ResponseEntity.noContent().build();
    }

    // ── маппинг ────────────────────────────────────────────────────────────────

    private GroupDetailResponse toDetailResponse(GroupDetail detail) {
        return new GroupDetailResponse(
                detail.group().getId(),
                detail.group().getTitle(),
                detail.group().getCreatedAt(),
                detail.members().stream().map(GroupController::toMemberResponse).toList(),
                detail.courses().stream().map(AssignedCourseResponse::from).toList());
    }

    private static MemberResponse toMemberResponse(MemberView view) {
        return new MemberResponse(view.studentId(), view.email(), view.displayName(), view.joinedAt());
    }
}
