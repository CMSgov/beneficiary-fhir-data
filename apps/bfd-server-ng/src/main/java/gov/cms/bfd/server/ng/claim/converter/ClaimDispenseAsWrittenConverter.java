package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimDispenseAsWrittenCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimDispenseAsWrittenConverter
    implements AttributeConverter<Optional<ClaimDispenseAsWrittenCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimDispenseAsWrittenCode> claimDispenseAsWrittenCode) {
    return claimDispenseAsWrittenCode.map(ClaimDispenseAsWrittenCode::getCode).orElse("0");
  }

  @Override
  public Optional<ClaimDispenseAsWrittenCode> convertToEntityAttribute(String code) {
    if (code == null || code.isBlank()) {
      return ClaimDispenseAsWrittenCode.tryFromCode("0");
    }
    return ClaimDispenseAsWrittenCode.tryFromCode(code.strip());
  }
}
