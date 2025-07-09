package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.PatientStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class PatientStatusCodeConverter
    implements AttributeConverter<Optional<PatientStatusCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<PatientStatusCode> patientStatusCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return patientStatusCode.map(PatientStatusCode::getCode).orElse("");
  }

  @Override
  public Optional<PatientStatusCode> convertToEntityAttribute(String code) {
    return PatientStatusCode.tryFromCode(code);
  }
}
