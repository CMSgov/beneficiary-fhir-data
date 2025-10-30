package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPrescriptionOriginCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPrescriptionOriginCodeConverter
    implements AttributeConverter<Optional<ClaimPrescriptionOriginCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimPrescriptionOriginCode> claimPrescriptionOriginCode) {
    return claimPrescriptionOriginCode.map(ClaimPrescriptionOriginCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPrescriptionOriginCode> convertToEntityAttribute(String code) {
    return ClaimPrescriptionOriginCode.tryFromCode(code);
  }
}
