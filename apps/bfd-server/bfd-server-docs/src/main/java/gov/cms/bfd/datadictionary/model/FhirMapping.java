package gov.cms.bfd.datadictionary.model;

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

  /** String[] of discriminators. */
  private String[] discriminator;

  /** String[] of additional info. */
  private String[] additional;

  /** Derived. */
  private String derived;

  /** Note. */
  private String note;

  /** Example. */
  private String example;
}
