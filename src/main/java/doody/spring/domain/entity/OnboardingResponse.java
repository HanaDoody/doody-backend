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
@Table(name = "onboarding_response")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "onboarding_response_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "gap_axis", length = 20)
    private String gapAxis;

    @Column(name = "goal_choice", length = 64)
    private String goalChoice;

    @Column(name = "recommended_period", length = 10)
    private String recommendedPeriod;

    @Column(name = "rhythm_choice")
    private Integer rhythmChoice;

    @Column(name = "autonomy_choice")
    private Integer autonomyChoice;

    @Column(name = "connection_choice")
    private Integer connectionChoice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}