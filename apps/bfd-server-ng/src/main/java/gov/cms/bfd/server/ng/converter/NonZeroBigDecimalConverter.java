package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Converts any null float values to an empty Optional. This should be manually invoked on fields
 * where a zero value should not be rendered in the FHIR. Note: This converter uses a scale of 2
 * with HALF_UP rounding to handle precision mismatches caused by conversion (e.g., preventing
 * 320.0899963378906 from being truncated incorrectly). If a future IDR field requires more than 2
 * decimal places, this logic may need to be changed or moved to individual field mappings.
 */
@Converter
public class NonZeroBigDecimalConverter
    implements AttributeConverter<Optional<BigDecimal>, BigDecimal> {
  @Override
  public BigDecimal convertToDatabaseColumn(Optional<BigDecimal> value) {
    // This API is read-only, so this won't actually persist to the DB.
    return value.orElse(BigDecimal.ZERO);
  }

  @Override
  public Optional<BigDecimal> convertToEntityAttribute(BigDecimal value) {
    if (value == null || value.equals(BigDecimal.ZERO)) {
      return Optional.empty();
    }
    return Optional.of(value.setScale(2, RoundingMode.HALF_UP));
  }
}
