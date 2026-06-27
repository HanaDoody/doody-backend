package doody.spring.domain.type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GoalChoiceConverter implements AttributeConverter<GoalChoice, String> {

    @Override
    public String convertToDatabaseColumn(GoalChoice attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public GoalChoice convertToEntityAttribute(String dbData) {
        return dbData == null ? null : GoalChoice.from(dbData);
    }
}