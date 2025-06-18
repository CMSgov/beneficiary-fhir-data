package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimLineDeductibleCoinsuranceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimLineDeductibleCoinsuranceCodeConverter
    implements AttributeConverter<ClaimLineDeductibleCoinsuranceCode, String> {
  @Override
  public String convertToDatabaseColumn(
      ClaimLineDeductibleCoinsuranceCode claimLineDeductibleCoinsuranceCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimLineDeductibleCoinsuranceCode.getCode();
  }

  @Override
  public ClaimLineDeductibleCoinsuranceCode convertToEntityAttribute(String idrCode) {
    return ClaimLineDeductibleCoinsuranceCode.fromCode(idrCode);
  }
}
