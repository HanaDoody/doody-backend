package doody.spring.domain.repository;

import doody.spring.domain.entity.OnboardingResponse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingResponseRepository extends JpaRepository<OnboardingResponse, Long> {

    Optional<OnboardingResponse> findTopByUser_IdOrderByCreatedAtDesc(String userId);
}