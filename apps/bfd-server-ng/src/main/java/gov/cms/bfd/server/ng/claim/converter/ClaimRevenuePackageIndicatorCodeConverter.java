package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.institutional.ClaimRevenuePackageIndicatorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimRevenuePackageIndicatorCodeConverter
    implements AttributeConverter<Optional<ClaimRevenuePackageIndicatorCode>, String> {
  @Override
  public String convertToDatabaseColumn(
      Optional<ClaimRevenuePackageIndicatorCode> carrierLineMTUSIndicatorCode) {
    return carrierLineMTUSIndicatorCode.map(ClaimRevenuePackageIndicatorCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimRevenuePackageIndicatorCode> convertToEntityAttribute(String code) {
    return ClaimRevenuePackageIndicatorCode.tryFromCode(code);
  }
}
