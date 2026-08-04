package gov.cms.bfd.server.ng.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimPaidStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Attribute converter to map optional {@link ClaimPaidStatusCode} to its database string
 * representation.
 */
@Converter
public class ClaimPaidStatusCodeConverter
    implements AttributeConverter<ClaimPaidStatusCode, String> {

  @Override
  public String convertToDatabaseColumn(ClaimPaidStatusCode claimPaidStatusCode) {
    return claimPaidStatusCode.getCode();
  }

  @Override
  public ClaimPaidStatusCode convertToEntityAttribute(String dbData) {
    return ClaimPaidStatusCode.tryFromCode(dbData);
  }
}
