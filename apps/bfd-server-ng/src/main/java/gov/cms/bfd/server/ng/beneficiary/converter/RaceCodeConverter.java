package gov.cms.bfd.server.ng.beneficiary.converter;

import gov.cms.bfd.server.ng.beneficiary.model.RaceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RaceCodeConverter implements AttributeConverter<RaceCode, String> {
  @Override
  public String convertToDatabaseColumn(RaceCode raceCode) {
    return raceCode.toString();
  }

  @Override
  public RaceCode convertToEntityAttribute(String raceCode) {
    return RaceCode.fromIdrCode(raceCode);
  }
}
