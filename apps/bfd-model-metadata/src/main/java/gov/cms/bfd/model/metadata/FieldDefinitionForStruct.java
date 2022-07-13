package gov.cms.bfd.model.metadata;

/** Models a {@link FieldDefinition} for a specific {@link Struct}. */
public interface FieldDefinitionForStruct extends FieldDefinition {
  /** @return the value to use for {@link StructField#XXX} */
  String getStructFieldId();
}
