package doody.spring.domain.repository;

import doody.spring.domain.entity.RhythmLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RhythmLogRepository extends JpaRepository<RhythmLog, Long> {

    List<RhythmLog> findByUser_IdAndTimestampBetweenOrderByTimestampDesc(
        String userId,
        LocalDateTime startAt,
        LocalDateTime endAt
    );

    long countByUser_IdAndTimestampBetween(
        String userId,
        LocalDateTime startAt,
        LocalDateTime endAt
    );

    boolean existsByUser_IdAndRhythmTypeAndAnchorDate(
        String userId,
        String rhythmType,
        LocalDate anchorDate
    );

    List<RhythmLog> findTop20ByUser_IdOrderByTimestampDesc(String userId);
}
