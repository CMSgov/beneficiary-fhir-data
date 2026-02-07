package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Database code converter. */
@Converter(autoApply = true)
public class MetaSourceSkConverter implements AttributeConverter<MetaSourceSk, Integer> {
  @Override
  public Integer convertToDatabaseColumn(MetaSourceSk metaSourceSk) {
    if (metaSourceSk == null) {
      return null;
    }
    return metaSourceSk.getSourceSk();
  }

  @Override
  public MetaSourceSk convertToEntityAttribute(Integer s) {
    return MetaSourceSk.tryFromSourceSk(s);
  }
}
