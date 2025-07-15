package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimLineRevenueCenterCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimLineRevenueCenterCodeConverter
    implements AttributeConverter<Optional<ClaimLineRevenueCenterCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimLineRevenueCenterCode> claimLineRevenueCenterCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimLineRevenueCenterCode.map(ClaimLineRevenueCenterCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimLineRevenueCenterCode> convertToEntityAttribute(String code) {
    return ClaimLineRevenueCenterCode.tryFromCode(code);
  }
}
