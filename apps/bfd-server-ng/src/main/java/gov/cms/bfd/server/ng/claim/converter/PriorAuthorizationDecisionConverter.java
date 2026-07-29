package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.PriorAuthorizationDecision;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class PriorAuthorizationDecisionConverter
    implements AttributeConverter<Optional<PriorAuthorizationDecision>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<PriorAuthorizationDecision> priorAuthorizationDecision) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return priorAuthorizationDecision.map(PriorAuthorizationDecision::getCode).orElse("");
  }

  @Override
  public Optional<PriorAuthorizationDecision> convertToEntityAttribute(String code) {
    return PriorAuthorizationDecision.tryFromCode(code);
  }
}
