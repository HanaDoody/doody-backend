package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "doody_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoodyTemplate {

    @Id
    @Column(name = "dudy_id", length = 100)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String tier;

    @Column(length = 20)
    private String axis;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "unlock_condition", columnDefinition = "TEXT")
    private String unlockCondition;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.active == null) {
            this.active = true;
        }
        this.createdAt = LocalDateTime.now();
    }
}