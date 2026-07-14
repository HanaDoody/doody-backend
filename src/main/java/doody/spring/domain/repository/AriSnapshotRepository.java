package doody.spring.domain.repository;

import doody.spring.domain.entity.AriSnapshot;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AriSnapshotRepository extends JpaRepository<AriSnapshot, Long> {

    Optional<AriSnapshot> findTopByUser_IdOrderByTimestampDesc(String userId);

    Optional<AriSnapshot> findTopByUser_IdAndTimestampLessThanEqualOrderByTimestampDesc(
        String userId,
        LocalDateTime timestamp
    );

    Optional<AriSnapshot> findTopByUser_IdOrderByTimestampAsc(String userId);
}
