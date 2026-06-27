package doody.spring.domain.repository;

import doody.spring.domain.entity.ReportSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportSummaryRepository extends JpaRepository<ReportSummary, Long> {
}