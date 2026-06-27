package doody.spring.domain.repository;

import doody.spring.domain.entity.EnergyLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnergyLogRepository extends JpaRepository<EnergyLog, Long> {
}