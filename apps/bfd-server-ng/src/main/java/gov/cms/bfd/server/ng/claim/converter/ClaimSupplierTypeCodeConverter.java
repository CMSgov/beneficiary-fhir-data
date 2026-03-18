package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimSupplierTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimSupplierTypeCodeConverter
    implements AttributeConverter<Optional<ClaimSupplierTypeCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<ClaimSupplierTypeCode> claimSupplierTypeCode) {
    return claimSupplierTypeCode.map(ClaimSupplierTypeCode::getCode).orElse("");
  }

  @Override
  public Optional<ClaimSupplierTypeCode> convertToEntityAttribute(String code) {
    return ClaimSupplierTypeCode.fromCode(code);
  }
}
