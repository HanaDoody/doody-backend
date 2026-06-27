package doody.spring.auth.dto;

import doody.spring.domain.type.GapAxis;
import doody.spring.domain.type.GoalChoice;
import doody.spring.domain.type.RecommendedPeriod;

public record SignupRequest(
    String email,
    String nickname,
    GapAxis gapAxis,
    GoalChoice goalChoice,
    RecommendedPeriod recommendedPeriod,
    Integer rhythmChoice,
    Integer autonomyChoice,
    Integer connectionChoice,
    Boolean isMydataLinked,
    String autonomyGoal,
    String connectionGoal,
    String firstStepMission
) {
}