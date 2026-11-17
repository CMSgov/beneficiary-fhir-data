package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.priorauth.PriorAuthorizationReasonCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Converts optional prior authorization reason codes from database strings. */
@Converter(autoApply = true)
public class PriorAuthorizationReasonCodeConverter
    implements AttributeConverter<Optional<PriorAuthorizationReasonCode>, String> {

  @Override
  public String convertToDatabaseColumn(Optional<PriorAuthorizationReasonCode> reasonCode) {
    return reasonCode.map(PriorAuthorizationReasonCode::code).orElse("");
  }

  @Override
  public Optional<PriorAuthorizationReasonCode> convertToEntityAttribute(String code) {
    return PriorAuthorizationReasonCode.tryFromCode(code);
  }
}
