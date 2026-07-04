package doody.spring.domain.repository;

import doody.spring.domain.entity.AriSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AriSnapshotRepository extends JpaRepository<AriSnapshot, Long> {

    Optional<AriSnapshot> findTopByUser_IdOrderByTimestampDesc(String userId);
}
