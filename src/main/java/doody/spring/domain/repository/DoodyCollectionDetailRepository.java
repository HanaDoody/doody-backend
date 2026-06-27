package doody.spring.domain.repository;

import doody.spring.domain.entity.DoodyCollectionDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoodyCollectionDetailRepository extends JpaRepository<DoodyCollectionDetail, Long> {

    Optional<DoodyCollectionDetail> findByDoodyCollection_Id(Long doodyCollectionId);
}