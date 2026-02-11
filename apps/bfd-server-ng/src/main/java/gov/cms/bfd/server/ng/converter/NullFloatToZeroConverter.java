package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts any null float values to 0.0. Note: This converter uses a scale of 2 with HALF_UP
 * rounding to handle precision mismatches caused by conversion (e.g., preventing 320.0899963378906
 * from being truncated incorrectly). If a future IDR field require more than 2 decimal places in
 * the future, this logic may need to be changed or moved to individual field mappings.
 */
@Converter(autoApply = true)
public class NullFloatToZeroConverter implements AttributeConverter<BigDecimal, Double> {
  @Override
  public Double convertToDatabaseColumn(BigDecimal value) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return value.doubleValue();
  }

  @Override
  public BigDecimal convertToEntityAttribute(Double value) {
    return (value == null)
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }
}
