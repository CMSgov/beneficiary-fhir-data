package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.HealthProfessionalShortageAreaScarcityCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class HealthProfessionalShortageAreaScarcityCodeConverter
    implements AttributeConverter<Optional<HealthProfessionalShortageAreaScarcityCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<HealthProfessionalShortageAreaScarcityCode>
          healthProfessionalShortageAreaScarcityCode) {
    return healthProfessionalShortageAreaScarcityCode
        .map(HealthProfessionalShortageAreaScarcityCode::getCode)
        .orElse("");
  }

  @Override
  public Optional<HealthProfessionalShortageAreaScarcityCode> convertToEntityAttribute(
      String code) {
    return HealthProfessionalShortageAreaScarcityCode.tryFromCode(code);
  }
}
