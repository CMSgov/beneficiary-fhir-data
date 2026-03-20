package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimServiceDeductibleCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimServiceDeductibleCodeConverter
    implements AttributeConverter<Optional<ClaimServiceDeductibleCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimServiceDeductibleCode> claimServiceDeductibleCode) {
    return claimServiceDeductibleCode.map(ClaimServiceDeductibleCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimServiceDeductibleCode> convertToEntityAttribute(String code) {
    return ClaimServiceDeductibleCode.tryFromCode(code);
  }
}
