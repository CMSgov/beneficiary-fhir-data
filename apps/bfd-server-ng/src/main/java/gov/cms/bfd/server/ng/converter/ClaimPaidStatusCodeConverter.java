package gov.cms.bfd.server.ng.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPaidStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Attribute converter to map {@link ClaimPaidStatusCode} to its database string representation.
 */
@Converter
public class ClaimPaidStatusCodeConverter
    implements AttributeConverter<ClaimPaidStatusCode, String> {

  @Override
  public String convertToDatabaseColumn(ClaimPaidStatusCode attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.getCode();
  }

  @Override
  public ClaimPaidStatusCode convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return ClaimPaidStatusCode.tryFromCode(dbData).orElse(null);
  }
}
