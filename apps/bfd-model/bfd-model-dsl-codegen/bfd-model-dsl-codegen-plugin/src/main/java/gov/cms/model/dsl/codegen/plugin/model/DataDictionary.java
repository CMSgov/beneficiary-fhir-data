package gov.cms.model.dsl.codegen.plugin.model;

import gov.cms.model.dsl.codegen.plugin.util.Version;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Class for Data Dictionary. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataDictionary {
  /** BFD API Version. */
  private Version version;

  /** Mapped FHIR elements for a Patient resource. */
  private List<FhirElementBean> patientFhirElements;

  /** Mapped FHIR elements for a Coverage resource. */
  private List<FhirElementBean> coverageFhirElements;

  /** Mapped FHIR elements for an ExplanationOfBenefit resource. */
  private List<FhirElementBean> ExplanationOfBenefitFhirElements;

  /** Mapped FHIR elements for all resource. */
  private List<FhirElementBean> allFhirElements;

  /**
   * FHIR elements grouped by FHIR resources.
   *
   * @param fhirElementBeans FHIR elements
   * @return map of grouped FHIR elements by FHIR resource
   */
  public Map<String, List<FhirElementBean>> groupFhirElementsByResource(
      List<FhirElementBean> fhirElementBeans) {
    return fhirElementBeans.stream()
        .filter(element -> !element.getFhirMapping().isEmpty())
        .collect(
            Collectors.groupingBy(element -> element.getFhirMapping().getFirst().getResource()));
  }
}
