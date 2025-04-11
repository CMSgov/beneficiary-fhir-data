package gov.cms.bfd.server.ng.beneficiary.converter;

import gov.cms.bfd.server.ng.beneficiary.model.LanguageCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LanguageCodeConverter implements AttributeConverter<LanguageCode, String> {
  @Override
  public String convertToDatabaseColumn(LanguageCode languageCode) {
    return languageCode.toString();
  }

  @Override
  public LanguageCode convertToEntityAttribute(String languageCode) {
    return LanguageCode.fromIdrCode(languageCode);
  }
}
