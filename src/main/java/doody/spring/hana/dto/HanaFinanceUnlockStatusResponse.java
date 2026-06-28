package doody.spring.hana.dto;

import java.time.LocalDateTime;

public record HanaFinanceUnlockStatusResponse(
    String axis,
    Integer requiredStage,
    Integer reachedStage,
    Boolean unlockable,
    Boolean unlocked,
    Contact contact,
    RareDoody rareDoody
) {

    public record Contact(
        String contactId,
        String title,
        String url,
        LocalDateTime unlockedAt
    ) {
    }

    public record RareDoody(
        Long doodyCollectionId,
        String doodyId,
        String name,
        String tier,
        String axis,
        String imageUrl,
        LocalDateTime collectedAt
    ) {
    }
}