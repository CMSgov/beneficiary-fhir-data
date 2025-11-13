package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimDispenseStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimDispenseStatusCodeConverter
    implements AttributeConverter<Optional<ClaimDispenseStatusCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimDispenseStatusCode> claimDispenseStatusCode) {
    return claimDispenseStatusCode.map(ClaimDispenseStatusCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimDispenseStatusCode> convertToEntityAttribute(String code) {
    return ClaimDispenseStatusCode.tryFromCode(code);
  }
}
