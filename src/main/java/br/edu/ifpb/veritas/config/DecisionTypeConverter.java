package br.edu.ifpb.veritas.config;

import br.edu.ifpb.veritas.enums.DecisionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Conversor JPA customizado para DecisionType.
 * Permite compatibilidade com valores antigos do banco de dados (DEFERIDO/INDEFERIDO).
 * Converte automaticamente para os novos valores (DEFERIMENTO/INDEFERIMENTO).
 */
@Converter(autoApply = true)
public class DecisionTypeConverter implements AttributeConverter<DecisionType, String> {

    @Override
    public String convertToDatabaseColumn(DecisionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public DecisionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return DecisionType.fromValue(dbData);
    }
}
