package doody.spring.domain.repository;

import doody.spring.domain.entity.RhythmLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RhythmLogRepository extends JpaRepository<RhythmLog, Long> {
}