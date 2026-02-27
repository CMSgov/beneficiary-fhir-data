package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Converts any zero values to None. */
@Converter(autoApply = true)
public class NonZeroIntConverter implements AttributeConverter<Optional<Integer>, Integer> {
  @Override
  public Integer convertToDatabaseColumn(Optional<Integer> value) {
    // This is a read-only API so this method will never actually persist anything to the
    // database.
    return value.orElse(0);
  }

  @Override
  public Optional<Integer> convertToEntityAttribute(Integer value) {

    if (value == null || value == 0) {
      return Optional.empty();
    }
    return Optional.of(value);
  }
}
