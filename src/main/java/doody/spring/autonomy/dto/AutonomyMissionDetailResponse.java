package doody.spring.autonomy.dto;

import java.util.List;

public record AutonomyMissionDetailResponse(
    String missionId,
    String axis,
    Integer stage,
    String title,
    String description,
    String reason,
    List<String> howTo,
    List<String> goalTags,
    String missionType,
    Integer requiredEvidenceCount,
    Integer reward,
    String externalUrl,
    String fallbackMissionId,
    Boolean isFallback
) {
}
