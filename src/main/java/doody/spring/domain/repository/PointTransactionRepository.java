package doody.spring.domain.repository;

import doody.spring.domain.entity.PointTransaction;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    @Query("""
        select coalesce(sum(point.amount), 0)
        from PointTransaction point
        where point.user.id = :userId
          and point.createdAt between :startAt and :endAt
        """)
    Integer sumAmountByUserIdAndCreatedAtBetween(
        @Param("userId") String userId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    @Query("""
        select coalesce(sum(point.amount), 0)
        from PointTransaction point
        where point.user.id = :userId
        """)
    Integer sumAmountByUserId(@Param("userId") String userId);
}