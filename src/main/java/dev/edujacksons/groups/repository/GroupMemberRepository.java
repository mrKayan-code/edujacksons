package dev.edujacksons.groups.repository;

import dev.edujacksons.groups.domain.GroupMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    List<GroupMember> findByGroupId(UUID groupId);

    boolean existsByGroupIdAndStudentId(UUID groupId, UUID studentId);

    long deleteByGroupIdAndStudentId(UUID groupId, UUID studentId);

    @Query("select m.groupId from GroupMember m where m.studentId = :studentId")
    List<UUID> findGroupIdsByStudentId(@Param("studentId") UUID studentId);
}
