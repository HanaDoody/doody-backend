package doody.spring.domain.repository;

import doody.spring.domain.entity.ReportSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportSummaryRepository extends JpaRepository<ReportSummary, Long> {

    List<ReportSummary> findByUser_IdOrderByGeneratedAtDesc(String userId);

    Optional<ReportSummary> findTopByUser_IdAndPeriodAndPeriodStartAndPeriodEndOrderByGeneratedAtDesc(
        String userId,
        String period,
        LocalDate periodStart,
        LocalDate periodEnd
    );
}