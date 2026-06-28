package doody.spring.autonomy.dto;

import doody.spring.mission.client.AiMissionActionClient.Contact;
import doody.spring.mission.client.AiMissionActionClient.Dudy;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import java.util.List;

public record AutonomyMissionCompleteResponse(
    Long missionLogId,
    String missionId,
    AriVector updatedAri,
    AriVector appliedDelta,
    Double eta,
    Integer reward,
    Boolean completed,
    Boolean signatureAvailable,
    Boolean signatureCompleted,
    Boolean contactUnlocked,
    List<Dudy> collectedDudy,
    List<Contact> unlockedContacts,
    String message
) {
}
