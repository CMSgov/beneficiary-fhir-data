package gov.cms.model.dsl.codegen.plugin.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Model class for FHIR Elements specifications in a mapping. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FhirElementBean implements ModelBean {

  /** Id. */
  private int id;

  /** Name. */
  private String name;

  /** Description. */
  private String description;

  /** List of String, Applies To. */
  private List<String> appliesTo;

  /** Maps fields from appliesTo to source entity class types. */
  private Map<String, String> sourceEntityClassNames =
      new HashMap<>() {
        {
          put("DME", "gov.cms.bfd.model.rif.entities.DMEClaim");
        }
      };

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
