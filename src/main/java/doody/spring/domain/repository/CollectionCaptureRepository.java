package doody.spring.domain.repository;

import doody.spring.domain.entity.CollectionCapture;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionCaptureRepository extends JpaRepository<CollectionCapture, Long> {

    @EntityGraph(attributePaths = "doodyTemplate")
    List<CollectionCapture> findByUser_IdAndCapturedAtBetweenOrderByCapturedAtDesc(
        String userId,
        LocalDateTime startAt,
        LocalDateTime endAt
    );
}
