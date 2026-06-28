package doody.spring.hana.dto;

import java.time.LocalDateTime;

public record HanaFinanceUnlockResponse(
    String axis,
    Integer requiredStage,
    Integer reachedStage,
    Boolean newlyUnlocked,
    Contact contact,
    RareDoody rareDoody,
    String message
) {

    public record Contact(
        Long contactUnlockId,
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
        String earnedReason,
        LocalDateTime collectedAt
    ) {
    }
}