package doody.spring.domain.repository;

import doody.spring.domain.entity.UserMissionTransition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMissionTransitionRepository extends JpaRepository<UserMissionTransition, Long> {
}
