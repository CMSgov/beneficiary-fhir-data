package gov.cms.bfd.model.metadata.ccw.rif;

import gov.cms.bfd.model.metadata.FieldDefinitionForStruct;
import gov.cms.bfd.model.metadata.FieldDefinitionImpl;
import java.util.Optional;

/** Enumerates the fields in the CCW RIF "CARRIER" layout. */
public enum RifCarrierClaimFields implements FieldDefinitionForStruct {
  CLM_ID("field_definitions/rif_claims_clm_id.yml"),

  BENE_ID("field_definitions/ccw_bene_id.yml"),

  LINE_NUM("field_definitions/rif_claims_line_num.yml");

  private final FieldDefinitionImpl fieldDefinition;

  /**
   * Enum constant constructor.
   *
   * @param fieldDefinitionResourceName the name of the field definition YAML resource to parse
   */
  private RifCarrierClaimFields(String fieldDefinitionResourceName) {
    this.fieldDefinition = FieldDefinitionImpl.parseFromYaml(fieldDefinitionResourceName);
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinitionForStruct#getStructFieldId() */
  @Override
  public String getStructFieldId() {
    return this.name();
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getId() */
  @Override
  public String getId() {
    return fieldDefinition.getId();
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getCommonName() */
  @Override
  public Optional<String> getCommonName() {
    return fieldDefinition.getCommonName();
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getDescriptionAsMarkdown() */
  @Override
  public Optional<String> getDescriptionAsMarkdown() {
    return fieldDefinition.getDescriptionAsMarkdown();
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getDescriptionAsHtml() */
  @Override
  public Optional<String> getDescriptionAsHtml() {
    return fieldDefinition.getDescriptionAsHtml();
  }
}
