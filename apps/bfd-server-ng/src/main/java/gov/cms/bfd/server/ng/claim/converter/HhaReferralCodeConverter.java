package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.HhaReferralCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class HhaReferralCodeConverter
    implements AttributeConverter<Optional<HhaReferralCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<HhaReferralCode> hhaReferralCode) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return hhaReferralCode.map(HhaReferralCode::getCode).orElse("");
  }

  @Override
  public Optional<HhaReferralCode> convertToEntityAttribute(String code) {
    return HhaReferralCode.tryFromCode(code);
  }
}
