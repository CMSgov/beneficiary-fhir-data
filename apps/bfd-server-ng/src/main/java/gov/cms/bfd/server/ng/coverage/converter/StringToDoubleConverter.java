package gov.cms.bfd.server.ng.coverage.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converts any string value to double. */
@Converter
public class StringToDoubleConverter implements AttributeConverter<Double, String> {
  @Override
  public String convertToDatabaseColumn(Double value) {
    return String.valueOf(value);
  }

  @Override
  public Double convertToEntityAttribute(String value) {
    if (value == null || value.trim().isEmpty()) {
      return 0.0;
    }

    try {
      return (double) Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new RuntimeException("Numeric input was not in a valid format: " + value, e);
    }
  }
}
