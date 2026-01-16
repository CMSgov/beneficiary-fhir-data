package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.PpsIndicatorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class PpsIndicatorCodeConverter
    implements AttributeConverter<Optional<PpsIndicatorCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<PpsIndicatorCode> ppsIndicatorCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return ppsIndicatorCode.map(PpsIndicatorCode::getCode).orElse("");
  }

  @Override
  public Optional<PpsIndicatorCode> convertToEntityAttribute(String code) {
    return PpsIndicatorCode.fromCode(code);
  }
}
