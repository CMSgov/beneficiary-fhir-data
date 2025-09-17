package gov.cms.bfd.server.ng.converter;

import gov.cms.bfd.server.ng.util.IdrConstants;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.util.Optional;

/** Converts any missing or default dates to an optional value. */
@Converter(autoApply = true)
public class OptionalDateConverter implements AttributeConverter<Optional<LocalDate>, LocalDate> {

  @Override
  public LocalDate convertToDatabaseColumn(Optional<LocalDate> maybeDate) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return maybeDate.orElse(IdrConstants.DEFAULT_DATE);
  }

  @Override
  public Optional<LocalDate> convertToEntityAttribute(LocalDate value) {
    if (value == null || value.equals(IdrConstants.DEFAULT_DATE)) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }
}
