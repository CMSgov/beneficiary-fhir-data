package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimRecordTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimRecordTypeCodeConverter
    implements AttributeConverter<Optional<ClaimRecordTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimRecordTypeCode> claimRecordTypeCode) {
    return claimRecordTypeCode.map(ClaimRecordTypeCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimRecordTypeCode> convertToEntityAttribute(String s) {
    return ClaimRecordTypeCode.fromCode(s);
  }
}
