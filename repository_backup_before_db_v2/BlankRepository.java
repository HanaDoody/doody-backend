package doody.spring.domain.repository;

import doody.spring.domain.entity.Blank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlankRepository extends JpaRepository<Blank, String> {
}
