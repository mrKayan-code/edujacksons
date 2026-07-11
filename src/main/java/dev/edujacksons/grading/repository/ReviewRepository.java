package dev.edujacksons.grading.repository;

import dev.edujacksons.grading.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для ручных проверок решений.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
}
