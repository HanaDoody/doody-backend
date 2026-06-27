package doody.spring.domain.repository;

import doody.spring.domain.entity.DoodyCollection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoodyCollectionRepository extends JpaRepository<DoodyCollection, Long> {
}