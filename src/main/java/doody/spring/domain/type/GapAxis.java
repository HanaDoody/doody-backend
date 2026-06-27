package doody.spring.domain.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum GapAxis {
    RHYTHM("RHYTHM"),
    AUTONOMY("AUTONOMY"),
    CONNECTION("CONNECTION"),
    COMPOSITE("COMPOSITE"),
    UNKNOWN("UNKNOWN");

    private final String value;

    GapAxis(String value) {
        this.value = value;
    }

    @JsonCreator
    public static GapAxis from(String value) {
        return Arrays.stream(values())
            .filter(axis -> axis.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("吏?먰븯吏 ?딅뒗 gapAxis?낅땲?? " + value));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}