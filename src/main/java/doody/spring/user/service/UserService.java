package doody.spring.user.service;

import doody.spring.domain.entity.OnboardingResponse;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.MissionLogRepository;
import doody.spring.domain.repository.OnboardingResponseRepository;
import doody.spring.domain.repository.PointTransactionRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OnboardingResponseRepository onboardingResponseRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final MissionLogRepository missionLogRepository;

    public UserService(
        UserRepository userRepository,
        OnboardingResponseRepository onboardingResponseRepository,
        PointTransactionRepository pointTransactionRepository,
        MissionLogRepository missionLogRepository
    ) {
        this.userRepository = userRepository;
        this.onboardingResponseRepository = onboardingResponseRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.missionLogRepository = missionLogRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));

        OnboardingResponse onboardingResponse = onboardingResponseRepository
            .findTopByUser_IdOrderByCreatedAtDesc(userId)
            .orElse(null);
        Integer hanaMoney = pointTransactionRepository.sumAmountByUserId(userId);
        long completedMissionCount = missionLogRepository.countCompletedRealMissionsByUserId(userId);

        return UserResponse.from(user, onboardingResponse, hanaMoney, completedMissionCount);
    }
}
