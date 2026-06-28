package doody.spring.domain.repository;

import doody.spring.domain.entity.ContactUnlock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactUnlockRepository extends JpaRepository<ContactUnlock, Long> {

    boolean existsByUser_IdAndContactId(String userId, String contactId);

    Optional<ContactUnlock> findByUser_IdAndContactId(String userId, String contactId);
}