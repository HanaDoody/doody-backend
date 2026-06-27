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
    name = "doody_collection",
    uniqueConstraints = @UniqueConstraint(name = "uk_doody_collection_user_dudy", columnNames = {"user_id", "dudy_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoodyCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dudy_collection_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dudy_id", nullable = false)
    private DoodyTemplate doodyTemplate;

    @Column(nullable = false, length = 20)
    private String tier;

    @Column(length = 20)
    private String axis;

    @Column(name = "earned_reason", nullable = false, columnDefinition = "TEXT")
    private String earnedReason;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    public static DoodyCollection create(
        User user,
        DoodyTemplate doodyTemplate,
        String tier,
        String axis,
        String earnedReason,
        String source,
        Long sourceId
    ) {
        DoodyCollection collection = new DoodyCollection();
        collection.user = user;
        collection.doodyTemplate = doodyTemplate;
        collection.tier = tier;
        collection.axis = axis;
        collection.earnedReason = earnedReason;
        collection.source = source;
        collection.sourceId = sourceId;
        return collection;
    }

    @PrePersist
    void prePersist() {
        if (this.collectedAt == null) {
            this.collectedAt = LocalDateTime.now();
        }
    }
}