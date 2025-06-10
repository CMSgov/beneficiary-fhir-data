package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.McoPaidSwitch;
import gov.cms.bfd.server.ng.claim.model.PatientStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PatientStatusCodeConverter implements AttributeConverter<PatientStatusCode, String> {
  @Override
  public String convertToDatabaseColumn(PatientStatusCode patientStatusCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return patientStatusCode.getCode();
  }

  @Override
  public PatientStatusCode convertToEntityAttribute(String code) {
    return PatientStatusCode.fromCode(code);
  }
}
