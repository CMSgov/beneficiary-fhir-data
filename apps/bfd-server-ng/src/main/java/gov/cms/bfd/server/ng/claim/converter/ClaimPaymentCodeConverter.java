package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPaymentCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPaymentCodeConverter
    implements AttributeConverter<Optional<ClaimPaymentCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimPaymentCode> claimPaymentCode) {
    return claimPaymentCode.map(ClaimPaymentCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPaymentCode> convertToEntityAttribute(String code) {
    return ClaimPaymentCode.fromCode(code);
  }
}
