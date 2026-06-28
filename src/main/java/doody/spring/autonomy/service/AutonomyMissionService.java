package doody.spring.autonomy.service;

import doody.spring.autonomy.dto.AutonomyMissionCompleteResponse;
import doody.spring.autonomy.dto.AutonomyMissionDetailResponse;
import doody.spring.autonomy.dto.AutonomyMissionEvidenceResponse;
import doody.spring.autonomy.dto.AutonomyMissionRejectRequest;
import doody.spring.autonomy.dto.AutonomyMissionRejectResponse;
import doody.spring.autonomy.dto.AutonomyMissionStartResponse;
import doody.spring.autonomy.dto.AutonomyPathResponse;
import doody.spring.autonomy.dto.AutonomyPathResponse.Node;
import doody.spring.domain.entity.EnergyLog;
import doody.spring.domain.entity.Goal;
import doody.spring.domain.entity.MissionEvidence;
import doody.spring.domain.entity.MissionLog;
import doody.spring.domain.entity.MissionTemplate;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.EnergyLogRepository;
import doody.spring.domain.repository.GoalRepository;
import doody.spring.domain.repository.MissionEvidenceRepository;
import doody.spring.domain.repository.MissionLogRepository;
import doody.spring.domain.repository.MissionTemplateRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.common.service.RewardPersistenceService;
import doody.spring.mission.client.AiMissionActionClient;
import doody.spring.mission.client.AiMissionActionClient.Contact;
import doody.spring.mission.client.AiMissionActionClient.Dudy;
import doody.spring.mission.client.AiMissionActionClient.MissionCompleteRequest;
import doody.spring.mission.client.AiMissionActionClient.MissionCompleteResult;
import doody.spring.mission.client.AiMissionActionClient.MissionRejectAiRequest;
import doody.spring.mission.client.AiMissionActionClient.MissionRejectResult;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutonomyMissionService {

    private final UserRepository userRepository;
    private final MissionTemplateRepository missionTemplateRepository;
    private final MissionLogRepository missionLogRepository;
    private final MissionEvidenceRepository missionEvidenceRepository;
    private final GoalRepository goalRepository;
    private final EnergyLogRepository energyLogRepository;
    private final RewardPersistenceService rewardPersistenceService;
    private final AiMissionActionClient aiMissionActionClient;

    public AutonomyMissionService(
        UserRepository userRepository,
        MissionTemplateRepository missionTemplateRepository,
        MissionLogRepository missionLogRepository,
        MissionEvidenceRepository missionEvidenceRepository,
        GoalRepository goalRepository,
        EnergyLogRepository energyLogRepository,
        RewardPersistenceService rewardPersistenceService,
        AiMissionActionClient aiMissionActionClient
    ) {
        this.userRepository = userRepository;
        this.missionTemplateRepository = missionTemplateRepository;
        this.missionLogRepository = missionLogRepository;
        this.missionEvidenceRepository = missionEvidenceRepository;
        this.goalRepository = goalRepository;
        this.energyLogRepository = energyLogRepository;
        this.rewardPersistenceService = rewardPersistenceService;
        this.aiMissionActionClient = aiMissionActionClient;
    }

    @Transactional(readOnly = true)
    public AutonomyPathResponse getPath(String userId) {
        requireUser(userId);
        List<MissionTemplate> templates = autonomyTemplates();
        List<MissionLog> logs = missionLogRepository.findByUserIdAndAxis(userId, "AUTONOMY");

        int completedCount = (int) logs.stream()
            .filter(log -> log.getCompletedAt() != null)
            .map(log -> log.getMissionTemplate().getId())
            .distinct()
            .count();
        int currentStep = Math.min(completedCount + 1, Math.max(templates.size(), 1));

        List<Node> nodes = templates.stream()
            .map(template -> toNode(template, logs, currentStep))
            .toList();

        return new AutonomyPathResponse("?먮┰ON", currentStep, true, nodes);
    }

    @Transactional(readOnly = true)
    public AutonomyMissionDetailResponse getMission(String userId, String missionId) {
        requireUser(userId);
        return toDetail(requireAutonomyMission(missionId));
    }

    @Transactional
    public AutonomyMissionStartResponse start(String userId, String missionId) {
        User user = requireUser(userId);
        MissionTemplate mission = requireAutonomyMission(missionId);

        MissionLog log = missionLogRepository.findTopByUser_IdAndMissionTemplate_IdOrderByCreatedAtDesc(userId, missionId)
            .filter(existing -> existing.getCompletedAt() == null && existing.getSkippedAt() == null)
            .orElseGet(() -> missionLogRepository.save(MissionLog.start(user, mission)));

        return new AutonomyMissionStartResponse(log.getId(), mission.getId(), log.getActionType(), log.getStartedAt());
    }

    @Transactional
    public AutonomyMissionRejectResponse reject(String userId, String missionId, AutonomyMissionRejectRequest request) {
        User user = requireUser(userId);
        MissionTemplate mission = requireAutonomyMission(missionId);

        MissionRejectResult result = aiMissionActionClient.reject(new MissionRejectAiRequest(
            user.getId(),
            mission.getId(),
            request == null ? null : request.reasonText()
        ));
        MissionLog log = missionLogRepository.save(MissionLog.reject(user, mission));

        return new AutonomyMissionRejectResponse(
            log.getId(),
            mission.getId(),
            result.action(),
            result.message(),
            result.restOption()
        );
    }

    @Transactional
    public AutonomyMissionEvidenceResponse addEvidence(
        String userId,
        String missionId,
        String fileUrl,
        String contentType
    ) {
        User user = requireUser(userId);
        MissionTemplate mission = requireAutonomyMission(missionId);
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileUrl is required.");
        }

        MissionLog log = missionLogRepository.findTopByUser_IdAndMissionTemplate_IdOrderByCreatedAtDesc(userId, missionId)
            .filter(existing -> existing.getCompletedAt() == null && existing.getSkippedAt() == null)
            .orElseGet(() -> missionLogRepository.save(MissionLog.start(user, mission)));

        MissionEvidence evidence = missionEvidenceRepository.save(MissionEvidence.create(
            log,
            user,
            mission,
            fileUrl,
            contentType
        ));

        return new AutonomyMissionEvidenceResponse(
            evidence.getId(),
            log.getId(),
            mission.getId(),
            evidence.getFileUrl(),
            evidence.getContentType(),
            evidence.getCreatedAt()
        );
    }

    @Transactional
    public AutonomyMissionCompleteResponse complete(String userId, String missionId) {
        User user = requireUser(userId);
        MissionTemplate mission = requireAutonomyMission(missionId);

        MissionCompleteResult result = aiMissionActionClient.complete(new MissionCompleteRequest(
            user.getId(),
            mission.getId(),
            currentAri(user.getId()),
            latestEnergy(user.getId())
        ));

        MissionLog log = missionLogRepository.findTopByUser_IdAndMissionTemplate_IdOrderByCreatedAtDesc(userId, missionId)
            .filter(existing -> existing.getCompletedAt() == null && existing.getSkippedAt() == null)
            .orElseGet(() -> missionLogRepository.save(MissionLog.start(user, mission)));
        log.complete();

        updateGoalAri(user.getId(), result.updatedAri());
        saveReward(user, mission, log);
        saveCollectedDudy(user, log.getId(), result.collectedDudy());
        saveUnlockedContacts(user, log.getId(), result.unlockedContacts());

        return new AutonomyMissionCompleteResponse(
            log.getId(),
            mission.getId(),
            result.updatedAri(),
            result.appliedDelta(),
            result.eta(),
            mission.getReward(),
            result.completed(),
            result.signatureAvailable(),
            result.signatureCompleted(),
            result.contactUnlocked(),
            result.collectedDudy() == null ? List.of() : result.collectedDudy(),
            result.unlockedContacts() == null ? List.of() : result.unlockedContacts(),
            result.message()
        );
    }

    private Node toNode(MissionTemplate template, List<MissionLog> logs, int currentStep) {
        boolean completed = logs.stream()
            .anyMatch(log -> template.getId().equals(log.getMissionTemplate().getId()) && log.getCompletedAt() != null);
        int step = template.getStage() == null ? currentStep : template.getStage();
        String status;
        if (completed) {
            status = "COMPLETED";
        } else if (step == currentStep) {
            status = "CURRENT";
        } else {
            status = "LOCKED";
        }
        return new Node(step, status, template.getId(), template.getTitle());
    }

    private AutonomyMissionDetailResponse toDetail(MissionTemplate mission) {
        return new AutonomyMissionDetailResponse(
            mission.getId(),
            normalize(mission.getAxis()),
            mission.getStage(),
            mission.getTitle(),
            mission.getDescription(),
            mission.getReason(),
            split(mission.getHowTo()),
            split(mission.getGoalTags()),
            mission.getMissionType(),
            mission.getRequiredEvidenceCount(),
            mission.getReward(),
            mission.getExternalUrl(),
            mission.getFallbackMissionId(),
            mission.getFallback()
        );
    }

    private List<MissionTemplate> autonomyTemplates() {
        return missionTemplateRepository.findByActiveTrueOrderByIdAsc().stream()
            .filter(template -> "AUTONOMY".equals(normalize(template.getAxis())))
            .sorted(Comparator.comparing(template -> template.getStage() == null ? Integer.MAX_VALUE : template.getStage()))
            .toList();
    }

    private User requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
    }

    private MissionTemplate requireAutonomyMission(String missionId) {
        if (missionId == null || missionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missionId is required.");
        }
        MissionTemplate mission = missionTemplateRepository.findById(missionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "mission not found."));
        if (!"AUTONOMY".equals(normalize(mission.getAxis()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mission is not an autonomy mission.");
        }
        return mission;
    }

    private void saveReward(User user, MissionTemplate mission, MissionLog log) {
        Integer reward = mission.getReward();
        if (reward == null || reward <= 0) {
            return;
        }
        rewardPersistenceService.earnPoint(user, reward, mission.getTitle(), "MISSION", log.getId());
    }

    private void updateGoalAri(String userId, AriVector updatedAri) {
        if (updatedAri == null) {
            return;
        }
        goalRepository.findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId)
            .ifPresent(goal -> goal.updateAri(
                toBigDecimal(updatedAri.rhythm()),
                toBigDecimal(updatedAri.autonomy()),
                toBigDecimal(updatedAri.connection())
            ));
    }

    private AriVector currentAri(String userId) {
        Goal goal = goalRepository.findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId).orElse(null);
        return new AriVector(
            goal == null || goal.getRhythm() == null ? 0.8 : goal.getRhythm().doubleValue(),
            goal == null || goal.getAutonomy() == null ? 0.2 : goal.getAutonomy().doubleValue(),
            goal == null || goal.getConnection() == null ? 0.1 : goal.getConnection().doubleValue()
        );
    }

    private Short latestEnergy(String userId) {
        return energyLogRepository.findTopByUser_IdOrderByCreatedAtDesc(userId)
            .map(EnergyLog::getEnergy)
            .orElse((short) 3);
    }

    private void saveCollectedDudy(User user, Long missionLogId, List<Dudy> collectedDudy) {
        if (collectedDudy == null || collectedDudy.isEmpty()) {
            return;
        }
        for (Dudy dudy : collectedDudy) {
            rewardPersistenceService.collectDoody(
                user,
                dudy.id(),
                dudy.tier(),
                dudy.axis(),
                dudy.earnedReason() == null ? "mission complete" : dudy.earnedReason(),
                "MISSION",
                missionLogId
            );
        }
    }

    private void saveUnlockedContacts(User user, Long missionLogId, List<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return;
        }
        for (Contact contact : contacts) {
            rewardPersistenceService.unlockContact(
                user,
                contact.id(),
                contact.axis() == null ? "autonomy" : contact.axis(),
                "MISSION",
                missionLogId
            );
        }
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

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String normalize(String value) {
        return value == null ? null : value.strip().toUpperCase();
    }
}
