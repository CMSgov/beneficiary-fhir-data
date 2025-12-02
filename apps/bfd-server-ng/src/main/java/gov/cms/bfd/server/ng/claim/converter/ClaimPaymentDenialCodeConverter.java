package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPaymentDenialCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPaymentDenialCodeConverter
    implements AttributeConverter<Optional<ClaimPaymentDenialCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimPaymentDenialCode> claimPaymentDenialCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimPaymentDenialCode.map(ClaimPaymentDenialCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPaymentDenialCode> convertToEntityAttribute(String code) {
    return ClaimPaymentDenialCode.tryFromCode(code);
  }
}
