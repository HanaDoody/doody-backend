package doody.spring.domain.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum GoalChoice {
    AUTONOMY("AUTONOMY"),
    RHYTHM("RHYTHM"),
    CONNECTION("CONNECTION");

    private final String value;

    GoalChoice(String value) {
        this.value = value;
    }

    @JsonCreator
    public static GoalChoice from(String value) {
        return Arrays.stream(values())
            .filter(choice -> choice.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 goalChoice야: " + value));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
