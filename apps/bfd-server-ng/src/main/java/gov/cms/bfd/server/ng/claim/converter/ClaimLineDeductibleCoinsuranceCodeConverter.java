package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimLineDeductibleCoinsuranceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimLineDeductibleCoinsuranceCodeConverter
    implements AttributeConverter<Optional<ClaimLineDeductibleCoinsuranceCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimLineDeductibleCoinsuranceCode> claimLineDeductibleCoinsuranceCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimLineDeductibleCoinsuranceCode
        .map(ClaimLineDeductibleCoinsuranceCode::getCode)
        .orElse("");
  }

  @Override
  public Optional<ClaimLineDeductibleCoinsuranceCode> convertToEntityAttribute(String idrCode) {
    return ClaimLineDeductibleCoinsuranceCode.tryFromCode(idrCode);
  }
}
