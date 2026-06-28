package doody.spring.domain.repository;

import doody.spring.domain.entity.EnergyLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnergyLogRepository extends JpaRepository<EnergyLog, Long> {

    Optional<EnergyLog> findTopByUser_IdOrderByCreatedAtDesc(String userId);
}