package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "goals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal {

    @Id
    @Column(name = "goal_id", length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "g_rhythm", nullable = false, precision = 10, scale = 2)
    private BigDecimal rhythm;

    @Column(name = "g_autonomy", nullable = false, precision = 10, scale = 2)
    private BigDecimal autonomy;

    @Column(name = "g_connection", nullable = false, precision = 10, scale = 2)
    private BigDecimal connection;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
