package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimTypePriorAuth;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimTypePriorAuthConverter implements AttributeConverter<ClaimTypePriorAuth, String> {
  @Override
  public String convertToDatabaseColumn(ClaimTypePriorAuth claimTypePriorAuth) {
    return claimTypePriorAuth.getCode();
  }

  @Override
  public ClaimTypePriorAuth convertToEntityAttribute(String code) {
    return ClaimTypePriorAuth.tryFromCode(code);
  }
}
