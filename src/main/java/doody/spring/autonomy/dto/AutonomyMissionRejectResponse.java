package doody.spring.autonomy.dto;

public record AutonomyMissionRejectResponse(
    Long missionLogId,
    String missionId,
    String action,
    String message,
    Boolean restOption
) {
}
