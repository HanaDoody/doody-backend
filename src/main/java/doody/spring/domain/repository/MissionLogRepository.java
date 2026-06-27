package doody.spring.domain.repository;

import doody.spring.domain.entity.MissionLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionLogRepository extends JpaRepository<MissionLog, Long> {

    @EntityGraph(attributePaths = "missionTemplate")
    List<MissionLog> findByUser_IdAndCompletedAtBetweenOrderByCompletedAtDesc(
        String userId,
        LocalDateTime startAt,
        LocalDateTime endAt
    );
}