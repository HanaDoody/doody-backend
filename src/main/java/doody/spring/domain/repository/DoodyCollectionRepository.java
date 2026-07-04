package doody.spring.domain.repository;

import doody.spring.domain.entity.DoodyCollection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoodyCollectionRepository extends JpaRepository<DoodyCollection, Long> {

    @EntityGraph(attributePaths = "doodyTemplate")
    List<DoodyCollection> findByUser_IdOrderByCollectedAtDesc(String userId);

    @EntityGraph(attributePaths = "doodyTemplate")
    Optional<DoodyCollection> findByIdAndUser_Id(Long id, String userId);

    boolean existsByUser_IdAndDoodyTemplate_Id(String userId, String doodyTemplateId);

    long countByUser_Id(String userId);
}
