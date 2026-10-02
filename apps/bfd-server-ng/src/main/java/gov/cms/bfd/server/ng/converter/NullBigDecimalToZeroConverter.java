package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts any null BigDecimal values to 0. Note: This converter uses a scale of 2 with HALF_UP
 * rounding to handle precision mismatches caused by conversion (e.g., preventing 320.0899963378906
 * from being truncated incorrectly). If a future IDR field requires more than 2 decimal places,
 * this logic may need to be changed or moved to individual field mappings.
 */
@Converter(autoApply = true)
public class NullBigDecimalToZeroConverter implements AttributeConverter<BigDecimal, BigDecimal> {
  @Override
  public BigDecimal convertToDatabaseColumn(BigDecimal value) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return value;
  }

  @Override
  public BigDecimal convertToEntityAttribute(BigDecimal value) {
    return (value == null) ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }
}
