package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimFederalTypeOfServiceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimFederalTypeOfServiceCodeConverter
    implements AttributeConverter<Optional<ClaimFederalTypeOfServiceCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimFederalTypeOfServiceCode> claimFederalTypeOfServiceCode) {
    return claimFederalTypeOfServiceCode.map(ClaimFederalTypeOfServiceCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimFederalTypeOfServiceCode> convertToEntityAttribute(String code) {
    return ClaimFederalTypeOfServiceCode.fromCode(code);
  }
}
