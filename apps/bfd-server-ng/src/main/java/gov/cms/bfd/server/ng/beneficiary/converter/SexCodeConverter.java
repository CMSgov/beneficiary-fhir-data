package gov.cms.bfd.server.ng.beneficiary.converter;

import gov.cms.bfd.server.ng.beneficiary.model.SexCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Converts the sex code between the string value in the database and a {@link SexCode}. */
@Converter(autoApply = true)
public class SexCodeConverter implements AttributeConverter<Optional<SexCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<SexCode> sexCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return sexCode.map(SexCode::getIdrCode).orElse("");
  }

  @Override
  public Optional<SexCode> convertToEntityAttribute(String idrCode) {
    return SexCode.tryFromIdrCode(idrCode);
  }
}
