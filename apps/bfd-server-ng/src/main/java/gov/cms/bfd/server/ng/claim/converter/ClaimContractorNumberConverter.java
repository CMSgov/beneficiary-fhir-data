package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimContractorNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimContractorNumberConverter
    implements AttributeConverter<Optional<ClaimContractorNumber>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimContractorNumber> claimContractorNumber) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimContractorNumber.map(ClaimContractorNumber::getCode).orElse("");
  }

  @Override
  public Optional<ClaimContractorNumber> convertToEntityAttribute(String code) {
    return ClaimContractorNumber.fromCode(code);
  }
}
