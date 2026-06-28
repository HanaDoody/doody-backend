package doody.spring.hana.service;

import doody.spring.common.service.RewardPersistenceService;
import doody.spring.domain.entity.ContactUnlock;
import doody.spring.domain.entity.DoodyCollection;
import doody.spring.domain.entity.DoodyTemplate;
import doody.spring.domain.entity.MissionLog;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.ContactUnlockRepository;
import doody.spring.domain.repository.DoodyCollectionRepository;
import doody.spring.domain.repository.DoodyTemplateRepository;
import doody.spring.domain.repository.MissionLogRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.hana.dto.HanaFinanceUnlockResponse;
import doody.spring.hana.dto.HanaFinanceUnlockStatusResponse;
import java.util.Comparator;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HanaFinanceUnlockService {

    private static final int REQUIRED_STAGE = 10;
    private static final String SOURCE = "HANA_FINANCE_UNLOCK";

    private final UserRepository userRepository;
    private final MissionLogRepository missionLogRepository;
    private final ContactUnlockRepository contactUnlockRepository;
    private final DoodyTemplateRepository doodyTemplateRepository;
    private final DoodyCollectionRepository doodyCollectionRepository;
    private final RewardPersistenceService rewardPersistenceService;
    private final String autonomyUrl;
    private final String connectionUrl;

    public HanaFinanceUnlockService(
        UserRepository userRepository,
        MissionLogRepository missionLogRepository,
        ContactUnlockRepository contactUnlockRepository,
        DoodyTemplateRepository doodyTemplateRepository,
        DoodyCollectionRepository doodyCollectionRepository,
        RewardPersistenceService rewardPersistenceService,
        @Value("${hana.finance.autonomy-url:https://www.hanafn.com}") String autonomyUrl,
        @Value("${hana.finance.connection-url:https://www.hanafn.com}") String connectionUrl
    ) {
        this.userRepository = userRepository;
        this.missionLogRepository = missionLogRepository;
        this.contactUnlockRepository = contactUnlockRepository;
        this.doodyTemplateRepository = doodyTemplateRepository;
        this.doodyCollectionRepository = doodyCollectionRepository;
        this.rewardPersistenceService = rewardPersistenceService;
        this.autonomyUrl = autonomyUrl;
        this.connectionUrl = connectionUrl;
    }

    @Transactional(readOnly = true)
    public HanaFinanceUnlockStatusResponse getStatus(String userId, String axis) {
        requireUser(userId);
        AxisMeta meta = axisMeta(axis);
        int reachedStage = reachedStage(userId, meta.axis());
        ContactUnlock contactUnlock = contactUnlockRepository.findByUser_IdAndContactId(userId, meta.contactId()).orElse(null);
        DoodyCollection rareDoody = findCollectedRareDoody(userId, meta);

        return new HanaFinanceUnlockStatusResponse(
            meta.axis(),
            REQUIRED_STAGE,
            reachedStage,
            reachedStage >= REQUIRED_STAGE,
            contactUnlock != null,
            new HanaFinanceUnlockStatusResponse.Contact(
                meta.contactId(),
                meta.title(),
                meta.url(),
                contactUnlock == null ? null : contactUnlock.getUnlockedAt()
            ),
            toStatusRareDoody(rareDoody, meta)
        );
    }

    @Transactional
    public HanaFinanceUnlockResponse unlock(String userId, String axis) {
        User user = requireUser(userId);
        AxisMeta meta = axisMeta(axis);
        int reachedStage = reachedStage(userId, meta.axis());
        if (reachedStage < REQUIRED_STAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hana finance unlock stage is not reached.");
        }

        boolean contactAlreadyUnlocked = contactUnlockRepository.existsByUser_IdAndContactId(userId, meta.contactId());
        ContactUnlock contactUnlock = rewardPersistenceService.unlockContact(
            user,
            meta.contactId(),
            meta.axis().toLowerCase(Locale.ROOT),
            SOURCE,
            null
        ).orElseThrow();

        DoodyCollection rareDoody = findCollectedRareDoody(userId, meta);
        boolean newlyUnlocked = !contactAlreadyUnlocked || rareDoody == null;
        if (rareDoody == null) {
            DoodyTemplate template = resolveRareDoodyTemplate(meta);
            rareDoody = rewardPersistenceService.collectDoody(
                user,
                template,
                "rare",
                meta.axis().toLowerCase(Locale.ROOT),
                meta.earnedReason(),
                SOURCE,
                contactUnlock.getId()
            ).orElseGet(() -> findCollectedRareDoody(userId, meta));
            newlyUnlocked = true;
        }

        return new HanaFinanceUnlockResponse(
            meta.axis(),
            REQUIRED_STAGE,
            reachedStage,
            newlyUnlocked,
            new HanaFinanceUnlockResponse.Contact(
                contactUnlock.getId(),
                meta.contactId(),
                meta.title(),
                meta.url(),
                contactUnlock.getUnlockedAt()
            ),
            toUnlockRareDoody(rareDoody, meta),
            newlyUnlocked ? meta.unlockMessage() : "Hana finance benefit is already unlocked."
        );
    }

    private int reachedStage(String userId, String axis) {
        return missionLogRepository.findByUserIdAndAxis(userId, axis).stream()
            .filter(log -> log.getCompletedAt() != null)
            .filter(log -> !Boolean.TRUE.equals(log.getMissionTemplate().getFallback()))
            .map(MissionLog::getMissionTemplate)
            .map(template -> template.getStage() == null ? 0 : template.getStage())
            .max(Comparator.naturalOrder())
            .orElse(0);
    }

    private DoodyCollection findCollectedRareDoody(String userId, AxisMeta meta) {
        return doodyCollectionRepository.findByUser_IdOrderByCollectedAtDesc(userId).stream()
            .filter(collection -> meta.rareDoodyId().equals(collection.getDoodyTemplate().getId())
                || ("rare".equalsIgnoreCase(collection.getTier()) && meta.axis().equalsIgnoreCase(collection.getAxis())))
            .findFirst()
            .orElse(null);
    }

    private DoodyTemplate resolveRareDoodyTemplate(AxisMeta meta) {
        return doodyTemplateRepository.findById(meta.rareDoodyId())
            .or(() -> doodyTemplateRepository.findFirstByTierIgnoreCaseAndAxisIgnoreCaseAndActiveTrue("rare", meta.axis()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "rare doody template not found."));
    }

    private HanaFinanceUnlockStatusResponse.RareDoody toStatusRareDoody(DoodyCollection collection, AxisMeta meta) {
        if (collection == null) {
            return new HanaFinanceUnlockStatusResponse.RareDoody(null, meta.rareDoodyId(), null, "rare", meta.axis(), null, null);
        }
        return new HanaFinanceUnlockStatusResponse.RareDoody(
            collection.getId(),
            collection.getDoodyTemplate().getId(),
            collection.getDoodyTemplate().getName(),
            collection.getTier(),
            collection.getAxis(),
            collection.getDoodyTemplate().getImageUrl(),
            collection.getCollectedAt()
        );
    }

    private HanaFinanceUnlockResponse.RareDoody toUnlockRareDoody(DoodyCollection collection, AxisMeta meta) {
        return new HanaFinanceUnlockResponse.RareDoody(
            collection.getId(),
            collection.getDoodyTemplate().getId(),
            collection.getDoodyTemplate().getName(),
            collection.getTier(),
            collection.getAxis(),
            collection.getDoodyTemplate().getImageUrl(),
            collection.getEarnedReason(),
            collection.getCollectedAt()
        );
    }

    private User requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
    }

    private AxisMeta axisMeta(String axis) {
        String normalized = axis == null ? "" : axis.strip().toUpperCase(Locale.ROOT);
        if ("AUTONOMY".equals(normalized)) {
            return new AxisMeta(
                "AUTONOMY",
                "hana_autonomy_benefit",
                "Autonomy benefit",
                autonomyUrl,
                "d_autonomy_rare_hana",
                "Rare dudy earned by autonomy stage unlock.",
                "Autonomy stage unlocked."
            );
        }
        if ("CONNECTION".equals(normalized)) {
            return new AxisMeta(
                "CONNECTION",
                "hana_connection_benefit",
                "Connection benefit",
                connectionUrl,
                "d_connection_rare_hana",
                "Rare dudy earned by connection stage unlock.",
                "Connection stage unlocked."
            );
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "axis must be AUTONOMY or CONNECTION.");
    }

    private record AxisMeta(
        String axis,
        String contactId,
        String title,
        String url,
        String rareDoodyId,
        String earnedReason,
        String unlockMessage
    ) {
    }
}