package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimNearLineRecordTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimNearLineRecordTypeCodeConverter
    implements AttributeConverter<Optional<ClaimNearLineRecordTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimNearLineRecordTypeCode> claimRecordTypeCode) {
    return claimRecordTypeCode.map(ClaimNearLineRecordTypeCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimNearLineRecordTypeCode> convertToEntityAttribute(String s) {
    return ClaimNearLineRecordTypeCode.fromCode(s);
  }
}
