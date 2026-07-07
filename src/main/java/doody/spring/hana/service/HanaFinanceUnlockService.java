package doody.spring.hana.service;

import doody.spring.common.service.RewardPersistenceService;
import doody.spring.domain.entity.ContactUnlock;
import doody.spring.domain.entity.DoodyCollection;
import doody.spring.domain.entity.DoodyTemplate;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.ContactUnlockRepository;
import doody.spring.domain.repository.DoodyCollectionRepository;
import doody.spring.domain.repository.DoodyTemplateRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.hana.dto.HanaFinanceUnlockResponse;
import doody.spring.hana.dto.HanaFinanceUnlockStatusResponse;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HanaFinanceUnlockService {

    private static final String SOURCE = "HANA_FINANCE_UNLOCK";

    private final UserRepository userRepository;
    private final ContactUnlockRepository contactUnlockRepository;
    private final DoodyTemplateRepository doodyTemplateRepository;
    private final DoodyCollectionRepository doodyCollectionRepository;
    private final RewardPersistenceService rewardPersistenceService;
    private final String autonomyUrl;
    private final String connectionUrl;

    public HanaFinanceUnlockService(
        UserRepository userRepository,
        ContactUnlockRepository contactUnlockRepository,
        DoodyTemplateRepository doodyTemplateRepository,
        DoodyCollectionRepository doodyCollectionRepository,
        RewardPersistenceService rewardPersistenceService,
        @Value("${hana.finance.autonomy-url:https://www.hanafn.com}") String autonomyUrl,
        @Value("${hana.finance.connection-url:https://www.hanafn.com}") String connectionUrl
    ) {
        this.userRepository = userRepository;
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
        ContactUnlock aiUnlockSignal = findAiUnlockSignal(userId, meta);
        ContactUnlock benefitUnlock = contactUnlockRepository.findByUser_IdAndContactId(userId, meta.contactId())
            .orElse(null);
        ContactUnlock displayUnlock = benefitUnlock == null ? aiUnlockSignal : benefitUnlock;
        DoodyCollection rareDoody = findCollectedRareDoody(userId, meta);

        return new HanaFinanceUnlockStatusResponse(
            meta.axis(),
            null,
            null,
            aiUnlockSignal != null,
            benefitUnlock != null || rareDoody != null,
            new HanaFinanceUnlockStatusResponse.Contact(
                meta.contactId(),
                meta.title(),
                meta.url(),
                displayUnlock == null ? null : displayUnlock.getUnlockedAt()
            ),
            toStatusRareDoody(rareDoody, meta)
        );
    }

    @Transactional
    public HanaFinanceUnlockResponse unlock(String userId, String axis) {
        User user = requireUser(userId);
        AxisMeta meta = axisMeta(axis);
        ContactUnlock aiUnlockSignal = findAiUnlockSignal(userId, meta);
        if (aiUnlockSignal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ai unlock signal is not found.");
        }

        boolean contactAlreadyUnlocked = contactUnlockRepository.existsByUser_IdAndContactId(userId, meta.contactId());
        ContactUnlock contactUnlock = rewardPersistenceService.unlockContact(
            user,
            meta.contactId(),
            meta.axis().toLowerCase(Locale.ROOT),
            SOURCE,
            aiUnlockSignal.getId()
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
                aiUnlockSignal.getId()
            ).orElseGet(() -> findCollectedRareDoody(userId, meta));
            newlyUnlocked = true;
        }

        return new HanaFinanceUnlockResponse(
            meta.axis(),
            null,
            null,
            newlyUnlocked,
            new HanaFinanceUnlockResponse.Contact(
                contactUnlock.getId(),
                meta.contactId(),
                meta.title(),
                meta.url(),
                contactUnlock.getUnlockedAt()
            ),
            toUnlockRareDoody(rareDoody, meta),
            newlyUnlocked ? meta.unlockMessage() : "이미 하나 금융 혜택이 열려 있어."
        );
    }

    private ContactUnlock findAiUnlockSignal(String userId, AxisMeta meta) {
        return contactUnlockRepository.findTopByUser_IdAndAxisIgnoreCaseOrderByUnlockedAtDesc(userId, meta.axis())
            .orElse(null);
    }

    private DoodyCollection findCollectedRareDoody(String userId, AxisMeta meta) {
        return doodyCollectionRepository.findByUser_IdOrderByCollectedAtDesc(userId).stream()
            .filter(collection -> meta.rareDoodyId().equals(collection.getDoodyTemplate().getId()))
            .findFirst()
            .orElse(null);
    }

    private DoodyTemplate resolveRareDoodyTemplate(AxisMeta meta) {
        return doodyTemplateRepository.findById(meta.rareDoodyId())
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
                "자립ON 하나 금융 혜택",
                autonomyUrl,
                "d_autonomy_rare_hana",
                "자립ON 하나 금융 혜택으로 만난 레어 두디야.",
                "자립ON 하나 금융 혜택이 열렸어."
            );
        }
        if ("CONNECTION".equals(normalized)) {
            return new AxisMeta(
                "CONNECTION",
                "hana_connection_benefit",
                "연결ON 하나 금융 혜택",
                connectionUrl,
                "d_connection_rare_hana",
                "연결ON 하나 금융 혜택으로 만난 레어 두디야.",
                "연결ON 하나 금융 혜택이 열렸어."
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
