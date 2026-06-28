package doody.spring.rhythm.service;

import doody.spring.domain.entity.DoodyCollection;
import doody.spring.domain.entity.DoodyTemplate;
import doody.spring.domain.entity.EnergyLog;
import doody.spring.domain.entity.PointTransaction;
import doody.spring.domain.entity.RhythmLog;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.DoodyCollectionRepository;
import doody.spring.domain.repository.DoodyTemplateRepository;
import doody.spring.domain.repository.EnergyLogRepository;
import doody.spring.domain.repository.MissionLogRepository;
import doody.spring.domain.repository.PointTransactionRepository;
import doody.spring.domain.repository.RhythmLogRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.rhythm.client.AiEveningRhythmClient;
import doody.spring.rhythm.client.AiEveningRhythmClient.AiEveningResult;
import doody.spring.rhythm.client.AiMorningRhythmClient;
import doody.spring.rhythm.client.AiMorningRhythmClient.AiMorningResult;
import doody.spring.rhythm.dto.EveningRhythmRequest;
import doody.spring.rhythm.dto.EveningRhythmResponse;
import doody.spring.rhythm.dto.MorningRhythmRequest;
import doody.spring.rhythm.dto.MorningRhythmResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RhythmService {

    private final UserRepository userRepository;
    private final RhythmLogRepository rhythmLogRepository;
    private final EnergyLogRepository energyLogRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final DoodyTemplateRepository doodyTemplateRepository;
    private final DoodyCollectionRepository doodyCollectionRepository;
    private final MissionLogRepository missionLogRepository;
    private final AiMorningRhythmClient aiMorningRhythmClient;
    private final AiEveningRhythmClient aiEveningRhythmClient;

    public RhythmService(
        UserRepository userRepository,
        RhythmLogRepository rhythmLogRepository,
        EnergyLogRepository energyLogRepository,
        PointTransactionRepository pointTransactionRepository,
        DoodyTemplateRepository doodyTemplateRepository,
        DoodyCollectionRepository doodyCollectionRepository,
        MissionLogRepository missionLogRepository,
        AiMorningRhythmClient aiMorningRhythmClient,
        AiEveningRhythmClient aiEveningRhythmClient
    ) {
        this.userRepository = userRepository;
        this.rhythmLogRepository = rhythmLogRepository;
        this.energyLogRepository = energyLogRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.doodyTemplateRepository = doodyTemplateRepository;
        this.doodyCollectionRepository = doodyCollectionRepository;
        this.missionLogRepository = missionLogRepository;
        this.aiMorningRhythmClient = aiMorningRhythmClient;
        this.aiEveningRhythmClient = aiEveningRhythmClient;
    }

    @Transactional
    public MorningRhythmResponse checkInMorning(MorningRhythmRequest request) {
        validateMorning(request);

        User user = getUser(request.userId());
        LocalDateTime timestamp = request.timestamp() == null ? LocalDateTime.now() : request.timestamp();

        AiMorningResult aiResult = aiMorningRhythmClient.checkIn(user.getId(), timestamp, request.energy());

        RhythmLog rhythmLog = rhythmLogRepository.save(RhythmLog.createMorning(
            user,
            timestamp,
            aiResult.greeting(),
            aiResult.hanaMoney()
        ));

        EnergyLog energyLog = energyLogRepository.save(EnergyLog.create(
            user,
            timestamp.toLocalDate(),
            request.energy(),
            rhythmLog
        ));

        savePointIfNeeded(user, aiResult.hanaMoney(), "morning rhythm check-in", rhythmLog.getId());

        List<MorningRhythmResponse.CollectedDudy> savedDudy = saveMorningCollectedDudy(
            user,
            rhythmLog.getId(),
            aiResult.collectedDudy()
        );

        return new MorningRhythmResponse(
            rhythmLog.getId(),
            energyLog.getId(),
            new MorningRhythmResponse.Reward(aiResult.hanaMoney()),
            aiResult.greeting(),
            savedDudy,
            timestamp
        );
    }

    @Transactional
    public EveningRhythmResponse leaveEveningNote(EveningRhythmRequest request) {
        validateEvening(request);

        User user = getUser(request.userId());
        LocalDateTime timestamp = request.timestamp() == null ? LocalDateTime.now() : request.timestamp();
        AiEveningResult aiResult = aiEveningRhythmClient.leaveNote(user.getId(), request.text());

        RhythmLog rhythmLog = rhythmLogRepository.save(RhythmLog.createEvening(
            user,
            timestamp,
            request.text(),
            aiResult.signals(),
            aiResult.hanaMoney()
        ));

        savePointIfNeeded(user, aiResult.hanaMoney(), "evening rhythm note", rhythmLog.getId());

        List<EveningRhythmResponse.CollectedDudy> savedDudy = saveEveningCollectedDudy(
            user,
            rhythmLog.getId(),
            aiResult.collectedDudy()
        );

        return new EveningRhythmResponse(
            rhythmLog.getId(),
            new EveningRhythmResponse.Reward(aiResult.hanaMoney()),
            aiResult.reply(),
            getMonthlyRecordCount(user.getId(), timestamp),
            savedDudy,
            timestamp
        );
    }

    private void savePointIfNeeded(User user, Integer hanaMoney, String reason, Long rhythmLogId) {
        if (hanaMoney != null && hanaMoney > 0) {
            pointTransactionRepository.save(PointTransaction.earn(
                user,
                hanaMoney,
                reason,
                "RHYTHM_LOG",
                rhythmLogId
            ));
        }
    }

    private Integer getMonthlyRecordCount(String userId, LocalDateTime timestamp) {
        YearMonth yearMonth = YearMonth.from(timestamp);
        LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        long rhythmCount = rhythmLogRepository.countByUser_IdAndTimestampBetween(userId, startAt, endAt);
        long missionCount = missionLogRepository.countByUser_IdAndCompletedAtBetween(userId, startAt, endAt);
        return Math.toIntExact(rhythmCount + missionCount);
    }

    private List<MorningRhythmResponse.CollectedDudy> saveMorningCollectedDudy(
        User user,
        Long rhythmLogId,
        List<MorningRhythmResponse.CollectedDudy> collectedDudy
    ) {
        if (collectedDudy == null || collectedDudy.isEmpty()) {
            return List.of();
        }

        List<MorningRhythmResponse.CollectedDudy> saved = new ArrayList<>();
        for (MorningRhythmResponse.CollectedDudy dudy : collectedDudy) {
            if (saveDudy(user, rhythmLogId, dudy.id(), dudy.tier(), dudy.axis(), dudy.earnedReason(), "morning rhythm check-in")) {
                saved.add(dudy);
            }
        }
        return saved;
    }

    private List<EveningRhythmResponse.CollectedDudy> saveEveningCollectedDudy(
        User user,
        Long rhythmLogId,
        List<EveningRhythmResponse.CollectedDudy> collectedDudy
    ) {
        if (collectedDudy == null || collectedDudy.isEmpty()) {
            return List.of();
        }

        List<EveningRhythmResponse.CollectedDudy> saved = new ArrayList<>();
        for (EveningRhythmResponse.CollectedDudy dudy : collectedDudy) {
            if (saveDudy(user, rhythmLogId, dudy.id(), dudy.tier(), dudy.axis(), dudy.earnedReason(), "evening rhythm note")) {
                saved.add(dudy);
            }
        }
        return saved;
    }

    private boolean saveDudy(
        User user,
        Long rhythmLogId,
        String dudyId,
        String tier,
        String axis,
        String earnedReason,
        String defaultReason
    ) {
        if (dudyId == null || dudyId.isBlank()) {
            return false;
        }
        if (doodyCollectionRepository.existsByUser_IdAndDoodyTemplate_Id(user.getId(), dudyId)) {
            return false;
        }

        DoodyTemplate template = doodyTemplateRepository.findById(dudyId).orElse(null);
        if (template == null) {
            return false;
        }

        doodyCollectionRepository.save(DoodyCollection.create(
            user,
            template,
            tier,
            axis,
            earnedReason == null ? defaultReason : earnedReason,
            "RHYTHM_LOG",
            rhythmLogId
        ));
        return true;
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
    }

    private void validateMorning(MorningRhythmRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
        if (request.energy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "energy is required.");
        }
        if (request.energy() < 1 || request.energy() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "energy must be between 1 and 5.");
        }
    }

    private void validateEvening(EveningRhythmRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
        if (request.text() == null || request.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required.");
        }
    }
}