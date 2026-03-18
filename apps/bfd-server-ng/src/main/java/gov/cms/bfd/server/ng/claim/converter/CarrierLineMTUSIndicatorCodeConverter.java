package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.CarrierLineMTUSIndicatorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class CarrierLineMTUSIndicatorCodeConverter
    implements AttributeConverter<Optional<CarrierLineMTUSIndicatorCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<CarrierLineMTUSIndicatorCode> carrierLineMTUSIndicatorCode) {
    return carrierLineMTUSIndicatorCode.map(CarrierLineMTUSIndicatorCode::getCode).orElse("");
  }

  @Override
  public Optional<CarrierLineMTUSIndicatorCode> convertToEntityAttribute(String code) {
    return CarrierLineMTUSIndicatorCode.fromCode(code);
  }
}
