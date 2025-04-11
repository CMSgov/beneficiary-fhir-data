package gov.cms.bfd.server.ng.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.util.Optional;

@Converter(autoApply = true)
public class DateConverter implements AttributeConverter<Optional<LocalDate>, LocalDate> {

  @Override
  public LocalDate convertToDatabaseColumn(Optional<LocalDate> maybeDate) {
    return maybeDate.orElse(IdrConstants.DEFAULT_DATE);
  }

  @Override
  public Optional<LocalDate> convertToEntityAttribute(LocalDate value) {
    if (value.equals(IdrConstants.DEFAULT_DATE)) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }
}
