package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class MetaSourceIdConverter implements AttributeConverter<Optional<MetaSourceSk>, Integer> {
  @Override
  public Integer convertToDatabaseColumn(Optional<MetaSourceSk> metaSourceId) {
    return metaSourceId.map(MetaSourceSk::getSourceSk).orElse(0);
  }

  @Override
  public Optional<MetaSourceSk> convertToEntityAttribute(Integer s) {
    return MetaSourceSk.tryFromSourceSk(s);
  }
}
