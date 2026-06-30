package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "report_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_summary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String period;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String stats;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String highlights;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public static ReportSummary create(
        User user,
        String period,
        LocalDate periodStart,
        LocalDate periodEnd,
        String stats,
        String summary,
        String highlights,
        LocalDateTime generatedAt
    ) {
        ReportSummary reportSummary = new ReportSummary();
        reportSummary.user = user;
        reportSummary.period = period;
        reportSummary.periodStart = periodStart;
        reportSummary.periodEnd = periodEnd;
        reportSummary.stats = stats;
        reportSummary.summary = summary;
        reportSummary.highlights = highlights;
        reportSummary.generatedAt = generatedAt;
        return reportSummary;
    }

    @PrePersist
    void prePersist() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }
}
