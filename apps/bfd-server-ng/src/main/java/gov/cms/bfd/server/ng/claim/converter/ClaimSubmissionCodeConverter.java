package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimSubmissionCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimSubmissionCodeConverter
    implements AttributeConverter<Optional<ClaimSubmissionCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimSubmissionCode> claimSubmissionCode) {
    return claimSubmissionCode.map(ClaimSubmissionCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimSubmissionCode> convertToEntityAttribute(String code) {
    return ClaimSubmissionCode.tryFromCode(code);
  }
}
