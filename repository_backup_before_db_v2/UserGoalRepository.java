package doody.spring.domain.repository;

import doody.spring.domain.entity.UserGoal;
import doody.spring.domain.entity.UserGoalId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalRepository extends JpaRepository<UserGoal, UserGoalId> {
}
