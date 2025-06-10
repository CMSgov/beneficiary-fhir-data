package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import gov.cms.bfd.server.ng.claim.model.McoPaidSwitch;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class McoPaidSwitchConverter implements AttributeConverter<McoPaidSwitch, String> {
  @Override
  public String convertToDatabaseColumn(McoPaidSwitch mcoPaidSwitch) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return mcoPaidSwitch.getCode();
  }

  @Override
  public McoPaidSwitch convertToEntityAttribute(String code) {
    return McoPaidSwitch.fromCode(code);
  }
}
