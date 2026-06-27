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
@Table(name = "user_chat_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_message", nullable = false, length = 2000)
    private String userMessage;

    @Column(name = "ai_reply", nullable = false, length = 2000)
    private String aiReply;

    @Column(name = "signal_rhythm", precision = 10, scale = 2)
    private BigDecimal signalRhythm;

    @Column(name = "signal_autonomy", precision = 10, scale = 2)
    private BigDecimal signalAutonomy;

    @Column(name = "signal_connection", precision = 10, scale = 2)
    private BigDecimal signalConnection;

    @Column(name = "context_mission_id", length = 50)
    private String contextMissionId;

    @Column(name = "context_energy")
    private Integer contextEnergy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
