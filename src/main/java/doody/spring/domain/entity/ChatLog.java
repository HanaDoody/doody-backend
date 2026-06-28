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
@Table(name = "chat_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reply;

    @Column(name = "current_mission_id", length = 100)
    private String currentMissionId;

    @Column(columnDefinition = "jsonb")
    private String signals;

    @Column(name = "suggested_action", length = 30)
    private String suggestedAction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static ChatLog create(
        User user,
        String message,
        String reply,
        String currentMissionId,
        String signals,
        String suggestedAction
    ) {
        ChatLog chatLog = new ChatLog();
        chatLog.user = user;
        chatLog.message = message;
        chatLog.reply = reply;
        chatLog.currentMissionId = currentMissionId;
        chatLog.signals = signals;
        chatLog.suggestedAction = suggestedAction;
        return chatLog;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}