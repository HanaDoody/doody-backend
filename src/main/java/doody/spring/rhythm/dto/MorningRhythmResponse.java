package doody.spring.rhythm.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MorningRhythmResponse(
    Long rhythmLogId,
    Long energyLogId,
    Reward reward,
    String greeting,
    List<CollectedDudy> collectedDudy,
    LocalDateTime recordedAt
) {

    public record Reward(
        Integer hanaMoney
    ) {
    }

    public record CollectedDudy(
        String id,
        String tier,
        String axis,
        String earnedReason
    ) {
    }
}