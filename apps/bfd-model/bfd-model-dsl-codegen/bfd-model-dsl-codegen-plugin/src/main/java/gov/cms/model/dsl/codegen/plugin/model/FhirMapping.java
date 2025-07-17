package gov.cms.model.dsl.codegen.plugin.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Data class for serializing FHIR mappings from data dictionary resource files. */
@Setter
@Getter
public class FhirMapping {
  /** Version. */
  private String version;

  /** Resource. */
  private String resource;

  /** Element. */
  private String element;

  /** FHIR Path. */
  private String fhirPath;

  /** List of String, of discriminators. */
  private List<String> discriminator;

  /** List of String, of additional info. */
  private List<String> additional;

  /** Derived. */
  private String derived;

  /** Note. */
  private String note;

  /** Example. */
  private String example;
}
