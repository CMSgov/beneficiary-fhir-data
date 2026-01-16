package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts any null float values to 0.0. */
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
        : BigDecimal.valueOf(value).setScale(2, RoundingMode.DOWN);
  }
}
