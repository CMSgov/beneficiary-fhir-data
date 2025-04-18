package gov.cms.bfd.server.ng.beneficiary.converter;

import gov.cms.bfd.server.ng.beneficiary.model.SexCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

@Converter(autoApply = true)
public class SexCodeConverter implements AttributeConverter<Optional<SexCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<SexCode> sexCode) {
    return sexCode.map(Enum::toString).orElse("");
  }

  @Override
  public Optional<SexCode> convertToEntityAttribute(String raceCode) {
    return SexCode.tryFromIdrCode(raceCode);
  }
}
