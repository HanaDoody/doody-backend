package doody.spring.domain.repository;

import doody.spring.domain.entity.AriSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AriSnapshotRepository extends JpaRepository<AriSnapshot, Long> {
}