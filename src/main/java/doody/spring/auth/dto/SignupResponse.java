package doody.spring.auth.dto;

import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;

public record SignupResponse(
    String userId,
    String email,
    String nickname,
    Boolean isMydataLinked,
    Long onboardingResponseId
) {

    public static SignupResponse from(User user, OnboardingResponse onboardingResponse) {
        return new SignupResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getMydataLinked(),
            onboardingResponse.getId()
        );
    }
}