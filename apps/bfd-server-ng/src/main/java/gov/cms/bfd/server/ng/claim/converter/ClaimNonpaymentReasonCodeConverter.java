package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimNonpaymentReasonCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimNonpaymentReasonCodeConverter
    implements AttributeConverter<Optional<ClaimNonpaymentReasonCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimNonpaymentReasonCode> claimNonpaymentReasonCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimNonpaymentReasonCode.map(ClaimNonpaymentReasonCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimNonpaymentReasonCode> convertToEntityAttribute(String code) {
    return ClaimNonpaymentReasonCode.tryFromCode(code);
  }
}
