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
@Table(name = "rhythm_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RhythmLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rhythm_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rhythm_type", nullable = false, length = 10)
    private String rhythmType;

    @Column(name = "anchor_date", nullable = false)
    private LocalDate anchorDate;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String signals;

    @Column(nullable = false)
    private Integer reward = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static RhythmLog createMorning(User user, LocalDateTime timestamp, String greeting, Integer reward) {
        RhythmLog rhythmLog = new RhythmLog();
        rhythmLog.user = user;
        rhythmLog.rhythmType = "MORNING";
        rhythmLog.anchorDate = timestamp.toLocalDate();
        rhythmLog.timestamp = timestamp;
        rhythmLog.text = greeting;
        rhythmLog.signals = null;
        rhythmLog.reward = reward == null ? 0 : reward;
        return rhythmLog;
    }

    public static RhythmLog createEvening(User user, LocalDateTime timestamp, String text, String signals, Integer reward) {
        RhythmLog rhythmLog = new RhythmLog();
        rhythmLog.user = user;
        rhythmLog.rhythmType = "EVENING";
        rhythmLog.anchorDate = timestamp.toLocalDate();
        rhythmLog.timestamp = timestamp;
        rhythmLog.text = text;
        rhythmLog.signals = signals;
        rhythmLog.reward = reward == null ? 0 : reward;
        return rhythmLog;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.timestamp == null) {
            this.timestamp = now;
        }
        if (this.anchorDate == null) {
            this.anchorDate = this.timestamp.toLocalDate();
        }
        if (this.reward == null) {
            this.reward = 0;
        }
        this.createdAt = now;
    }
}
