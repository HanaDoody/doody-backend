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

    @Column(name = "mission_type", nullable = false, length = 30)
    private String missionType = "CHECK";

    @Column(nullable = false)
    private Integer difficulty = 1;

    @Column
    private Integer stage;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "how_to", columnDefinition = "TEXT")
    private String howTo;

    @Column(name = "required_evidence_count", nullable = false)
    private Integer requiredEvidenceCount = 0;

    @Column(name = "fallback_mission_id", length = 100)
    private String fallbackMissionId;

    @Column(name = "goal_tags", columnDefinition = "TEXT")
    private String goalTags;

    @Column(name = "is_signature", nullable = false)
    private Boolean signature = false;

    @Column(name = "is_fallback", nullable = false)
    private Boolean fallback = false;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}
