package gov.cms.bfd.server.war.adapters.stu3;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.DiagnosisComponent;
import gov.cms.bfd.server.war.adapters.FhirResource;
import gov.cms.bfd.server.war.adapters.ItemComponent;
import gov.cms.bfd.server.war.adapters.ProcedureComponent;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

/**
 * Adapter for creating stu3 FHIR resources from an {@link
 * org.hl7.fhir.r4.model.ExplanationOfBenefit}.
 */
public class ExplanationOfBenefitAdapter implements FhirResource {

  /** The explanation of benefit to build components from. */
  private final ExplanationOfBenefit eob;

  /**
   * Instantiates a new Explanation of benefit adapter.
   *
   * @param eob the eob
   */
  public ExplanationOfBenefitAdapter(ExplanationOfBenefit eob) {
    this.eob = eob;
  }

  /** {@inheritDoc} */
  @Override
  public List<ProcedureComponent> getProcedure() {
    return eob.getProcedure().stream()
        .map(ProcedureComponentAdapter::new)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<DiagnosisComponent> getDiagnosis() {
    return eob.getDiagnosis().stream()
        .map(DiagnosisComponentAdapter::new)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<ItemComponent> getItem() {
    return eob.getItem().stream().map(ItemComponentAdapter::new).collect(Collectors.toList());
  }

  /** Adapter for creating stu3 FHIR procedure components. */
  public static class ProcedureComponentAdapter implements ProcedureComponent {

    /** The eob's procedure component. */
    private final ExplanationOfBenefit.ProcedureComponent procedureComponent;

    /**
     * Instantiates a new Procedure component adapter.
     *
     * @param procedureComponent the procedure component
     */
    public ProcedureComponentAdapter(ExplanationOfBenefit.ProcedureComponent procedureComponent) {
      this.procedureComponent = procedureComponent;
    }

    /** {@inheritDoc} */
    @Override
    public CodeableConcept getProcedureCodeableConcept() {
      // The STU3 versions seem to have less protections for null items, so we need a null check
      return procedureComponent.getProcedureCodeableConcept() == null
          ? null
          : new CodeableConceptAdapter(procedureComponent.getProcedureCodeableConcept());
    }
  }

  /** Adapter for creating stu3 FHIR diagnosis components. */
  public static class DiagnosisComponentAdapter implements DiagnosisComponent {

    /** The eob's diagnosis component. */
    private final ExplanationOfBenefit.DiagnosisComponent diagnosisComponent;

    /**
     * Instantiates a new Diagnosis component adapter.
     *
     * @param diagnosisComponent the diagnosis component
     */
    public DiagnosisComponentAdapter(ExplanationOfBenefit.DiagnosisComponent diagnosisComponent) {
      this.diagnosisComponent = diagnosisComponent;
    }

    /** {@inheritDoc} */
    @Override
    public CodeableConcept getDiagnosisCodeableConcept() {
      // The STU3 versions seem to have less protections for null items, so we need a null check
      return diagnosisComponent.getDiagnosisCodeableConcept() == null
          ? null
          : new CodeableConceptAdapter(diagnosisComponent.getDiagnosisCodeableConcept());
    }

    /** {@inheritDoc} */
    @Override
    public CodeableConcept getPackageCode() {
      return new CodeableConceptAdapter(diagnosisComponent.getPackageCode());
    }
  }

  /** Adapter for creating stu3 FHIR item components. */
  public static class ItemComponentAdapter implements ItemComponent {

    /** The eob's item component. */
    private final ExplanationOfBenefit.ItemComponent itemComponent;

    /**
     * Instantiates a new Item component adapter.
     *
     * @param itemComponent the item component
     */
    public ItemComponentAdapter(ExplanationOfBenefit.ItemComponent itemComponent) {
      this.itemComponent = itemComponent;
    }

    /** {@inheritDoc} */
    @Override
    public CodeableConcept getProductOrService() {
      return new CodeableConceptAdapter(itemComponent.getService());
    }
  }

  /** Adapter for creating stu3 FHIR codeable concepts. */
  public static class CodeableConceptAdapter implements CodeableConcept {

    /** The eob's codeable concept. */
    private final org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept;

    /**
     * Instantiates a new Codeable concept adapter.
     *
     * @param codeableConcept the codeable concept
     */
    public CodeableConceptAdapter(org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept) {
      this.codeableConcept = codeableConcept;
    }

    /** {@inheritDoc} */
    @Override
    public List<Coding> getCoding() {
      return codeableConcept.getCoding().stream()
          .map(CodingAdapter::new)
          .collect(Collectors.toList());
    }
  }

  /** Adapter for creating stu3 FHIR codings. */
  public static class CodingAdapter implements Coding {

    /** The eob's coding. */
    private final org.hl7.fhir.dstu3.model.Coding coding;

    /**
     * Instantiates a new Coding adapter.
     *
     * @param coding the coding
     */
    public CodingAdapter(org.hl7.fhir.dstu3.model.Coding coding) {
      this.coding = coding;
    }

    /** {@inheritDoc} */
    @Override
    public String getSystem() {
      return coding.getSystem();
    }

    /** {@inheritDoc} */
    @Override
    public String getCode() {
      return coding.getCode();
    }
  }
}
