package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DefaultFalseBooleanConverter implements AttributeConverter<Boolean, String> {
  @Override
  public String convertToDatabaseColumn(Boolean value) {
    return String.valueOf(value);
  }

  @Override
  public Boolean convertToEntityAttribute(String value) {
    return IdrConstants.YES.equals(value);
  }
}
