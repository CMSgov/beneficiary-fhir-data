package gov.cms.bfd.server.ng.claim.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Converts the POA indicator, mapping 0 -> empty, because 0 really means blank. */
@Converter
public class ClaimPoaIndicatorConverter implements AttributeConverter<Optional<String>, String> {

  @Override
  public String convertToDatabaseColumn(Optional<String> attribute) {
    return attribute.orElse("");
  }

  @Override
  public Optional<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty() || "0".equals(dbData)) {
      // Special snowflake - treat 0 as null for POA indicators.
      return Optional.empty();
    }
    return Optional.of(dbData);
  }
}
