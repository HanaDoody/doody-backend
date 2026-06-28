package doody.spring.rhythm.dto;

import java.time.LocalDateTime;

public record EveningRhythmRequest(
    String userId,
    LocalDateTime timestamp,
    String text
) {
}