package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimQueryCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimQueryCodeConverter
    implements AttributeConverter<Optional<ClaimQueryCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimQueryCode> claimQueryCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return claimQueryCode.map(ClaimQueryCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimQueryCode> convertToEntityAttribute(String code) {
    return ClaimQueryCode.tryFromCode(code);
  }
}
