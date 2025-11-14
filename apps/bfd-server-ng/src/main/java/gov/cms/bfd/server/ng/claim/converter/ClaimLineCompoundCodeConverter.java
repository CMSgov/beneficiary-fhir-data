package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimLineCompoundCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimLineCompoundCodeConverter
    implements AttributeConverter<Optional<ClaimLineCompoundCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimLineCompoundCode> claimLineCompoundCode) {
    return claimLineCompoundCode.map(ClaimLineCompoundCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimLineCompoundCode> convertToEntityAttribute(String code) {
    return ClaimLineCompoundCode.tryFromCode(code);
  }
}
