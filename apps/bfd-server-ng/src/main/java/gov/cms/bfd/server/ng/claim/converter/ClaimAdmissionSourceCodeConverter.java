package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimAdmissionSourceCode;
import gov.cms.bfd.server.ng.claim.model.ClaimDiagnosisType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimAdmissionSourceCodeConverter
    implements AttributeConverter<ClaimAdmissionSourceCode, String> {
  @Override
  public String convertToDatabaseColumn(ClaimAdmissionSourceCode claimAdmissionSourceCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimAdmissionSourceCode.getCode();
  }

  @Override
  public ClaimAdmissionSourceCode convertToEntityAttribute(String code) {
    return ClaimAdmissionSourceCode.fromCode(code);
  }
}
