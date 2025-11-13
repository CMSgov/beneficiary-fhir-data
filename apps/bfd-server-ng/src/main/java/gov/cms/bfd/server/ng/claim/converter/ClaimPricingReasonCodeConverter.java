package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPricingReasonCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPricingReasonCodeConverter
    implements AttributeConverter<Optional<ClaimPricingReasonCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimPricingReasonCode> claimPricingReasonCode) {
    return claimPricingReasonCode.map(ClaimPricingReasonCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPricingReasonCode> convertToEntityAttribute(String code) {
    return ClaimPricingReasonCode.tryFromCode(code);
  }
}
