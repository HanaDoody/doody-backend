package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "collection_pins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionPin {

    @Id
    @Column(name = "pin_id", length = 100)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dudy_id", nullable = false)
    private DoodyTemplate doodyTemplate;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CollectionPin createRandom(String title, BigDecimal lat, BigDecimal lng, DoodyTemplate doodyTemplate) {
        CollectionPin pin = new CollectionPin();
        pin.id = "pin_" + UUID.randomUUID();
        pin.title = title;
        pin.lat = lat;
        pin.lng = lng;
        pin.doodyTemplate = doodyTemplate;
        pin.active = true;
        return pin;
    }

    @PrePersist
    void prePersist() {
        if (this.active == null) {
            this.active = true;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
