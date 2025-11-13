package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimLineBrandGenericCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimLineBrandGenericCodeConverter
    implements AttributeConverter<Optional<ClaimLineBrandGenericCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimLineBrandGenericCode> claimLineBrandGenericCode) {
    return claimLineBrandGenericCode.map(ClaimLineBrandGenericCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimLineBrandGenericCode> convertToEntityAttribute(String code) {
    return ClaimLineBrandGenericCode.tryFromCode(code);
  }
}
