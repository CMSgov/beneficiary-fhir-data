package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimAdmissionTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimAdmissionTypeCodeConverter
    implements AttributeConverter<Optional<ClaimAdmissionTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimAdmissionTypeCode> claimAdmissionSourceCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimAdmissionSourceCode.map(ClaimAdmissionTypeCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimAdmissionTypeCode> convertToEntityAttribute(String code) {
    return ClaimAdmissionTypeCode.tryFromCode(code);
  }
}
