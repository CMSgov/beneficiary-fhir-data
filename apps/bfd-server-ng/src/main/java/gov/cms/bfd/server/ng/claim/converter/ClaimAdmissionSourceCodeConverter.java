package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimAdmissionSourceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimAdmissionSourceCodeConverter
    implements AttributeConverter<Optional<ClaimAdmissionSourceCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimAdmissionSourceCode> claimAdmissionSourceCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimAdmissionSourceCode.map(ClaimAdmissionSourceCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimAdmissionSourceCode> convertToEntityAttribute(String code) {
    return ClaimAdmissionSourceCode.tryFromCode(code);
  }
}
