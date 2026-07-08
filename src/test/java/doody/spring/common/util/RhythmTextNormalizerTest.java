package doody.spring.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RhythmTextNormalizerTest {

    @Test
    void replacesEveningPlaceholderKeyWithKoreanDefault() {
        assertThat(RhythmTextNormalizer.normalizeRhythmTitle("EVENING", "night_mood_default"))
            .isEqualTo("오늘 저녁 회고가 기록됐어.");
    }

    @Test
    void keepsShortEveningTextAfterPlaceholderPrefix() {
        assertThat(RhythmTextNormalizer.normalizeRhythmTitle("EVENING", "night_mood_default. kk"))
            .isEqualTo("kk");
    }

    @Test
    void keepsKoreanConsonantEveningTextAfterPlaceholderPrefix() {
        assertThat(RhythmTextNormalizer.normalizeRhythmTitle("EVENING", "night_mood_default. ㅇㅇ"))
            .isEqualTo("ㅇㅇ");
    }

    @Test
    void keepsNormalEveningText() {
        assertThat(RhythmTextNormalizer.normalizeRhythmTitle("EVENING", "오늘은 일찍 쉬고 싶어."))
            .isEqualTo("오늘은 일찍 쉬고 싶어.");
    }

    @Test
    void keepsTextThatOnlyStartsLikeAPlaceholder() {
        assertThat(RhythmTextNormalizer.normalizeRhythmTitle("EVENING", "night_mood_defaulted text"))
            .isEqualTo("night_mood_defaulted text");
    }

    @Test
    void formatsMorningReportTitleWithEnergy() {
        assertThat(RhythmTextNormalizer.reportRhythmTitle("MORNING", "좋아. 오늘 아침 리듬이 기록됐어.", (short) 4))
            .isEqualTo("그날의 에너지: 4");
    }

    @Test
    void formatsEveningReportTitleWithOneLineNote() {
        assertThat(RhythmTextNormalizer.reportRhythmTitle("EVENING", "night_mood_default. kk", null))
            .isEqualTo("그날의 한마디: kk");
    }
}
