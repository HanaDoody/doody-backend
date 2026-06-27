package doody.spring.domain.repository;

import doody.spring.domain.entity.UserChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChatLogRepository extends JpaRepository<UserChatLog, Long> {
}
