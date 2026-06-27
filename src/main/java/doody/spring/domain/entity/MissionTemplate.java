package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mission_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionTemplate {

    @Id
    @Column(name = "mission_id", length = 100)
    private String id;

    @Column(nullable = false, length = 20)
    private String axis;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String waypoint;

    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;

    @Column(nullable = false)
    private Integer reward = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}