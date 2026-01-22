package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimDispositionCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimDispositionCodeConverter
    implements AttributeConverter<Optional<ClaimDispositionCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimDispositionCode> claimDispositionCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimDispositionCode.map(ClaimDispositionCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimDispositionCode> convertToEntityAttribute(String code) {
    return ClaimDispositionCode.fromCode(code);
  }
}
