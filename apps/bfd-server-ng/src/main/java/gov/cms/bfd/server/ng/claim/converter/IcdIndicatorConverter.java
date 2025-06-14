package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IcdIndicatorConverter implements AttributeConverter<IcdIndicator, String> {
  @Override
  public String convertToDatabaseColumn(IcdIndicator icdIndicator) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return icdIndicator.getCode();
  }

  @Override
  public IcdIndicator convertToEntityAttribute(String code) {
    return IcdIndicator.fromCode(code);
  }
}
