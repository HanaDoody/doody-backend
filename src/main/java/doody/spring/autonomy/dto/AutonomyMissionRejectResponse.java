package doody.spring.autonomy.dto;

import doody.spring.mission.dto.TodayMissionResponse.Mission;
import java.util.List;

public record AutonomyMissionRejectResponse(
    Long missionLogId,
    String missionId,
    String action,
    String message,
    Boolean restOption,
    List<Mission> candidates
) {
}
