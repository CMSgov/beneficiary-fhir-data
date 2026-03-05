package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ProviderSpecialtyCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ProviderSpecialtyCodeConverter
    implements AttributeConverter<Optional<ProviderSpecialtyCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ProviderSpecialtyCode> providerSpecialtyCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return providerSpecialtyCode.map(ProviderSpecialtyCode::getCode).orElse("");
  }

  @Override
  public Optional<ProviderSpecialtyCode> convertToEntityAttribute(String code) {
    return ProviderSpecialtyCode.fromCodeOptional(code);
  }
}
