package dev.edujacksons.groups.repository;

import dev.edujacksons.groups.domain.Group;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    List<Group> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
