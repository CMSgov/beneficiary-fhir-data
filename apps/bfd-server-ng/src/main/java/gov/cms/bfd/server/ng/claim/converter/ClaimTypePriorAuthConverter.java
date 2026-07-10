package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimTypePriorAuth;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimTypePriorAuthConverter
    implements AttributeConverter<Optional<ClaimTypePriorAuth>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimTypePriorAuth> claimTypePriorAuth) {
    return claimTypePriorAuth.map(ClaimTypePriorAuth::getCode).orElse("");
  }

  @Override
  public Optional<ClaimTypePriorAuth> convertToEntityAttribute(String code) {
    return ClaimTypePriorAuth.tryFromCode(code);
  }
}
