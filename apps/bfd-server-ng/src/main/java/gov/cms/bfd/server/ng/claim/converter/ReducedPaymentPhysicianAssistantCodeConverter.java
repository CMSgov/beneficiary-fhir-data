package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ReducedPaymentPhysicianAssistantCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ReducedPaymentPhysicianAssistantCodeConverter
    implements AttributeConverter<Optional<ReducedPaymentPhysicianAssistantCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ReducedPaymentPhysicianAssistantCode> reducedPaymentPhysicianAssistantCode) {
    return reducedPaymentPhysicianAssistantCode
        .map(ReducedPaymentPhysicianAssistantCode::getCode)
        .orElse("");
  }

  @Override
  public Optional<ReducedPaymentPhysicianAssistantCode> convertToEntityAttribute(String code) {
    return ReducedPaymentPhysicianAssistantCode.tryFromCode(code);
  }
}
