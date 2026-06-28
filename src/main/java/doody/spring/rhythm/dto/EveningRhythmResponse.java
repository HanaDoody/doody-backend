package doody.spring.rhythm.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EveningRhythmResponse(
    Long rhythmLogId,
    Reward reward,
    String reply,
    Integer monthlyRecordCount,
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