package doody.spring.common.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class RhythmTextNormalizer {

    private static final String MORNING_DEFAULT_TITLE = "오늘 아침 리듬이 기록됐어.";
    private static final String EVENING_DEFAULT_TITLE = "오늘 저녁 회고가 기록됐어.";
    private static final String UNKNOWN_MORNING_MOOD = "확인 안 됨";
    private static final Set<String> EVENING_PLACEHOLDER_KEYS = Set.of(
        "night_mood_default",
        "evening_mood_default"
    );
    private static final Pattern LEADING_SEPARATOR = Pattern.compile("^[\\s._:\\-/]+");

    private RhythmTextNormalizer() {
    }

    public static String normalizeRhythmTitle(String rhythmType, String text) {
        String normalizedType = rhythmType == null ? "" : rhythmType.toUpperCase(Locale.ROOT);
        if (text == null || text.isBlank()) {
            return defaultTitle(normalizedType);
        }

        String title = text.strip();
        if ("EVENING".equals(normalizedType)) {
            String placeholderRemainder = removeEveningPlaceholderPrefix(title);
            if (placeholderRemainder != null) {
                return placeholderRemainder.isBlank()
                    ? EVENING_DEFAULT_TITLE
                    : placeholderRemainder;
            }
        }

        return title;
    }

    public static String reportRhythmTitle(String rhythmType, String text, Short energy) {
        String normalizedType = rhythmType == null ? "" : rhythmType.toUpperCase(Locale.ROOT);
        if ("MORNING".equals(normalizedType)) {
            return "그날의 에너지: " + (energy == null ? UNKNOWN_MORNING_MOOD : energy);
        }
        if ("EVENING".equals(normalizedType)) {
            return "그날의 한마디: " + normalizeRhythmTitle(normalizedType, text);
        }
        return normalizeRhythmTitle(normalizedType, text);
    }

    private static String defaultTitle(String rhythmType) {
        if ("MORNING".equals(rhythmType)) {
            return MORNING_DEFAULT_TITLE;
        }
        if ("EVENING".equals(rhythmType)) {
            return EVENING_DEFAULT_TITLE;
        }
        return rhythmType;
    }

    private static String removeEveningPlaceholderPrefix(String title) {
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        for (String placeholderKey : EVENING_PLACEHOLDER_KEYS) {
            if (lowerTitle.equals(placeholderKey)) {
                return "";
            }
            if (lowerTitle.startsWith(placeholderKey)) {
                String remainder = title.substring(placeholderKey.length());
                if (remainder.isBlank() || isSeparator(remainder.charAt(0))) {
                    return LEADING_SEPARATOR.matcher(remainder).replaceFirst("").strip();
                }
            }
        }
        return null;
    }

    private static boolean isSeparator(char value) {
        return Character.isWhitespace(value)
            || value == '.'
            || value == '_'
            || value == ':'
            || value == '-'
            || value == '/';
    }

}
