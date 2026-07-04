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
@Table(name = "ari_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AriSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ari_snapshot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal rhythm;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal autonomy;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal connection;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public static AriSnapshot create(
        User user,
        BigDecimal rhythm,
        BigDecimal autonomy,
        BigDecimal connection,
        String sourceType,
        Long sourceId
    ) {
        AriSnapshot snapshot = new AriSnapshot();
        snapshot.user = user;
        snapshot.rhythm = rhythm;
        snapshot.autonomy = autonomy;
        snapshot.connection = connection;
        snapshot.sourceType = sourceType;
        snapshot.sourceId = sourceId;
        return snapshot;
    }

    @PrePersist
    void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
