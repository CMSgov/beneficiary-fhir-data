package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPlaceOfServiceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPlaceOfServiceCodeConverter
    implements AttributeConverter<Optional<ClaimPlaceOfServiceCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimPlaceOfServiceCode> claimPlaceOfServiceCode) {
    return claimPlaceOfServiceCode.map(ClaimPlaceOfServiceCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPlaceOfServiceCode> convertToEntityAttribute(String code) {
    return ClaimPlaceOfServiceCode.fromCode(code);
  }
}
