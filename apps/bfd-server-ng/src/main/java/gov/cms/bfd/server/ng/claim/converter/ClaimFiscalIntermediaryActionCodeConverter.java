package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimFiscalIntermediaryActionCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimFiscalIntermediaryActionCodeConverter
    implements AttributeConverter<Optional<ClaimFiscalIntermediaryActionCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimFiscalIntermediaryActionCode> claimFiscalIntermediaryActionCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimFiscalIntermediaryActionCode
        .map(ClaimFiscalIntermediaryActionCode::getCode)
        .orElse("");
  }

  @Override
  public Optional<ClaimFiscalIntermediaryActionCode> convertToEntityAttribute(String code) {
    return ClaimFiscalIntermediaryActionCode.tryFromCode(code);
  }
}
