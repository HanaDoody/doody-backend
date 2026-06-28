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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "contact_unlocks",
    uniqueConstraints = @UniqueConstraint(name = "uk_contact_unlocks_user_contact", columnNames = {"user_id", "contact_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContactUnlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_unlock_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contact_id", nullable = false, length = 100)
    private String contactId;

    @Column(nullable = false, length = 20)
    private String axis;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;

    public static ContactUnlock create(User user, String contactId, String axis, String source, Long sourceId) {
        ContactUnlock contactUnlock = new ContactUnlock();
        contactUnlock.user = user;
        contactUnlock.contactId = contactId;
        contactUnlock.axis = axis;
        contactUnlock.source = source;
        contactUnlock.sourceId = sourceId;
        return contactUnlock;
    }

    @PrePersist
    void prePersist() {
        if (this.unlockedAt == null) {
            this.unlockedAt = LocalDateTime.now();
        }
    }
}