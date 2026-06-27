package doody.spring.auth.service;

import doody.spring.auth.dto.SignupRequest;
import doody.spring.auth.dto.SignupResponse;
import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.OnboardingResponseRepository;
import doody.spring.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OnboardingResponseRepository onboardingResponseRepository;

    public AuthService(
        UserRepository userRepository,
        OnboardingResponseRepository onboardingResponseRepository
    ) {
        this.userRepository = userRepository;
        this.onboardingResponseRepository = onboardingResponseRepository;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateSignupRequest(request);

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "?대? 媛?낅맂 ?대찓?쇱엯?덈떎.");
        }

        User user = userRepository.save(User.create(
            request.email(),
            request.nickname(),
            request.isMydataLinked()
        ));

        OnboardingResponse onboardingResponse = onboardingResponseRepository.save(
            OnboardingResponse.create(
                user,
                request.gapAxis(),
                request.goalChoice(),
                request.recommendedPeriod(),
                request.rhythmChoice(),
                request.autonomyChoice(),
                request.connectionChoice()
            )
        );

        return SignupResponse.from(user, onboardingResponse);
    }

    private void validateSignupRequest(SignupRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email? ?꾩닔?낅땲??");
        }
        if (request.nickname() == null || request.nickname().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname? ?꾩닔?낅땲??");
        }
        if (request.gapAxis() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gapAxis???꾩닔?낅땲??");
        }
        if (request.goalChoice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "goalChoice???꾩닔?낅땲??");
        }
        if (request.recommendedPeriod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recommendedPeriod???꾩닔?낅땲??");
        }
    }
}