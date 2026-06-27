package doody.spring.domain.repository;

import doody.spring.domain.entity.UserAiStateLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAiStateLogRepository extends JpaRepository<UserAiStateLog, Long> {
}
