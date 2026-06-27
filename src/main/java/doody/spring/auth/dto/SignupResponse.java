package doody.spring.auth.dto;

import doody.spring.domain.entity.Goal;
import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;

public record SignupResponse(
    String userId,
    String email,
    String nickname,
    Boolean isMydataLinked,
    Long onboardingResponseId,
    Long goalId,
    String firstStepMission
) {

    public static SignupResponse from(User user, OnboardingResponse onboardingResponse, Goal goal) {
        return new SignupResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getMydataLinked(),
            onboardingResponse.getId(),
            goal.getId(),
            goal.getFirstStepMission()
        );
    }
}