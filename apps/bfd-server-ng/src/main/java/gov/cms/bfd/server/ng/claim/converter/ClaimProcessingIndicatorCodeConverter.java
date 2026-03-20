package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimProcessingIndicatorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimProcessingIndicatorCodeConverter
    implements AttributeConverter<Optional<ClaimProcessingIndicatorCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimProcessingIndicatorCode> claimProcessingIndicatorCode) {
    return claimProcessingIndicatorCode.map(ClaimProcessingIndicatorCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimProcessingIndicatorCode> convertToEntityAttribute(String code) {
    return ClaimProcessingIndicatorCode.tryFromCode(code);
  }
}
