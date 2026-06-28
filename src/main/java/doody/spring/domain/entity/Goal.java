package doody.spring.domain.entity;

import doody.spring.domain.type.RecommendedPeriod;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "goals",
    uniqueConstraints = @UniqueConstraint(name = "uk_goals_user_active", columnNames = {"user_id", "is_active"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "autonomy_goal", nullable = false, length = 100)
    private String autonomyGoal;

    @Column(name = "connection_goal", nullable = false, length = 100)
    private String connectionGoal;

    @Column(length = 10)
    private RecommendedPeriod period;

    @Column(name = "first_step_mission", columnDefinition = "TEXT")
    private String firstStepMission;

    @Column(precision = 4, scale = 3)
    private BigDecimal rhythm;

    @Column(precision = 4, scale = 3)
    private BigDecimal autonomy;

    @Column(precision = 4, scale = 3)
    private BigDecimal connection;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Goal create(
        User user,
        String title,
        String autonomyGoal,
        String connectionGoal,
        RecommendedPeriod period,
        String firstStepMission
    ) {
        Goal goal = new Goal();
        goal.user = user;
        goal.title = title;
        goal.autonomyGoal = autonomyGoal;
        goal.connectionGoal = connectionGoal;
        goal.period = period;
        goal.firstStepMission = firstStepMission;
        goal.active = true;
        return goal;
    }

    public void updateAri(BigDecimal rhythm, BigDecimal autonomy, BigDecimal connection) {
        this.rhythm = rhythm;
        this.autonomy = autonomy;
        this.connection = connection;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }
}