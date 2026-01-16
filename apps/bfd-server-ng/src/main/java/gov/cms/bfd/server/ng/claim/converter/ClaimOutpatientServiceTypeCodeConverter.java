package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimOutpatientServiceTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimOutpatientServiceTypeCodeConverter
    implements AttributeConverter<Optional<ClaimOutpatientServiceTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimOutpatientServiceTypeCode> claimOutpatientServiceTypeCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimOutpatientServiceTypeCode.map(ClaimOutpatientServiceTypeCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimOutpatientServiceTypeCode> convertToEntityAttribute(String code) {
    return ClaimOutpatientServiceTypeCode.tryFromCode(code);
  }
}
