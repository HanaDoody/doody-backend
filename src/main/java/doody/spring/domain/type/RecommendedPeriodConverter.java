package doody.spring.domain.type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RecommendedPeriodConverter implements AttributeConverter<RecommendedPeriod, String> {

    @Override
    public String convertToDatabaseColumn(RecommendedPeriod attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public RecommendedPeriod convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RecommendedPeriod.from(dbData);
    }
}