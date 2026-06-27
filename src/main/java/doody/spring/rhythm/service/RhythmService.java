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
import doody.spring.domain.repository.PointTransactionRepository;
import doody.spring.domain.repository.RhythmLogRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.rhythm.client.AiMorningRhythmClient;
import doody.spring.rhythm.client.AiMorningRhythmClient.AiMorningResult;
import doody.spring.rhythm.dto.MorningRhythmRequest;
import doody.spring.rhythm.dto.MorningRhythmResponse;
import doody.spring.rhythm.dto.MorningRhythmResponse.CollectedDudy;
import doody.spring.rhythm.dto.MorningRhythmResponse.Reward;
import java.time.LocalDateTime;
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
    private final AiMorningRhythmClient aiMorningRhythmClient;

    public RhythmService(
        UserRepository userRepository,
        RhythmLogRepository rhythmLogRepository,
        EnergyLogRepository energyLogRepository,
        PointTransactionRepository pointTransactionRepository,
        DoodyTemplateRepository doodyTemplateRepository,
        DoodyCollectionRepository doodyCollectionRepository,
        AiMorningRhythmClient aiMorningRhythmClient
    ) {
        this.userRepository = userRepository;
        this.rhythmLogRepository = rhythmLogRepository;
        this.energyLogRepository = energyLogRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.doodyTemplateRepository = doodyTemplateRepository;
        this.doodyCollectionRepository = doodyCollectionRepository;
        this.aiMorningRhythmClient = aiMorningRhythmClient;
    }

    @Transactional
    public MorningRhythmResponse checkInMorning(MorningRhythmRequest request) {
        validate(request);

        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
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

        if (aiResult.hanaMoney() != null && aiResult.hanaMoney() > 0) {
            pointTransactionRepository.save(PointTransaction.earn(
                user,
                aiResult.hanaMoney(),
                "morning rhythm check-in",
                "RHYTHM_LOG",
                rhythmLog.getId()
            ));
        }

        List<CollectedDudy> savedDudy = saveCollectedDudy(user, rhythmLog.getId(), aiResult.collectedDudy());

        return new MorningRhythmResponse(
            rhythmLog.getId(),
            energyLog.getId(),
            new Reward(aiResult.hanaMoney()),
            aiResult.greeting(),
            savedDudy,
            timestamp
        );
    }

    private List<CollectedDudy> saveCollectedDudy(User user, Long rhythmLogId, List<CollectedDudy> collectedDudy) {
        if (collectedDudy == null || collectedDudy.isEmpty()) {
            return List.of();
        }

        List<CollectedDudy> saved = new ArrayList<>();
        for (CollectedDudy dudy : collectedDudy) {
            if (dudy.id() == null || dudy.id().isBlank()) {
                continue;
            }
            if (doodyCollectionRepository.existsByUser_IdAndDoodyTemplate_Id(user.getId(), dudy.id())) {
                continue;
            }

            DoodyTemplate template = doodyTemplateRepository.findById(dudy.id()).orElse(null);
            if (template == null) {
                continue;
            }

            doodyCollectionRepository.save(DoodyCollection.create(
                user,
                template,
                dudy.tier(),
                dudy.axis(),
                dudy.earnedReason() == null ? "morning rhythm check-in" : dudy.earnedReason(),
                "RHYTHM_LOG",
                rhythmLogId
            ));
            saved.add(dudy);
        }
        return saved;
    }

    private void validate(MorningRhythmRequest request) {
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
}