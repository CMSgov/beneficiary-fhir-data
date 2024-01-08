package gov.cms.bfd.server.war.adapters.r4;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.DiagnosisComponent;
import gov.cms.bfd.server.war.adapters.FhirResource;
import gov.cms.bfd.server.war.adapters.ItemComponent;
import gov.cms.bfd.server.war.adapters.ProcedureComponent;
import gov.cms.bfd.server.war.adapters.SupportingInfoComponent;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Adapter for creating R4 FHIR resources from an {@link ExplanationOfBenefit}. */
public class ExplanationOfBenefitAdapter implements FhirResource {

  /** The explanation of benefit to build components from. */
  private final ExplanationOfBenefit eob;

  /**
   * Instantiates a new Explanation of benefit adapter.
   *
   * @param eob the explanation of benefit
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

  /** {@inheritDoc} */
  public List<SupportingInfoComponent> getSupportingInfo() {
    return eob.getSupportingInfo().stream()
        .map(SupportingInfoAdapter::new)
        .collect(Collectors.toList());
  }

  /** Adapter for creating R4 FHIR supporting info components. */
  public static class SupportingInfoAdapter implements SupportingInfoComponent {

    /** The eob's supporting information component. */
    private final ExplanationOfBenefit.SupportingInformationComponent supportingInfoComponent;

    /**
     * Instantiates a new SupportingInfo component adapter.
     *
     * @param supportingInfoComponent the supporting info component
     */
    public SupportingInfoAdapter(
        ExplanationOfBenefit.SupportingInformationComponent supportingInfoComponent) {
      this.supportingInfoComponent = supportingInfoComponent;
    }

    /** {@inheritDoc} */
    @Override
    public CodeableConcept getSupportingInfoCodeableConcept() {
      return new CodeableConceptAdapter(supportingInfoComponent.getCode());
    }
  }

  /** Adapter for creating R4 FHIR procedure components. */
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
      return new CodeableConceptAdapter(procedureComponent.getProcedureCodeableConcept());
    }
  }

  /** Adapter for creating R4 FHIR diagnosis components. */
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
      return new CodeableConceptAdapter(diagnosisComponent.getDiagnosisCodeableConcept());
    }

    /** {@inheritDoc} */
    @Override
    public CodeableConcept getPackageCode() {
      return new CodeableConceptAdapter(diagnosisComponent.getPackageCode());
    }
  }

  /** Adapter for creating R4 FHIR item components. */
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
      return new CodeableConceptAdapter(itemComponent.getProductOrService());
    }
  }

  /** Adapter for creating R4 FHIR codeable concepts. */
  public static class CodeableConceptAdapter implements CodeableConcept {

    /** The eob's codeable concept. */
    private final org.hl7.fhir.r4.model.CodeableConcept codeableConcept;

    /**
     * Instantiates a new Codeable concept adapter.
     *
     * @param codeableConcept the codeable concept
     */
    public CodeableConceptAdapter(org.hl7.fhir.r4.model.CodeableConcept codeableConcept) {
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

  /** Adapter for creating R4 FHIR codings. */
  public static class CodingAdapter implements Coding {

    /** The eob's coding. */
    private final org.hl7.fhir.r4.model.Coding coding;

    /**
     * Instantiates a new Coding adapter.
     *
     * @param coding the coding
     */
    public CodingAdapter(org.hl7.fhir.r4.model.Coding coding) {
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
