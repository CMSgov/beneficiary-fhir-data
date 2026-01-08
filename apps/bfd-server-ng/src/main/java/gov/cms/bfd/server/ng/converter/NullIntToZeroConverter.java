package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converts any null Integer values to 0. */
@Converter(autoApply = true)
public class NullIntToZeroConverter implements AttributeConverter<Integer, Integer> {
  @Override
  public Integer convertToDatabaseColumn(Integer value) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return value;
  }

  @Override
  public Integer convertToEntityAttribute(Integer value) {
    return value == null ? 0 : value;
  }
}
