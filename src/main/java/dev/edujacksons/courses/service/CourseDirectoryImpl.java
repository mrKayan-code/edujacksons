package dev.edujacksons.courses.service;

import dev.edujacksons.courses.repository.CourseRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация границы {@link CourseDirectory}. Тонкий адаптер над {@link CourseRepository} —
 * не зависит от {@code GroupService}, поэтому граф бинов courses↔groups остаётся ацикличным.
 */
@Service
class CourseDirectoryImpl implements CourseDirectory {

    private final CourseRepository courseRepository;

    CourseDirectoryImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnedBy(UUID courseId, UUID ownerId) {
        return courseRepository.existsByIdAndOwnerId(courseId, ownerId);
    }
}
