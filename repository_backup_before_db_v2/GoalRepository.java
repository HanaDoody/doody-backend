package doody.spring.domain.repository;

import doody.spring.domain.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, String> {
}
