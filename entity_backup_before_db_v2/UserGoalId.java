package doody.spring.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserGoalId implements Serializable {

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "goal_id", length = 50)
    private String goalId;
}
