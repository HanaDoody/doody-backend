package doody.spring.rhythm.dto;

import java.time.LocalDateTime;

public record MorningRhythmRequest(
    String userId,
    LocalDateTime timestamp,
    Short energy
) {
}