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

@Getter
@Entity
@Table(name = "energy_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnergyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "energy_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "energy_date", nullable = false)
    private LocalDate energyDate;

    @Column(nullable = false)
    private Short energy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rhythm_log_id")
    private RhythmLog rhythmLog;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static EnergyLog create(User user, LocalDate energyDate, Short energy, RhythmLog rhythmLog) {
        EnergyLog energyLog = new EnergyLog();
        energyLog.user = user;
        energyLog.energyDate = energyDate;
        energyLog.energy = energy;
        energyLog.rhythmLog = rhythmLog;
        return energyLog;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}