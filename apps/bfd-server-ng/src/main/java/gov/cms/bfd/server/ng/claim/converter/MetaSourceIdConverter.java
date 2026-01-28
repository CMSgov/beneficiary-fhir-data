package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.MetaSourceId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class MetaSourceIdConverter implements AttributeConverter<Optional<MetaSourceId>, Integer> {
  @Override
  public Integer convertToDatabaseColumn(Optional<MetaSourceId> metaSourceId) {
    return metaSourceId.map(MetaSourceId::getSourceId).orElse(0);
  }

  @Override
  public Optional<MetaSourceId> convertToEntityAttribute(Integer s) {
    return MetaSourceId.tryFromSourceId(s);
  }
}
