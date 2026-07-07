package doody.spring.domain.repository;

import doody.spring.domain.entity.CollectionPin;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionPinRepository extends JpaRepository<CollectionPin, String> {

    @EntityGraph(attributePaths = "doodyTemplate")
    List<CollectionPin> findByActiveTrue();

    @EntityGraph(attributePaths = "doodyTemplate")
    List<CollectionPin> findByActiveTrueAndDoodyTemplate_Id(String doodyTemplateId);

    @EntityGraph(attributePaths = "doodyTemplate")
    Optional<CollectionPin> findByIdAndActiveTrue(String id);
}
