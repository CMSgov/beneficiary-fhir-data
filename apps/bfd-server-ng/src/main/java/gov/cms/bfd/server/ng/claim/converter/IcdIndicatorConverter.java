package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class IcdIndicatorConverter implements AttributeConverter<Optional<IcdIndicator>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<IcdIndicator> icdIndicator) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return icdIndicator.map(IcdIndicator::getCode).orElse("");
  }

  @Override
  public Optional<IcdIndicator> convertToEntityAttribute(String code) {
    return IcdIndicator.tryFromCode(code);
  }
}
