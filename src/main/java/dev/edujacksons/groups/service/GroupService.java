package dev.edujacksons.groups.service;

import dev.edujacksons.auth.domain.Role;
import dev.edujacksons.auth.service.UserDirectory;
import dev.edujacksons.auth.service.UserView;
import dev.edujacksons.common.api.ForbiddenException;
import dev.edujacksons.courses.service.CourseDirectory;
import dev.edujacksons.groups.domain.Group;
import dev.edujacksons.groups.domain.GroupCourse;
import dev.edujacksons.groups.domain.GroupMember;
import dev.edujacksons.groups.repository.GroupCourseRepository;
import dev.edujacksons.groups.repository.GroupMemberRepository;
import dev.edujacksons.groups.repository.GroupRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Публичный сервис модуля groups. Всё owner-scoped (учитель работает только со своими группами).
 * Общается с другими модулями только через границы: {@link UserDirectory} (auth) для поиска
 * ученика по email и {@link CourseDirectory} (courses) для проверки курса при назначении.
 * Границу {@link CourseAccessQuery} для модуля courses реализует отдельный тонкий бин
 * {@code CourseAccessQueryImpl} — чтобы не было цикла бинов с courses.
 */
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupCourseRepository courseRepository;
    private final UserDirectory userDirectory;
    private final CourseDirectory courseDirectory;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository memberRepository,
                        GroupCourseRepository courseRepository,
                        UserDirectory userDirectory,
                        CourseDirectory courseDirectory) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.userDirectory = userDirectory;
        this.courseDirectory = courseDirectory;
    }

    // ── группы ────────────────────────────────────────────────────────────────

    @Transactional
    public Group create(UUID ownerId, String title) {
        return groupRepository.save(new Group(ownerId, title));
    }

    @Transactional(readOnly = true)
    public List<Group> listOwned(UUID ownerId) {
        return groupRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public GroupDetail getDetail(UUID groupId, UUID ownerId) {
        Group group = requireOwned(groupId, ownerId);
        List<MemberView> members = memberRepository.findByGroupId(groupId).stream()
                .map(this::toMemberView)
                .toList();
        List<GroupCourse> courses = courseRepository.findByGroupId(groupId);
        return new GroupDetail(group, members, courses);
    }

    // ── участники ────────────────────────────────────────────────────────────

    @Transactional
    public MemberView addMember(UUID groupId, UUID ownerId, String email) {
        requireOwned(groupId, ownerId);
        UserView user = userDirectory.findByEmail(email)
                .orElseThrow(() -> new StudentNotFoundException(email));
        if (user.role() != Role.STUDENT) {
            throw new NotAStudentException(email);
        }
        if (memberRepository.existsByGroupIdAndStudentId(groupId, user.id())) {
            throw new AlreadyMemberException();
        }
        GroupMember member = memberRepository.save(new GroupMember(groupId, user.id()));
        return new MemberView(user.id(), user.email(), user.displayName(), member.getCreatedAt());
    }

    @Transactional
    public void removeMember(UUID groupId, UUID ownerId, UUID studentId) {
        requireOwned(groupId, ownerId);
        memberRepository.deleteByGroupIdAndStudentId(groupId, studentId);
    }

    // ── назначение курсов ──────────────────────────────────────────────────────

    /** Привязывает курс к группе. Идемпотентно: повторная привязка не создаёт дубликат. */
    @Transactional
    public void assignCourse(UUID groupId, UUID ownerId, UUID courseId) {
        requireOwned(groupId, ownerId);
        if (!courseDirectory.isOwnedBy(courseId, ownerId)) {
            throw new CourseNotAssignableException(courseId);
        }
        if (!courseRepository.existsByGroupIdAndCourseId(groupId, courseId)) {
            courseRepository.save(new GroupCourse(groupId, courseId));
        }
    }

    @Transactional
    public void unassignCourse(UUID groupId, UUID ownerId, UUID courseId) {
        requireOwned(groupId, ownerId);
        courseRepository.deleteByGroupIdAndCourseId(groupId, courseId);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Group requireOwned(UUID groupId, UUID ownerId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
        if (!group.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Группа принадлежит другому пользователю");
        }
        return group;
    }

    private MemberView toMemberView(GroupMember member) {
        return userDirectory.findById(member.getStudentId())
                .map(u -> new MemberView(u.id(), u.email(), u.displayName(), member.getCreatedAt()))
                .orElseGet(() -> new MemberView(
                        member.getStudentId(), null, null, member.getCreatedAt()));
    }
}
