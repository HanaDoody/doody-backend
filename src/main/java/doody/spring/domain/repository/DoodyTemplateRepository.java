package doody.spring.domain.repository;

import doody.spring.domain.entity.DoodyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoodyTemplateRepository extends JpaRepository<DoodyTemplate, String> {
}