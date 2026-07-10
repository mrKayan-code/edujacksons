package dev.edujacksons.problems.api.dto;

import dev.edujacksons.problems.domain.TestCase;
import java.util.UUID;

public record TestCaseResponse(
        UUID id,
        String input,
        String expectedOutput,
        boolean sample,
        int orderIndex
) {
    public static TestCaseResponse from(TestCase tc) {
        return new TestCaseResponse(tc.getId(), tc.getInput(), tc.getExpectedOutput(),
                tc.isSample(), tc.getOrderIndex());
    }
}
