package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimSourceIdConverter implements AttributeConverter<ClaimSourceId, String> {
  @Override
  public String convertToDatabaseColumn(ClaimSourceId claimSourceId) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimSourceId.getId();
  }

  @Override
  public ClaimSourceId convertToEntityAttribute(String claimSourceId) {
    return ClaimSourceId.fromId(claimSourceId);
  }
}
