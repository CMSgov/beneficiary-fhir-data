package gov.cms.bfd.server.ng.converter;

import gov.cms.bfd.server.ng.claim.model.common.ClaimPaidStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/**
 * JPA Attribute converter to map optional {@link ClaimPaidStatusCode} to its database string
 * representation.
 */
@Converter
public class ClaimPaidStatusCodeConverter
    implements AttributeConverter<Optional<ClaimPaidStatusCode>, String> {

  @Override
  public String convertToDatabaseColumn(Optional<ClaimPaidStatusCode> attribute) {
    return attribute.map(ClaimPaidStatusCode::getCode).orElse(null);
  }

  @Override
  public Optional<ClaimPaidStatusCode> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return Optional.empty();
    }
    return ClaimPaidStatusCode.tryFromCode(dbData);
  }
}
