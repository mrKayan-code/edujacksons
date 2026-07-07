package dev.edujacksons.groups.service;

import dev.edujacksons.groups.repository.GroupCourseRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация границы {@link CourseAccessQuery}. Тонкий адаптер над {@link GroupCourseRepository} —
 * не зависит от {@code CourseService}, поэтому граф бинов courses↔groups остаётся ацикличным.
 */
@Service
class CourseAccessQueryImpl implements CourseAccessQuery {

    private final GroupCourseRepository groupCourseRepository;

    CourseAccessQueryImpl(GroupCourseRepository groupCourseRepository) {
        this.groupCourseRepository = groupCourseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> accessibleCourseIds(UUID studentId) {
        return groupCourseRepository.findAccessibleCourseIds(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccess(UUID studentId, UUID courseId) {
        return groupCourseRepository.isCourseAccessible(studentId, courseId);
    }
}
