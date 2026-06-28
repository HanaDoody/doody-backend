package doody.spring.autonomy.dto;

import java.time.LocalDateTime;

public record AutonomyMissionEvidenceResponse(
    Long evidenceId,
    Long missionLogId,
    String missionId,
    String fileUrl,
    String contentType,
    LocalDateTime createdAt
) {
}
