package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "onboarding_responses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingResponse {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blank_id", nullable = false)
    private Blank blank;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "blank_choice", nullable = false, length = 100)
    private String blankChoice;

    @Column(name = "initial_energy", nullable = false)
    private Integer initialEnergy;

    @Column(name = "goal_choice", nullable = false, length = 100)
    private String goalChoice;

    @Column(nullable = false, length = 50)
    private String period;

    @Column(name = "diagnostic_responses", nullable = false, columnDefinition = "jsonb")
    private String diagnosticResponses;

    @Column(name = "free_text", nullable = false, length = 2000)
    private String freeText;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
