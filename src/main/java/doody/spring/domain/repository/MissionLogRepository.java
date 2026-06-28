package doody.spring.domain.repository;

import doody.spring.domain.entity.MissionLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionLogRepository extends JpaRepository<MissionLog, Long> {

    @EntityGraph(attributePaths = "missionTemplate")
    List<MissionLog> findByUser_IdAndCompletedAtBetweenOrderByCompletedAtDesc(
        String userId,
        LocalDateTime startAt,
        LocalDateTime endAt
    );

    long countByUser_IdAndCompletedAtBetween(
        String userId,
        LocalDateTime startAt,
        LocalDateTime endAt
    );

    @EntityGraph(attributePaths = "missionTemplate")
    List<MissionLog> findTop20ByUser_IdOrderByCreatedAtDesc(String userId);

    @EntityGraph(attributePaths = "missionTemplate")
    Optional<MissionLog> findTopByUser_IdAndMissionTemplate_IdOrderByCreatedAtDesc(String userId, String missionId);

    @EntityGraph(attributePaths = "missionTemplate")
    @Query("""
        select ml
        from MissionLog ml
        where ml.user.id = :userId
          and lower(ml.missionTemplate.axis) = lower(:axis)
        order by ml.createdAt desc
        """)
    List<MissionLog> findByUserIdAndAxis(@Param("userId") String userId, @Param("axis") String axis);
}
