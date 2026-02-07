package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimAuditTrailLocationCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimAuditTrailLocationCodeConverter
    implements AttributeConverter<ClaimAuditTrailLocationCode, String> {
  @Override
  public String convertToDatabaseColumn(ClaimAuditTrailLocationCode claimAuditTrailLocationCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimAuditTrailLocationCode.getCode();
  }

  @Override
  public ClaimAuditTrailLocationCode convertToEntityAttribute(String code) {
    return ClaimAuditTrailLocationCode.tryFromCode(code).orElse(ClaimAuditTrailLocationCode.NA);
  }
}
