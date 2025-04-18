package gov.cms.bfd.server.ng.beneficiary.converter;

import gov.cms.bfd.server.ng.beneficiary.model.RaceCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converts the race code between the string value in the database and a {@link RaceCode}. */
@Converter(autoApply = true)
public class RaceCodeConverter implements AttributeConverter<RaceCode, String> {
  @Override
  public String convertToDatabaseColumn(RaceCode raceCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return raceCode.getIdrCode();
  }

  @Override
  public RaceCode convertToEntityAttribute(String raceCode) {
    return RaceCode.fromIdrCode(raceCode);
  }
}
