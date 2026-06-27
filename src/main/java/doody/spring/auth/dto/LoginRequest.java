package doody.spring.auth.dto;

public record LoginRequest(
    String email,
    String nickname
) {
}