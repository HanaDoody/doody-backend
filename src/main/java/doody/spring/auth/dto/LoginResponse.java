package doody.spring.auth.dto;

import doody.spring.domain.entity.User;

public record LoginResponse(
    String userId,
    String email,
    String nickname,
    Boolean isMydataLinked
) {

    public static LoginResponse from(User user) {
        return new LoginResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getMydataLinked()
        );
    }
}