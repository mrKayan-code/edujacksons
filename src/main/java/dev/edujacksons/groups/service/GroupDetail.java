package dev.edujacksons.groups.service;

import dev.edujacksons.groups.domain.Group;
import dev.edujacksons.groups.domain.GroupCourse;
import java.util.List;

/** Группа с участниками и привязанными курсами. Сервис-слойное представление для детального экрана. */
public record GroupDetail(Group group, List<MemberView> members, List<GroupCourse> courses) {
}
