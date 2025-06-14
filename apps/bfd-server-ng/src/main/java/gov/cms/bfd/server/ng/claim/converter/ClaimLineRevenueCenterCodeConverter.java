package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimLineRevenueCenterCode;
import jakarta.persistence.AttributeConverter;

public class ClaimLineRevenueCenterCodeConverter
    implements AttributeConverter<ClaimLineRevenueCenterCode, String> {
  @Override
  public String convertToDatabaseColumn(ClaimLineRevenueCenterCode claimLineRevenueCenterCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimLineRevenueCenterCode.getCode();
  }

  @Override
  public ClaimLineRevenueCenterCode convertToEntityAttribute(String code) {
    return ClaimLineRevenueCenterCode.fromCode(code);
  }
}
