package doody.spring.autonomy.dto;

import java.time.LocalDateTime;

public record AutonomyMissionStartResponse(
    Long missionLogId,
    String missionId,
    String actionType,
    LocalDateTime startedAt
) {
}
