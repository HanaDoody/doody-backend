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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mission_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private MissionTemplate missionTemplate;

    @Column(name = "action_type", length = 30)
    private String actionType;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "skipped_at")
    private LocalDateTime skippedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MissionLog recommend(User user, MissionTemplate missionTemplate) {
        MissionLog log = new MissionLog();
        log.user = user;
        log.missionTemplate = missionTemplate;
        log.actionType = "RECOMMEND";
        return log;
    }

    public static MissionLog start(User user, MissionTemplate missionTemplate) {
        MissionLog log = new MissionLog();
        log.user = user;
        log.missionTemplate = missionTemplate;
        log.actionType = "START";
        log.startedAt = LocalDateTime.now();
        return log;
    }

    public static MissionLog reject(User user, MissionTemplate missionTemplate) {
        MissionLog log = new MissionLog();
        log.user = user;
        log.missionTemplate = missionTemplate;
        log.actionType = "REJECT";
        log.skippedAt = LocalDateTime.now();
        return log;
    }

    public static MissionLog complete(User user, MissionTemplate missionTemplate) {
        MissionLog log = new MissionLog();
        log.user = user;
        log.missionTemplate = missionTemplate;
        log.actionType = "COMPLETE";
        log.completedAt = LocalDateTime.now();
        return log;
    }

    public void start() {
        this.actionType = "START";
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    public void complete() {
        this.actionType = "COMPLETE";
        this.completedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
