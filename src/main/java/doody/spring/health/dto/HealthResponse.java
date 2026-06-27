package doody.spring.health.dto;

import java.time.LocalDateTime;

public record HealthResponse(
    String status,
    String server,
    DatabaseHealth database,
    LocalDateTime checkedAt
) {

    public boolean healthy() {
        return "UP".equals(status);
    }

    public record DatabaseHealth(
        String status,
        boolean connected,
        String message
    ) {
    }
}