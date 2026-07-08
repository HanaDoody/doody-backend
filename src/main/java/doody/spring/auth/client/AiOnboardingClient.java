package doody.spring.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.auth.dto.SignupRequest;
import doody.spring.domain.type.RecommendedPeriod;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiOnboardingClient {

    private static final List<String> PERIOD_OPTIONS = List.of("today", "1w", "1m", "3m");

    private final String baseUrl;
    private final RestClient restClient;

    public AiOnboardingClient(
        @Value("${AI_ENGINE_BASE_URL:}") String baseUrl,
        @Qualifier("aiEngineRestClient") RestClient restClient
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = restClient;
    }

    public AiOnboardingResult onboard(String userId, SignupRequest request) {
        AiIntroResponse intro = intro(userId, request);
        AiRecommendPeriodResponse recommendation = recommendPeriod(userId, request);
        RecommendedPeriod period = resolvePeriod(recommendation.recommendedPeriod(), request.recommendedPeriod());
        AiCompleteResponse complete = complete(userId, request, period);

        RecommendedPeriod finalPeriod = resolvePeriod(complete.period(), period);

        return new AiOnboardingResult(
            intro.directionPromise(),
            intro.message(),
            finalPeriod,
            recommendation.options(),
            recommendation.message(),
            normalizeAri(complete.initialAri(), fallbackInitialAri(request)),
            normalizeAri(complete.goal(), new AriVector(0.8, 0.8, 0.4)),
            complete.startAxis(),
            complete.firstStepMission(),
            complete.planSummary(),
            complete.diagnostics(),
            baseUrl.isBlank() ? "FALLBACK" : "AI"
        );
    }

    private AiIntroResponse intro(String userId, SignupRequest request) {
        AiIntroResponse fallback = new AiIntroResponse(
            "오늘 상태에 맞춰서 천천히 시작해볼게.",
            "두디가 부담 없는 속도로 같이 시작해줄게."
        );
        if (baseUrl.isBlank()) {
            return fallback;
        }

        try {
            AiIntroResponse response = restClient.post()
                .uri(baseUrl + "/onboarding/intro")
                .body(new AiIntroRequest(userId, axis(request), energy(request)))
                .retrieve()
                .body(AiIntroResponse.class);
            return response == null ? fallback : response.withFallback(fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private AiRecommendPeriodResponse recommendPeriod(String userId, SignupRequest request) {
        AiRecommendPeriodResponse fallback = new AiRecommendPeriodResponse(
            request.recommendedPeriod() == null ? "1w" : request.recommendedPeriod().getValue(),
            PERIOD_OPTIONS,
            "처음엔 지키기 쉬운 작은 기간부터 시작해보자."
        );
        if (baseUrl.isBlank()) {
            return fallback;
        }

        try {
            AiRecommendPeriodResponse response = restClient.post()
                .uri(baseUrl + "/onboarding/recommend-period")
                .body(new AiRecommendPeriodRequest(userId, goalChoice(request), axis(request), energy(request)))
                .retrieve()
                .body(AiRecommendPeriodResponse.class);
            return response == null ? fallback : response.withFallback(fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private AiCompleteResponse complete(String userId, SignupRequest request, RecommendedPeriod period) {
        AiCompleteResponse fallback = new AiCompleteResponse(
            fallbackInitialAri(request),
            new AriVector(0.8, 0.8, 0.4),
            period.getValue(),
            "rhythm",
            fallbackFirstStepMission(request),
            "먼저 리듬을 잡고, 자립과 연결로 천천히 이어가자.",
            Map.of("source", "fallback")
        );
        if (baseUrl.isBlank()) {
            return fallback;
        }

        try {
            AiCompleteResponse response = restClient.post()
                .uri(baseUrl + "/onboarding/complete")
                .body(new AiCompleteRequest(
                    userId,
                    goalChoice(request),
                    new Diagnosis(request.rhythmChoice(), request.autonomyChoice(), request.connectionChoice()),
                    period.getValue(),
                    axis(request),
                    new Mydata(Boolean.TRUE.equals(request.isMydataLinked()))
                ))
                .retrieve()
                .body(AiCompleteResponse.class);
            return response == null ? fallback : response.withFallback(fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private RecommendedPeriod resolvePeriod(String period, RecommendedPeriod fallback) {
        if (period == null || period.isBlank()) {
            return fallback == null ? RecommendedPeriod.ONE_WEEK : fallback;
        }
        try {
            return RecommendedPeriod.from(period);
        } catch (IllegalArgumentException exception) {
            return fallback == null ? RecommendedPeriod.ONE_WEEK : fallback;
        }
    }

    private AriVector fallbackInitialAri(SignupRequest request) {
        return new AriVector(
            scoreToAri(request.rhythmChoice()),
            scoreToAri(request.autonomyChoice()),
            scoreToAri(request.connectionChoice())
        );
    }

    private AriVector normalizeAri(AriVector value, AriVector fallback) {
        if (value == null) {
            return fallback;
        }
        return new AriVector(
            value.rhythm() == null ? fallback.rhythm() : value.rhythm(),
            value.autonomy() == null ? fallback.autonomy() : value.autonomy(),
            value.connection() == null ? fallback.connection() : value.connection()
        );
    }

    private double scoreToAri(Integer score) {
        if (score == null) {
            return 0.2;
        }
        return Math.max(0.1, Math.min(score, 5) / 5.0);
    }

    private String axis(SignupRequest request) {
        return request.gapAxis() == null ? "UNKNOWN" : request.gapAxis().getValue();
    }

    private String goalChoice(SignupRequest request) {
        return request.goalChoice() == null ? null : request.goalChoice().getValue();
    }

    private String fallbackFirstStepMission(SignupRequest request) {
        if (request.firstStepMission() != null && !request.firstStepMission().isBlank()) {
            return request.firstStepMission();
        }
        return "오늘 할 수 있는 작은 미션부터 시작하기";
    }

    private Integer energy(SignupRequest request) {
        return request.rhythmChoice() == null ? 3 : request.rhythmChoice();
    }

    public record AiOnboardingResult(
        String directionPromise,
        String introMessage,
        RecommendedPeriod period,
        List<String> recommendedPeriodOptions,
        String recommendedPeriodMessage,
        AriVector initialAri,
        AriVector goal,
        String startAxis,
        String firstStepMission,
        String planSummary,
        Map<String, Object> diagnostics,
        String source
    ) {
    }

    private record AiIntroRequest(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("gap_axis")
        String gapAxis,
        Integer energy
    ) {
    }

    private record AiIntroResponse(
        @JsonProperty("direction_promise")
        String directionPromise,
        String message
    ) {
        AiIntroResponse withFallback(AiIntroResponse fallback) {
            return new AiIntroResponse(
                directionPromise == null || directionPromise.isBlank() ? fallback.directionPromise() : directionPromise,
                message == null || message.isBlank() ? fallback.message() : message
            );
        }
    }

    private record AiRecommendPeriodRequest(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("goal_choice")
        String goalChoice,
        @JsonProperty("gap_axis")
        String gapAxis,
        Integer energy
    ) {
    }

    private record AiRecommendPeriodResponse(
        @JsonProperty("recommended_period")
        String recommendedPeriod,
        List<String> options,
        String message
    ) {
        AiRecommendPeriodResponse withFallback(AiRecommendPeriodResponse fallback) {
            return new AiRecommendPeriodResponse(
                recommendedPeriod == null || recommendedPeriod.isBlank() ? fallback.recommendedPeriod() : recommendedPeriod,
                options == null || options.isEmpty() ? fallback.options() : options,
                message == null || message.isBlank() ? fallback.message() : message
            );
        }
    }

    private record AiCompleteRequest(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("goal_choice")
        String goalChoice,
        Diagnosis diagnosis,
        String period,
        @JsonProperty("gap_axis")
        String gapAxis,
        Mydata mydata
    ) {
    }

    private record Diagnosis(
        Integer rhythm,
        Integer autonomy,
        Integer connection
    ) {
    }

    private record Mydata(
        Boolean linked
    ) {
    }

    private record AiCompleteResponse(
        @JsonProperty("initial_ari")
        AriVector initialAri,
        AriVector goal,
        String period,
        @JsonProperty("start_axis")
        String startAxis,
        @JsonProperty("first_step_mission")
        String firstStepMission,
        @JsonProperty("plan_summary")
        String planSummary,
        Map<String, Object> diagnostics
    ) {
        AiCompleteResponse withFallback(AiCompleteResponse fallback) {
            return new AiCompleteResponse(
                initialAri == null ? fallback.initialAri() : initialAri,
                goal == null ? fallback.goal() : goal,
                period == null || period.isBlank() ? fallback.period() : period,
                startAxis == null || startAxis.isBlank() ? fallback.startAxis() : startAxis,
                firstStepMission == null || firstStepMission.isBlank() ? fallback.firstStepMission() : firstStepMission,
                planSummary == null || planSummary.isBlank() ? fallback.planSummary() : planSummary,
                diagnostics == null ? fallback.diagnostics() : diagnostics
            );
        }
    }
}
