package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimAdmissionSourceCode;
import gov.cms.bfd.server.ng.claim.model.ClaimAdmissionTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimAdmissionTypeCodeConverter
    implements AttributeConverter<ClaimAdmissionTypeCode, String> {
  @Override
  public String convertToDatabaseColumn(ClaimAdmissionTypeCode claimAdmissionSourceCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimAdmissionSourceCode.getCode();
  }

  @Override
  public ClaimAdmissionTypeCode convertToEntityAttribute(String code) {
    return ClaimAdmissionTypeCode.fromCode(code);
  }
}
