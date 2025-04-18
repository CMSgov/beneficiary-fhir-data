package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

@Converter(autoApply = true)
public class StringConverter implements AttributeConverter<Optional<String>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<String> maybeString) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return maybeString.orElse("");
  }

  @Override
  public Optional<String> convertToEntityAttribute(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }
}
