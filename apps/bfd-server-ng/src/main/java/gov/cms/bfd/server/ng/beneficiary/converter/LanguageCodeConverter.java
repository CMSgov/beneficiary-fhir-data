package gov.cms.bfd.server.ng.beneficiary.converter;

import gov.cms.bfd.server.ng.beneficiary.model.LanguageCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the language code between the string value in the database and a {@link LanguageCode}.
 */
@Converter(autoApply = true)
public class LanguageCodeConverter implements AttributeConverter<LanguageCode, String> {
  @Override
  public String convertToDatabaseColumn(LanguageCode languageCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return languageCode.getIdrCode();
  }

  @Override
  public LanguageCode convertToEntityAttribute(String languageCode) {
    return LanguageCode.fromIdrCode(languageCode);
  }
}
