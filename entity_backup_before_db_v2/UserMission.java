package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_missions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_mission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "mission_id", nullable = false, length = 50)
    private String missionId;

    @Column(nullable = false, length = 100)
    private String axis;

    @Column(length = 100)
    private String waypoint;

    @Column(nullable = false)
    private Integer difficulty;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "how_to", nullable = false, columnDefinition = "jsonb")
    private String howTo;

    @Column(name = "recommend_reason", length = 1000)
    private String recommendReason;

    @Column(name = "delta_rhythm", precision = 10, scale = 2)
    private BigDecimal deltaRhythm;

    @Column(name = "delta_autonomy", precision = 10, scale = 2)
    private BigDecimal deltaAutonomy;

    @Column(name = "delta_connection", precision = 10, scale = 2)
    private BigDecimal deltaConnection;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "certified_image_url", length = 1000)
    private String certifiedImageUrl;

    @Column(nullable = false)
    private Integer reward;

    @Column(name = "recommended_at")
    private LocalDateTime recommendedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
}
