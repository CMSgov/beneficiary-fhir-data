package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ProviderIdQualifierCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ProviderIdQualifierCodeConverter
    implements AttributeConverter<Optional<ProviderIdQualifierCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ProviderIdQualifierCode> providerIdQualifierCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return providerIdQualifierCode.map(ProviderIdQualifierCode::getCode).orElse("");
  }

  @Override
  public Optional<ProviderIdQualifierCode> convertToEntityAttribute(String code) {
    return ProviderIdQualifierCode.fromCodeOptional(code);
  }
}
