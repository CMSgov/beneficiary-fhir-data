package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPricingLocalityCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPricingLocalityCodeConverter
    implements AttributeConverter<Optional<ClaimPricingLocalityCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimPricingLocalityCode> claimPricingLocalityCode) {
    return claimPricingLocalityCode.map(ClaimPricingLocalityCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPricingLocalityCode> convertToEntityAttribute(String code) {
    return ClaimPricingLocalityCode.fromCode(code);
  }
}
