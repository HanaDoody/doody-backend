package doody.spring.domain.repository;

import doody.spring.domain.entity.MissionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionLogRepository extends JpaRepository<MissionLog, Long> {
}