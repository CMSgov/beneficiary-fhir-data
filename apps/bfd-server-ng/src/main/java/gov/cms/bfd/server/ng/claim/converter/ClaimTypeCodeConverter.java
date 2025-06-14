package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimTypeCodeConverter implements AttributeConverter<ClaimTypeCode, Integer> {
  @Override
  public Integer convertToDatabaseColumn(ClaimTypeCode claimTypeCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimTypeCode.getCode();
  }

  @Override
  public ClaimTypeCode convertToEntityAttribute(Integer claimTypeCode) {
    return ClaimTypeCode.fromCode(claimTypeCode);
  }
}
