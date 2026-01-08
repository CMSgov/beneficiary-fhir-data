package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts any null Float values to 0.0 Double. */
@Converter(autoApply = true)
public class NullFloatToZeroConverter implements AttributeConverter<Double, Float> {
  @Override
  public Float convertToDatabaseColumn(Double value) {
    // This is a read-only API so this method will never actually persist anything to the database.
    if (value == null) {
      return null;
    }
    return value.floatValue();
  }

  @Override
  public Double convertToEntityAttribute(Float value) {
    if (value == null) {
      return 0.0;
    }
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
