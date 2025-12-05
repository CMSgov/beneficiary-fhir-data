package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimRelatedConditionCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimRelatedConditionCodeConverter
    implements AttributeConverter<Optional<ClaimRelatedConditionCode>, String> {

  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimRelatedConditionCode> claimRelatedConditionCode) {
    return claimRelatedConditionCode.map(ClaimRelatedConditionCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimRelatedConditionCode> convertToEntityAttribute(String code) {
    return ClaimRelatedConditionCode.fromCode(code);
  }
}
