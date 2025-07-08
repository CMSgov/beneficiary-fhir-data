package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimDiagnosisType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimDiagnosisTypeConverter
    implements AttributeConverter<Optional<ClaimDiagnosisType>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimDiagnosisType> claimDiagnosisType) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimDiagnosisType.map(ClaimDiagnosisType::getIdrCode).orElse("");
  }

  @Override
  public Optional<ClaimDiagnosisType> convertToEntityAttribute(String idrCode) {
    return ClaimDiagnosisType.tryFromIdrCode(idrCode);
  }
}
