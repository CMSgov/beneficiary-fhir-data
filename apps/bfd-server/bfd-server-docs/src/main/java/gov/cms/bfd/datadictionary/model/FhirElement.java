package gov.cms.bfd.datadictionary.model;

import lombok.Getter;
import lombok.Setter;

/** Data class for serializing data dictionary FHIR element resource files. */
@Setter
@Getter
public class FhirElement {
  /** Id. */
  private int id;

  /** Name. */
  private String name;

  /** Description. */
  private String description;

  /** String[] Applies To. */
  private String[] appliesTo;

  /** String[] Supplied In. */
  private String[] suppliedIn;

  /** BFD Database Table Type. */
  private String bfdTableType;

  /** BFD Database Column Name. */
  private String bfdColumnName;

  /** BFD Database Type. */
  private String bfdDbType;

  /** BFD Database Column Size. */
  private Integer bfdDbSize;

  /** BFD Java Field Name. */
  private String bfdJavaFieldName;

  /** String[] of CCW Mappings. */
  private String[] ccwMapping;

  /** String[] of CCLF Mappings. */
  private String[] cclfMapping;

  /** Array of FHIR Mappings. */
  private FhirMapping[] fhirMapping;
}
