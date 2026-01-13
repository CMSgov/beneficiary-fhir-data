package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimFinalAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Database code converter for ClaimFinalAction. */
@Converter(autoApply = true)
public class ClaimFinalActionConverter implements AttributeConverter<ClaimFinalAction, Character> {
  @Override
  public Character convertToDatabaseColumn(ClaimFinalAction finalAction) {
    return finalAction.getCode();
  }

  @Override
  public ClaimFinalAction convertToEntityAttribute(Character code) {
    return ClaimFinalAction.fromCode(code);
  }
}
