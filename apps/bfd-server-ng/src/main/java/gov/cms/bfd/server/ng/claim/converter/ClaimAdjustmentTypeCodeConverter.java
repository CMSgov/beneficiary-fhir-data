package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimAdjustmentTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimAdjustmentTypeCodeConverter
    implements AttributeConverter<Optional<ClaimAdjustmentTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimAdjustmentTypeCode> claimAdjustmentTypeCode) {
    // This is a read-only API, so this method will never actually persist anything to the database.
    return claimAdjustmentTypeCode.map(ClaimAdjustmentTypeCode::code).orElse("");
  }

  @Override
  public Optional<ClaimAdjustmentTypeCode> convertToEntityAttribute(String code) {
    return ClaimAdjustmentTypeCode.fromCode(code);
  }
}
