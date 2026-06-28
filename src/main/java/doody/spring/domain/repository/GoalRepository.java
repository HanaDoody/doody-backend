package doody.spring.domain.repository;

import doody.spring.domain.entity.Goal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    Optional<Goal> findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(String userId);
}