package doody.spring.domain.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum RecommendedPeriod {
    TODAY("today"),
    ONE_WEEK("1w"),
    ONE_MONTH("1m"),
    THREE_MONTHS("3m");

    private final String value;

    RecommendedPeriod(String value) {
        this.value = value;
    }

    @JsonCreator
    public static RecommendedPeriod from(String value) {
        return Arrays.stream(values())
            .filter(period -> period.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("吏?먰븯吏 ?딅뒗 recommendedPeriod?낅땲?? " + value));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}