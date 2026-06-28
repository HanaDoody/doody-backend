package doody.spring.common.service;

import doody.spring.domain.entity.ContactUnlock;
import doody.spring.domain.entity.DoodyCollection;
import doody.spring.domain.entity.DoodyTemplate;
import doody.spring.domain.entity.PointTransaction;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.ContactUnlockRepository;
import doody.spring.domain.repository.DoodyCollectionRepository;
import doody.spring.domain.repository.DoodyTemplateRepository;
import doody.spring.domain.repository.PointTransactionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RewardPersistenceService {

    private final PointTransactionRepository pointTransactionRepository;
    private final DoodyTemplateRepository doodyTemplateRepository;
    private final DoodyCollectionRepository doodyCollectionRepository;
    private final ContactUnlockRepository contactUnlockRepository;

    public RewardPersistenceService(
        PointTransactionRepository pointTransactionRepository,
        DoodyTemplateRepository doodyTemplateRepository,
        DoodyCollectionRepository doodyCollectionRepository,
        ContactUnlockRepository contactUnlockRepository
    ) {
        this.pointTransactionRepository = pointTransactionRepository;
        this.doodyTemplateRepository = doodyTemplateRepository;
        this.doodyCollectionRepository = doodyCollectionRepository;
        this.contactUnlockRepository = contactUnlockRepository;
    }

    public void earnPoint(User user, Integer amount, String reason, String sourceType, Long sourceId) {
        if (amount == null || amount <= 0) {
            return;
        }
        pointTransactionRepository.save(PointTransaction.earn(user, amount, reason, sourceType, sourceId));
    }

    public Optional<DoodyCollection> collectDoody(
        User user,
        String doodyId,
        String tier,
        String axis,
        String earnedReason,
        String source,
        Long sourceId
    ) {
        if (doodyId == null || doodyId.isBlank()) {
            return Optional.empty();
        }
        return doodyTemplateRepository.findById(doodyId)
            .flatMap(template -> collectDoody(user, template, tier, axis, earnedReason, source, sourceId));
    }

    public Optional<DoodyCollection> collectDoody(
        User user,
        DoodyTemplate template,
        String tier,
        String axis,
        String earnedReason,
        String source,
        Long sourceId
    ) {
        if (template == null) {
            return Optional.empty();
        }
        if (doodyCollectionRepository.existsByUser_IdAndDoodyTemplate_Id(user.getId(), template.getId())) {
            return Optional.empty();
        }
        return Optional.of(doodyCollectionRepository.save(DoodyCollection.create(
            user,
            template,
            tier == null ? template.getTier() : tier,
            axis == null ? template.getAxis() : axis,
            earnedReason == null ? source : earnedReason,
            source,
            sourceId
        )));
    }

    public Optional<ContactUnlock> unlockContact(
        User user,
        String contactId,
        String axis,
        String source,
        Long sourceId
    ) {
        if (contactId == null || contactId.isBlank()) {
            return Optional.empty();
        }
        return contactUnlockRepository.findByUser_IdAndContactId(user.getId(), contactId)
            .or(() -> Optional.of(contactUnlockRepository.save(ContactUnlock.create(
                user,
                contactId,
                axis,
                source,
                sourceId
            ))));
    }
}