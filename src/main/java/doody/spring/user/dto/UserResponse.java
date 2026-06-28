package doody.spring.user.dto;

import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;
import doody.spring.domain.type.GapAxis;
import doody.spring.domain.type.GoalChoice;
import doody.spring.domain.type.RecommendedPeriod;
import java.time.LocalDateTime;

public record UserResponse(
    String userId,
    String email,
    String nickname,
    Boolean isMydataLinked,
    Integer hanaMoney,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Onboarding onboarding
) {

    public static UserResponse from(User user, OnboardingResponse onboardingResponse, Integer hanaMoney) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getMydataLinked(),
            hanaMoney == null ? 0 : hanaMoney,
            user.getCreatedAt(),
            user.getUpdatedAt(),
            Onboarding.from(onboardingResponse)
        );
    }

    public record Onboarding(
        Long onboardingResponseId,
        GapAxis gapAxis,
        GoalChoice goalChoice,
        RecommendedPeriod recommendedPeriod,
        Integer rhythmChoice,
        Integer autonomyChoice,
        Integer connectionChoice,
        LocalDateTime createdAt
    ) {

        public static Onboarding from(OnboardingResponse onboardingResponse) {
            if (onboardingResponse == null) {
                return null;
            }

            return new Onboarding(
                onboardingResponse.getId(),
                onboardingResponse.getGapAxis(),
                onboardingResponse.getGoalChoice(),
                onboardingResponse.getRecommendedPeriod(),
                onboardingResponse.getRhythmChoice(),
                onboardingResponse.getAutonomyChoice(),
                onboardingResponse.getConnectionChoice(),
                onboardingResponse.getCreatedAt()
            );
        }
    }
}