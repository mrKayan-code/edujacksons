package dev.edujacksons.courses.service;

import dev.edujacksons.common.api.ForbiddenException;
import dev.edujacksons.courses.api.dto.CreateCourseRequest;
import dev.edujacksons.courses.api.dto.CreateMaterialRequest;
import dev.edujacksons.courses.api.dto.UpdateCourseRequest;
import dev.edujacksons.courses.api.dto.UpdateMaterialRequest;
import dev.edujacksons.courses.domain.Course;
import dev.edujacksons.courses.domain.Material;
import dev.edujacksons.courses.repository.CourseRepository;
import dev.edujacksons.courses.repository.MaterialRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Публичный сервис модуля courses. Записи owner-scoped (менять может только владелец-учитель);
 * чтение доступно владельцу или ученику, которому курс выдан через группу
 * ({@link CourseAccessQuery}). Границу {@link CourseDirectory} для модуля groups реализует
 * отдельный тонкий бин {@code CourseDirectoryImpl} — чтобы не было цикла бинов с groups.
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final MaterialRepository materialRepository;
    private final CourseAccessQuery courseAccessQuery;

    public CourseService(CourseRepository courseRepository,
                         MaterialRepository materialRepository,
                         CourseAccessQuery courseAccessQuery) {
        this.courseRepository = courseRepository;
        this.materialRepository = materialRepository;
        this.courseAccessQuery = courseAccessQuery;
    }

    // ── курсы ────────────────────────────────────────────────────────────────

    /**
     * Создает новый курс.
     *
     * @param ownerId id преподавателя-владельца
     * @param request данные курса (заголовок, описание)
     * @return созданный объект {@link Course}
     */
    @Transactional
    public Course create(UUID ownerId, CreateCourseRequest request) {
        return courseRepository.save(new Course(ownerId, request.title(), request.description()));
    }

    /**
     * Обновляет данные существующего курса.
     * <p>
     * Доступно только владельцу курса. Если курс принадлежит другому пользователю,
     * выбрасывается {@link ForbiddenException}.
     *
     * @param courseId id обновляемого курса
     * @param actorId  id пользователя, совершающего действие
     * @param request  новые данные курса
     * @return обновленный объект {@link Course}
     * @throws ForbiddenException если пользователь не является владельцем курса
     */
    @Transactional
    public Course update(UUID courseId, UUID actorId, UpdateCourseRequest request) {
        Course course = requireOwned(courseId, actorId);
        course.setTitle(request.title());
        course.setDescription(request.description());
        return course;
    }

    /**
     * Удаляет курс из системы.
     * <p>
     * Доступно только владельцу курса.
     *
     * @param courseId id удаляемого курса
     * @param actorId  id пользователя, совершающего действие
     * @throws ForbiddenException если пользователь не является владельцем курса
     */
    @Transactional
    public void delete(UUID courseId, UUID actorId) {
        courseRepository.delete(requireOwned(courseId, actorId));
    }

    /**
     * Возвращает список всех курсов, которые доступны пользователю.
     * <p>
     * Список включает:
     * 1. Курсы, где пользователь является владельцем (для учителей).
     * 2. Курсы, которые были назначены группам, в которых состоит пользователь (для учеников).
     * Результаты объединяются и сортируются по дате создания (сначала новые).
     *
     * @param actorId id пользователя, для которого ищутся курсы
     * @return отсортированный список доступных курсов
     */
    @Transactional(readOnly = true)
    public List<Course> listVisible(UUID actorId) {
        Map<UUID, Course> byId = new LinkedHashMap<>();
        for (Course c : courseRepository.findByOwnerIdOrderByCreatedAtDesc(actorId)) {
            byId.put(c.getId(), c);
        }
        Set<UUID> accessible = courseAccessQuery.accessibleCourseIds(actorId);
        if (!accessible.isEmpty()) {
            for (Course c : courseRepository.findByIdInOrderByCreatedAtDesc(accessible)) {
                byId.putIfAbsent(c.getId(), c);
            }
        }
        return List.copyOf(byId.values());
    }

    /**
     * Возвращает данные конкретного курса, если у пользователя есть права на его чтение.
     * <p>
     * Доступ разрешен, если:
     * - Пользователь является владельцем курса.
     * - Пользователь имеет доступ через привязку к группе.
     *
     * @param courseId id курса
     * @param actorId  id пользователя, запрашивающего доступ
     * @return объект {@link Course}
     * @throws CourseNotFoundException если курс не найден
     * @throws ForbiddenException      если у пользователя нет прав на чтение
     */
    @Transactional(readOnly = true)
    public Course getReadable(UUID courseId, UUID actorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        requireReadAccess(course, actorId);
        return course;
    }

    // ── материалы ────────────────────────────────────────────────────────────

    @Transactional
    public Material addMaterial(UUID courseId, UUID actorId, CreateMaterialRequest request) {
        requireOwned(courseId, actorId);
        int orderIndex = request.orderIndex() != null ? request.orderIndex() : nextOrderIndex(courseId);
        return materialRepository.save(
                new Material(courseId, request.type(), request.title(), request.body(), orderIndex));
    }

    @Transactional
    public Material updateMaterial(UUID courseId, UUID materialId, UUID actorId,
                                   UpdateMaterialRequest request) {
        requireOwned(courseId, actorId);
        Material material = requireMaterialOfCourse(materialId, courseId);
        material.setType(request.type());
        material.setTitle(request.title());
        material.setBody(request.body());
        material.setOrderIndex(request.orderIndex());
        return material;
    }

    @Transactional
    public void deleteMaterial(UUID courseId, UUID materialId, UUID actorId) {
        requireOwned(courseId, actorId);
        materialRepository.delete(requireMaterialOfCourse(materialId, courseId));
    }

    @Transactional(readOnly = true)
    public List<Material> listMaterials(UUID courseId, UUID actorId) {
        requireReadAccess(requireCourse(courseId), actorId);
        return materialRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Material getMaterial(UUID courseId, UUID materialId, UUID actorId) {
        requireReadAccess(requireCourse(courseId), actorId);
        return requireMaterialOfCourse(materialId, courseId);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Course requireCourse(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private Course requireOwned(UUID courseId, UUID actorId) {
        Course course = requireCourse(courseId);
        if (!course.getOwnerId().equals(actorId)) {
            throw new ForbiddenException("Курс принадлежит другому пользователю");
        }
        return course;
    }

    private void requireReadAccess(Course course, UUID actorId) {
        if (!course.getOwnerId().equals(actorId) && !courseAccessQuery.canAccess(actorId, course.getId())) {
            throw new ForbiddenException("Нет доступа к курсу");
        }
    }

    private Material requireMaterialOfCourse(UUID materialId, UUID courseId) {
        return materialRepository.findByIdAndCourseId(materialId, courseId)
                .orElseThrow(() -> new MaterialNotFoundException(materialId));
    }

    private int nextOrderIndex(UUID courseId) {
        List<Material> existing = materialRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getOrderIndex() + 1;
    }
}
