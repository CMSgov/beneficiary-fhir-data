package gov.cms.model.dsl.codegen.plugin.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Data class for serializing data dictionary FHIR element resource files. */
@Setter
@Getter
@Builder
public class FhirElement {
  /** Id. */
  private int id;

  /** Name. */
  private String name;

  /** Description. */
  private String description;

  /** List of String, Applies To. */
  private List<String> appliesTo;

  /** List of String, Supplied In. */
  private List<String> suppliedIn;

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

  /** List of String, of CCW Mappings. */
  private List<String> ccwMapping;

  /** List of String, of CCLF Mappings. */
  private List<String> cclfMapping;

  /** List of FHIR Mappings. */
  private List<FhirMapping> fhirMapping;
}
