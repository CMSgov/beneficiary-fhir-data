package gov.cms.bfd.server.ng.coverage.converter;

import gov.cms.bfd.server.ng.coverage.model.EnrollmentProgramTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Converts an enrollment program type code to and from the database representation. */
@Converter(autoApply = true)
public class EnrollmentProgramTypeCodeConverter
    implements AttributeConverter<Optional<EnrollmentProgramTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<EnrollmentProgramTypeCode> code) {
    return code.map(EnrollmentProgramTypeCode::getCode).orElse("");
  }

  @Override
  public Optional<EnrollmentProgramTypeCode> convertToEntityAttribute(String code) {
    return EnrollmentProgramTypeCode.tryFromCode(code);
  }
}
