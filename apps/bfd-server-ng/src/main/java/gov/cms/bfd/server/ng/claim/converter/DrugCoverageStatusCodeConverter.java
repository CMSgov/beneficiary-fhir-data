package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.DrugCoverageStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class DrugCoverageStatusCodeConverter
    implements AttributeConverter<Optional<DrugCoverageStatusCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<DrugCoverageStatusCode> drugCoverageStatusCode) {
    return drugCoverageStatusCode.map(DrugCoverageStatusCode::getCode).orElse("");
  }

  @Override
  public Optional<DrugCoverageStatusCode> convertToEntityAttribute(String code) {
    return DrugCoverageStatusCode.tryFromCode(code);
  }
}
