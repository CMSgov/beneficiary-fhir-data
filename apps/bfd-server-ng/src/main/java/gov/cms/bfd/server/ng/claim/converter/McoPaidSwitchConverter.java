package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.McoPaidSwitch;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class McoPaidSwitchConverter implements AttributeConverter<Optional<McoPaidSwitch>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<McoPaidSwitch> mcoPaidSwitch) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return mcoPaidSwitch.map(McoPaidSwitch::getCode).orElse("");
  }

  @Override
  public Optional<McoPaidSwitch> convertToEntityAttribute(String code) {
    return McoPaidSwitch.tryFromCode(code);
  }
}
