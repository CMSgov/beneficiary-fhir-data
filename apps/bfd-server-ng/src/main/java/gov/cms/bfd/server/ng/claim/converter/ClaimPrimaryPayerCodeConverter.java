package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPrimaryPayerCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimPrimaryPayerCodeConverter
    implements AttributeConverter<Optional<ClaimPrimaryPayerCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimPrimaryPayerCode> claimPrimaryPayerCode) {
    return claimPrimaryPayerCode.map(ClaimPrimaryPayerCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimPrimaryPayerCode> convertToEntityAttribute(String code) {
    return ClaimPrimaryPayerCode.fromCode(code);
  }
}
