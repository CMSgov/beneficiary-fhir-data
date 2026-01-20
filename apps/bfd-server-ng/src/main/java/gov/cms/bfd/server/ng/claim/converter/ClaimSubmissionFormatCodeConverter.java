package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimSubmissionFormatCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimSubmissionFormatCodeConverter
    implements AttributeConverter<Optional<ClaimSubmissionFormatCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimSubmissionFormatCode> claimSubmissionFormatCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimSubmissionFormatCode.map(ClaimSubmissionFormatCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimSubmissionFormatCode> convertToEntityAttribute(String code) {
    return ClaimSubmissionFormatCode.fromCode(code);
  }
}
