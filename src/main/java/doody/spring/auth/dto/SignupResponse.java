package doody.spring.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.domain.entity.Goal;
import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import java.util.List;
import java.util.Map;

public record SignupResponse(
    String userId,
    String email,
    String nickname,
    Boolean isMydataLinked,
    Long onboardingResponseId,
    Long goalId,
    String firstStepMission,
    @JsonProperty("direction_promise")
    String directionPromise,
    @JsonProperty("intro_message")
    String introMessage,
    @JsonProperty("recommended_period_options")
    List<String> recommendedPeriodOptions,
    @JsonProperty("recommended_period_message")
    String recommendedPeriodMessage,
    @JsonProperty("initial_ari")
    AriVector initialAri,
    AriVector goal,
    String period,
    @JsonProperty("start_axis")
    String startAxis,
    @JsonProperty("plan_summary")
    String planSummary,
    Map<String, Object> diagnostics,
    String source
) {

    public static SignupResponse from(
        User user,
        OnboardingResponse onboardingResponse,
        Goal goal,
        String directionPromise,
        String introMessage,
        List<String> recommendedPeriodOptions,
        String recommendedPeriodMessage,
        AriVector initialAri,
        AriVector goalAri,
        String startAxis,
        String planSummary,
        Map<String, Object> diagnostics,
        String source
    ) {
        return new SignupResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getMydataLinked(),
            onboardingResponse.getId(),
            goal.getId(),
            goal.getFirstStepMission(),
            directionPromise,
            introMessage,
            recommendedPeriodOptions,
            recommendedPeriodMessage,
            initialAri,
            goalAri,
            goal.getPeriod() == null ? null : goal.getPeriod().getValue(),
            startAxis,
            planSummary,
            diagnostics,
            source
        );
    }
}