package doody.spring.health.service;

import doody.spring.health.dto.HealthResponse;
import doody.spring.health.dto.HealthResponse.DatabaseHealth;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthResponse check() {
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            if (Integer.valueOf(1).equals(result)) {
                return new HealthResponse(
                    "UP",
                    "UP",
                    new DatabaseHealth("UP", true, "database connection is healthy."),
                    LocalDateTime.now()
                );
            }

            return new HealthResponse(
                "DOWN",
                "UP",
                new DatabaseHealth("DOWN", false, "database health query returned unexpected result."),
                LocalDateTime.now()
            );
        } catch (Exception exception) {
            return new HealthResponse(
                "DOWN",
                "UP",
                new DatabaseHealth("DOWN", false, rootMessage(exception)),
                LocalDateTime.now()
            );
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}