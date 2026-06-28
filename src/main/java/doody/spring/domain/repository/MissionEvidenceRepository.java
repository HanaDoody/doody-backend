package doody.spring.domain.repository;

import doody.spring.domain.entity.MissionEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionEvidenceRepository extends JpaRepository<MissionEvidence, Long> {

    List<MissionEvidence> findByUser_IdAndMissionTemplate_IdOrderByCreatedAtDesc(String userId, String missionId);
}
