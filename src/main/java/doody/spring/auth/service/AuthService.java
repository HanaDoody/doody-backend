package doody.spring.auth.service;

import doody.spring.auth.dto.LoginRequest;
import doody.spring.auth.dto.LoginResponse;
import doody.spring.auth.dto.SignupRequest;
import doody.spring.auth.dto.SignupResponse;
import doody.spring.domain.entity.Goal;
import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.GoalRepository;
import doody.spring.domain.repository.OnboardingResponseRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OnboardingResponseRepository onboardingResponseRepository;
    private final GoalRepository goalRepository;

    public AuthService(
        UserRepository userRepository,
        OnboardingResponseRepository onboardingResponseRepository,
        GoalRepository goalRepository
    ) {
        this.userRepository = userRepository;
        this.onboardingResponseRepository = onboardingResponseRepository;
        this.goalRepository = goalRepository;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateSignupRequest(request);

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists.");
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

        Goal goal = goalRepository.save(Goal.create(
            user,
            request.gapAxis().getValue(),
            request.autonomyGoal(),
            request.connectionGoal(),
            request.recommendedPeriod(),
            request.firstStepMission()
        ));

        AriVector initialAri = new AriVector(
            scoreToAri(request.rhythmChoice()),
            scoreToAri(request.autonomyChoice()),
            scoreToAri(request.connectionChoice())
        );
        AriVector goalAri = new AriVector(0.8, 0.8, 0.4);
        goal.updateAri(
            BigDecimal.valueOf(initialAri.rhythm()),
            BigDecimal.valueOf(initialAri.autonomy()),
            BigDecimal.valueOf(initialAri.connection())
        );

        return SignupResponse.from(
            user,
            onboardingResponse,
            goal,
            initialAri,
            goalAri,
            "Start with rhythm, then open autonomy and connection steps."
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);

        User user = userRepository.findByEmailAndNickname(request.email(), request.nickname())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "email or nickname is invalid."));

        return LoginResponse.from(user);
    }

    private double scoreToAri(Integer score) {
        if (score == null) {
            return 0.2;
        }
        return Math.max(0.1, Math.min(score, 5) / 5.0);
    }

    private void validateSignupRequest(SignupRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required.");
        }
        if (request.nickname() == null || request.nickname().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname is required.");
        }
        if (request.gapAxis() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gapAxis is required.");
        }
        if (request.goalChoice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "goalChoice is required.");
        }
        if (request.recommendedPeriod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recommendedPeriod is required.");
        }
        if (request.autonomyGoal() == null || request.autonomyGoal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "autonomyGoal is required.");
        }
        if (request.connectionGoal() == null || request.connectionGoal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "connectionGoal is required.");
        }
        if (request.firstStepMission() == null || request.firstStepMission().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "firstStepMission is required.");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required.");
        }
        if (request.nickname() == null || request.nickname().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname is required.");
        }
    }
}