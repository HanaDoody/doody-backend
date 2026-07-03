package doody.spring.domain.repository;

import doody.spring.domain.entity.DoodyTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoodyTemplateRepository extends JpaRepository<DoodyTemplate, String> {

    Optional<DoodyTemplate> findFirstByTierIgnoreCaseAndAxisIgnoreCaseAndActiveTrue(String tier, String axis);

    List<DoodyTemplate> findByActiveTrue();
}
