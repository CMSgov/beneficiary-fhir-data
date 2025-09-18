package gov.cms.bfd.server.ng.converter;

import gov.cms.bfd.server.ng.util.IdrConstants;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts any IDR boolean strings ("Y"/"N") into Java booleans, defaulting to false if not
 * explicitly set to true.
 */
@Converter
public class DefaultFalseBooleanConverter implements AttributeConverter<Boolean, String> {
  @Override
  public String convertToDatabaseColumn(Boolean value) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return String.valueOf(value);
  }

  @Override
  public Boolean convertToEntityAttribute(String value) {
    return IdrConstants.YES.equals(value);
  }
}
