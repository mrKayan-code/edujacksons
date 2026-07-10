package dev.edujacksons.groups.repository;

import dev.edujacksons.groups.domain.GroupCourse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupCourseRepository extends JpaRepository<GroupCourse, UUID> {

    List<GroupCourse> findByGroupId(UUID groupId);

    boolean existsByGroupIdAndCourseId(UUID groupId, UUID courseId);

    long deleteByGroupIdAndCourseId(UUID groupId, UUID courseId);

    /** Курсы, доступные ученику через его группы (для {@code CourseAccessQuery}). */
    @Query("select gc.courseId from GroupCourse gc where gc.groupId in "
            + "(select m.groupId from GroupMember m where m.studentId = :studentId)")
    Set<UUID> findAccessibleCourseIds(@Param("studentId") UUID studentId);

    /** Доступен ли ученику конкретный курс через какую-либо его группу. */
    @Query("select count(gc) > 0 from GroupCourse gc where gc.courseId = :courseId and gc.groupId in "
            + "(select m.groupId from GroupMember m where m.studentId = :studentId)")
    boolean isCourseAccessible(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);
}
