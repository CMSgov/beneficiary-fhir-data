package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Converts any zero double values to None. */
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
