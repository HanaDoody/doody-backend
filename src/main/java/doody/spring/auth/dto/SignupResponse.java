package doody.spring.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.domain.entity.Goal;
import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import java.util.Map;

public record SignupResponse(
    String userId,
    String email,
    String nickname,
    Boolean isMydataLinked,
    Long onboardingResponseId,
    Long goalId,
    String firstStepMission,
    @JsonProperty("initial_ari")
    AriVector initialAri,
    AriVector goal,
    String period,
    @JsonProperty("start_axis")
    String startAxis,
    @JsonProperty("plan_summary")
    String planSummary,
    Map<String, Object> diagnostics
) {

    public static SignupResponse from(
        User user,
        OnboardingResponse onboardingResponse,
        Goal goal,
        AriVector initialAri,
        AriVector goalAri,
        String planSummary
    ) {
        return new SignupResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getMydataLinked(),
            onboardingResponse.getId(),
            goal.getId(),
            goal.getFirstStepMission(),
            initialAri,
            goalAri,
            goal.getPeriod() == null ? null : goal.getPeriod().getValue(),
            goal.getTitle(),
            planSummary,
            null
        );
    }
}