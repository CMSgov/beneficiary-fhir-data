package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimDiagnosisType;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimDiagnosisTypeConverter implements AttributeConverter<ClaimDiagnosisType, String> {
  @Override
  public String convertToDatabaseColumn(ClaimDiagnosisType claimDiagnosisType) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimDiagnosisType.getIdrCode();
  }

  @Override
  public ClaimDiagnosisType convertToEntityAttribute(String idrCode) {
    return ClaimDiagnosisType.fromIdrCode(idrCode);
  }
}
