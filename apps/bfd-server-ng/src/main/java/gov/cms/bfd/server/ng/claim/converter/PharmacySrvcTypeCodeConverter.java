package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.PharmacySrvcTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class PharmacySrvcTypeCodeConverter
    implements AttributeConverter<Optional<PharmacySrvcTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<PharmacySrvcTypeCode> pharmacySrvcTypeCode) {
    return pharmacySrvcTypeCode.map(PharmacySrvcTypeCode::getCode).orElse("");
  }

  @Override
  public Optional<PharmacySrvcTypeCode> convertToEntityAttribute(String code) {
    return PharmacySrvcTypeCode.tryFromCode(code);
  }
}
