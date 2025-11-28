package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Converts any zero double values to None. */
@Converter
public class NonZeroDoubleConverter implements AttributeConverter<Optional<Double>, Double> {
  @Override
  public Double convertToDatabaseColumn(Optional<Double> value) {
    // This API is read-only, so this won't actually persist to the DB.
    return value.orElse(0.0);
  }

  @Override
  public Optional<Double> convertToEntityAttribute(Double value) {
    if (value == null || value == 0.0) {
      return Optional.empty();
    }
    return Optional.of(value);
  }
}
