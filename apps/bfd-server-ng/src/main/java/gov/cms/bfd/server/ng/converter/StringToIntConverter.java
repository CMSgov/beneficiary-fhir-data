package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converts any string values to integer. */
@Converter
public class StringToIntConverter implements AttributeConverter<Integer, String> {
  @Override
  public String convertToDatabaseColumn(Integer value) {
    return String.valueOf(value);
  }

  @Override
  public Integer convertToEntityAttribute(String value) {
    if (value == null || value.trim().isEmpty()) {
      return 0;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid authorized fill number: " + value, e);
    }
  }
}
