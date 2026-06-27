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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_ai_state_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAiStateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rhythm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal autonomy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal connection;

    @Column(name = "trigger_type", nullable = false, length = 100)
    private String triggerType;

    @Column(nullable = false)
    private Integer energy;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
