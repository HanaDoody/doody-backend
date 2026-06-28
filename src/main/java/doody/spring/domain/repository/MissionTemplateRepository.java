package doody.spring.domain.repository;

import doody.spring.domain.entity.MissionTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionTemplateRepository extends JpaRepository<MissionTemplate, String> {

    Optional<MissionTemplate> findTopByActiveTrueOrderByIdAsc();
}