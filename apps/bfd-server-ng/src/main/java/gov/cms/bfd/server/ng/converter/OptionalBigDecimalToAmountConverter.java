package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Converts null BigDecimal values to an empty Optional and rounds to two decimal places. */
@Converter
public class OptionalBigDecimalToAmountConverter
    implements AttributeConverter<Optional<BigDecimal>, BigDecimal> {
  private static final int ROUNDING_SCALE = 2;

  @Override
  public BigDecimal convertToDatabaseColumn(Optional<BigDecimal> value) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return value.orElse(null);
  }

  @Override
  public Optional<BigDecimal> convertToEntityAttribute(BigDecimal value) {
    return Optional.ofNullable(value).map(v -> v.setScale(ROUNDING_SCALE, RoundingMode.HALF_UP));
  }
}
