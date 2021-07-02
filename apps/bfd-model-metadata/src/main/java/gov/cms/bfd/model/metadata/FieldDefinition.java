package gov.cms.bfd.model.metadata;

import java.util.Optional;

/**
 * Models a standalone field definition, which can be "mixed in" to flesh out the details of a
 * {@link StructField}.
 */
public interface FieldDefinition {
  /** @return the ID for this {@link FieldDefinition}, which must be globally unique */
  String getId();

  /**
   * @return the "common name" for this {@link FieldDefinition}, which is the name that is typically
   *     used in layouts, schemas, etc. for this field
   */
  Optional<String> getCommonName();

  /**
   * @return the detailed description for this {@link FieldDefinition}, formatted as Markdown, which
   *     explains how the data in this field is formatted and what it means
   */
  Optional<String> getDescriptionAsMarkdown();

  /**
   * @return the detailed description for this {@link FieldDefinition}, formatted as HTML, which
   *     explains how the data in this field is formatted and what it means
   */
  Optional<String> getDescriptionAsHtml();
}
