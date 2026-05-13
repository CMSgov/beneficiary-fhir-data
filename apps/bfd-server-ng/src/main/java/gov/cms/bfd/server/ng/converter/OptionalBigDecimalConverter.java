package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.util.Optional;

/** Converts null BigDecimal values to an empty Optional. */
@Converter(autoApply = true)
public class OptionalBigDecimalConverter
    implements AttributeConverter<Optional<BigDecimal>, BigDecimal> {
  @Override
  public BigDecimal convertToDatabaseColumn(Optional<BigDecimal> value) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return value.orElse(null);
  }

  @Override
  public Optional<BigDecimal> convertToEntityAttribute(BigDecimal value) {
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(value);
  }
}
