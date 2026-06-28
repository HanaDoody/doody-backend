package doody.spring.autonomy.dto;

import java.util.List;

public record AutonomyPathResponse(
    String title,
    Integer currentStep,
    Boolean todayAvailable,
    List<Node> nodes
) {

    public record Node(
        Integer step,
        String status,
        String missionId,
        String title
    ) {
    }
}
