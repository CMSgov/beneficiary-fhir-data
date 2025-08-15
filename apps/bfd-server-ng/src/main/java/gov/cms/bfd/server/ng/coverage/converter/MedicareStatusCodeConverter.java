package gov.cms.bfd.server.ng.coverage.converter;

import gov.cms.bfd.server.ng.coverage.model.MedicareStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Converts a medicare status code to and from the database representation. */
@Converter(autoApply = true)
public class MedicareStatusCodeConverter
    implements AttributeConverter<Optional<MedicareStatusCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<MedicareStatusCode> statusCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return statusCode.map(MedicareStatusCode::getCode).orElse("");
  }

  @Override
  public Optional<MedicareStatusCode> convertToEntityAttribute(String languageCode) {
    return MedicareStatusCode.tryFromCode(languageCode);
  }
}
