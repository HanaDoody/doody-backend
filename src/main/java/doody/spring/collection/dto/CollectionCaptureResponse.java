package doody.spring.collection.dto;

import java.util.List;

public record CollectionCaptureResponse(
    Long captureId,
    Dudy dudy,
    Reward reward,
    AriVector updatedAri,
    AriVector appliedDelta,
    List<Dudy> collectedDudy,
    List<Contact> unlockedContacts
) {

    public record Dudy(
        String id,
        String tier,
        String axis,
        String earnedReason
    ) {
    }

    public record Reward(
        Integer hanaMoney
    ) {
    }

    public record AriVector(
        Double rhythm,
        Double autonomy,
        Double connection
    ) {
    }

    public record Contact(
        String id,
        String title,
        String axis
    ) {
    }
}