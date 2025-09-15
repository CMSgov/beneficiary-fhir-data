package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.RevenueCenterAnsiReasonCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class RevenueCenterAnsiReasonCodeConverter
    implements AttributeConverter<Optional<RevenueCenterAnsiReasonCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<RevenueCenterAnsiReasonCode> revenueCenterAnsiReasonCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return revenueCenterAnsiReasonCode.map(RevenueCenterAnsiReasonCode::getCode).orElse("");
  }

  @Override
  public Optional<RevenueCenterAnsiReasonCode> convertToEntityAttribute(String code) {
    return RevenueCenterAnsiReasonCode.tryFromCode(code);
  }
}
