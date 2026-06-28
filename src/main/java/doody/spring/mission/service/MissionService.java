package doody.spring.mission.service;

import doody.spring.domain.entity.EnergyLog;
import doody.spring.domain.entity.Goal;
import doody.spring.domain.entity.MissionLog;
import doody.spring.domain.entity.MissionTemplate;
import doody.spring.domain.entity.RhythmLog;
import doody.spring.domain.repository.EnergyLogRepository;
import doody.spring.domain.repository.GoalRepository;
import doody.spring.domain.repository.MissionLogRepository;
import doody.spring.domain.repository.MissionTemplateRepository;
import doody.spring.domain.repository.RhythmLogRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.mission.client.AiMissionRecommendClient;
import doody.spring.mission.client.AiMissionRecommendClient.AiMissionRecommendRequest;
import doody.spring.mission.client.AiMissionRecommendClient.EveningHistory;
import doody.spring.mission.client.AiMissionRecommendClient.MissionHistory;
import doody.spring.mission.client.AiMissionRecommendClient.MorningHistory;
import doody.spring.mission.client.AiMissionRecommendClient.RhythmHistory;
import doody.spring.mission.dto.TodayMissionResponse;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import doody.spring.mission.dto.TodayMissionResponse.Mission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MissionService {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final EnergyLogRepository energyLogRepository;
    private final RhythmLogRepository rhythmLogRepository;
    private final MissionLogRepository missionLogRepository;
    private final MissionTemplateRepository missionTemplateRepository;
    private final AiMissionRecommendClient aiMissionRecommendClient;

    public MissionService(
        UserRepository userRepository,
        GoalRepository goalRepository,
        EnergyLogRepository energyLogRepository,
        RhythmLogRepository rhythmLogRepository,
        MissionLogRepository missionLogRepository,
        MissionTemplateRepository missionTemplateRepository,
        AiMissionRecommendClient aiMissionRecommendClient
    ) {
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.energyLogRepository = energyLogRepository;
        this.rhythmLogRepository = rhythmLogRepository;
        this.missionLogRepository = missionLogRepository;
        this.missionTemplateRepository = missionTemplateRepository;
        this.aiMissionRecommendClient = aiMissionRecommendClient;
    }

    @Transactional(readOnly = true)
    public TodayMissionResponse getTodayMission(String userId) {
        validateUserId(userId);
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found.");
        }

        Goal activeGoal = goalRepository.findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId).orElse(null);
        Short energy = energyLogRepository.findTopByUser_IdOrderByCreatedAtDesc(userId)
            .map(EnergyLog::getEnergy)
            .orElse((short) 3);

        AiMissionRecommendRequest request = new AiMissionRecommendRequest(
            userId,
            currentAri(activeGoal, userId),
            goalAri(activeGoal),
            List.of(),
            energy,
            rhythmHistory(userId),
            missionHistory(userId)
        );

        Mission fallbackMission = fallbackMission();
        return aiMissionRecommendClient.recommend(request, fallbackMission);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
    }

    private AriVector currentAri(Goal goal, String userId) {
        return new AriVector(
            rhythmReadiness(userId),
            decimalOrDefault(goal == null ? null : goal.getAutonomy(), 0.2),
            decimalOrDefault(goal == null ? null : goal.getConnection(), 0.1)
        );
    }

    private AriVector goalAri(Goal goal) {
        return new AriVector(
            decimalOrDefault(goal == null ? null : goal.getRhythm(), 0.8),
            decimalOrDefault(goal == null ? null : goal.getAutonomy(), 0.8),
            decimalOrDefault(goal == null ? null : goal.getConnection(), 0.4)
        );
    }

    private Double rhythmReadiness(String userId) {
        LocalDate today = LocalDate.now();
        boolean hasMorning = rhythmLogRepository.findByUser_IdAndTimestampBetweenOrderByTimestampDesc(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay().minusNanos(1)
            ).stream()
            .anyMatch(log -> "MORNING".equals(log.getRhythmType()));
        return hasMorning ? 0.8 : 0.2;
    }

    private RhythmHistory rhythmHistory(String userId) {
        List<RhythmLog> logs = rhythmLogRepository.findTop20ByUser_IdOrderByTimestampDesc(userId);
        List<MorningHistory> morning = logs.stream()
            .filter(log -> "MORNING".equals(log.getRhythmType()))
            .limit(14)
            .map(log -> new MorningHistory(log.getTimestamp(), true))
            .toList();
        List<EveningHistory> evening = logs.stream()
            .filter(log -> "EVENING".equals(log.getRhythmType()))
            .limit(14)
            .map(log -> new EveningHistory(log.getTimestamp(), log.getText()))
            .toList();
        return new RhythmHistory(morning, evening);
    }

    private List<MissionHistory> missionHistory(String userId) {
        return missionLogRepository.findTop20ByUser_IdOrderByCreatedAtDesc(userId).stream()
            .filter(log -> log.getMissionTemplate() != null)
            .map(this::toMissionHistory)
            .toList();
    }

    private MissionHistory toMissionHistory(MissionLog log) {
        return new MissionHistory(
            log.getMissionTemplate().getId(),
            log.getMissionTemplate().getAxis(),
            log.getMissionTemplate().getTitle(),
            log.getCompletedAt()
        );
    }

    private Mission fallbackMission() {
        return missionTemplateRepository.findByActiveTrueOrderByIdAsc().stream()
            .filter(template -> isTargetAxis(template.getAxis()))
            .findFirst()
            .map(this::toMission)
            .orElse(null);
    }

    private boolean isTargetAxis(String axis) {
        String normalized = normalize(axis);
        return "AUTONOMY".equals(normalized) || "CONNECTION".equals(normalized);
    }

    private Mission toMission(MissionTemplate template) {
        return new Mission(
            template.getId(),
            template.getId(),
            normalize(template.getAxis()),
            template.getStage(),
            template.getWaypoint(),
            template.getDifficulty(),
            defaultDelta(template.getAxis()),
            template.getTitle(),
            template.getDescription(),
            template.getMissionType(),
            template.getRequiredEvidenceCount(),
            template.getSignature(),
            template.getFallback(),
            template.getFallbackMissionId(),
            split(template.getGoalTags()),
            split(template.getHowTo()),
            template.getReason() == null ? "Today mission is ready." : template.getReason()
        );
    }

    private AriVector defaultDelta(String axis) {
        String normalized = normalize(axis);
        if ("CONNECTION".equals(normalized)) {
            return new AriVector(0.0, 0.0, 0.01);
        }
        return new AriVector(0.0, 0.01, 0.0);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\r?\\n|,"))
            .map(String::strip)
            .filter(item -> !item.isBlank())
            .toList();
    }

    private String normalize(String value) {
        return value == null ? null : value.strip().toUpperCase();
    }

    private Double decimalOrDefault(BigDecimal value, Double defaultValue) {
        return value == null ? defaultValue : value.doubleValue();
    }
}
