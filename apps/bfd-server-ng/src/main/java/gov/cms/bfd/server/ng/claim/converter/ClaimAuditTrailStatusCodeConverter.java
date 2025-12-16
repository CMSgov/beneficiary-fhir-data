package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimAuditTrailStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimAuditTrailStatusCodeConverter
    implements AttributeConverter<Optional<ClaimAuditTrailStatusCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimAuditTrailStatusCode> claimCurrentStatusCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimCurrentStatusCode.map(ClaimAuditTrailStatusCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimAuditTrailStatusCode> convertToEntityAttribute(String idrCode) {
    return ClaimAuditTrailStatusCode.tryFromCode(idrCode);
  }
}
