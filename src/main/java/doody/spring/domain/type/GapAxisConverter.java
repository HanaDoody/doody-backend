package doody.spring.domain.type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GapAxisConverter implements AttributeConverter<GapAxis, String> {

    @Override
    public String convertToDatabaseColumn(GapAxis attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public GapAxis convertToEntityAttribute(String dbData) {
        return dbData == null ? null : GapAxis.from(dbData);
    }
}