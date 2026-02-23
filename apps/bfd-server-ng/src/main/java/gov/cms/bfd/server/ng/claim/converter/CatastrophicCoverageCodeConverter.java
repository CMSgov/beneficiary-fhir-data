package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.CatastrophicCoverageCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class CatastrophicCoverageCodeConverter
    implements AttributeConverter<Optional<CatastrophicCoverageCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<CatastrophicCoverageCode> catastrophicCoverageCode) {
    return catastrophicCoverageCode.map(CatastrophicCoverageCode::getCode).orElse("");
  }

  @Override
  public Optional<CatastrophicCoverageCode> convertToEntityAttribute(String code) {
    return CatastrophicCoverageCode.tryFromCode(code);
  }
}
