package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPatientResidenceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPatientResidenceCodeConverter
    implements AttributeConverter<Optional<ClaimPatientResidenceCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimPatientResidenceCode> claimPatientResidenceCode) {
    return claimPatientResidenceCode.map(ClaimPatientResidenceCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPatientResidenceCode> convertToEntityAttribute(String code) {
    return ClaimPatientResidenceCode.tryFromCode(code);
  }
}
