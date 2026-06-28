package doody.spring.mission.dto;

import java.util.List;
import java.util.Map;

public record TodayMissionResponse(
    Mission mission,
    Mission fallback,
    String missionState,
    String recommendedAxis,
    String nextPath,
    String restMessage,
    List<Contact> unlockedContacts,
    Double vRhythm,
    Map<String, Object> diagnostics,
    String source
) {

    public record Mission(
        String id,
        String missionId,
        String axis,
        Integer stage,
        String waypoint,
        Integer difficulty,
        AriVector delta,
        String title,
        String description,
        String missionType,
        Integer requiredCount,
        Boolean isSignature,
        Boolean isFallback,
        String fallbackMissionId,
        List<String> goalTags,
        List<String> howTo,
        String reason
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
