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
import org.hl7.fhir.r4.model.Claim;

/** Adapter creating R4 FHIR components and other objects from a {@link Claim}. */
public class ClaimAdapter implements FhirResource {

  /** The claim to generate components from. */
  private final Claim claim;

  /**
   * Instantiates a new Claim adapter.
   *
   * @param claim the claim
   */
  public ClaimAdapter(Claim claim) {
    this.claim = claim;
  }

  /** {@inheritDoc} */
  public List<SupportingInfoComponent> getSupportingInfo() {
    return claim.getSupportingInfo().stream()
        .map(ClaimAdapter.SupportingInfoAdapter::new)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<ProcedureComponent> getProcedure() {
    return claim.getProcedure().stream()
        .map(ProcedureComponentAdapter::new)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<DiagnosisComponent> getDiagnosis() {
    return claim.getDiagnosis().stream()
        .map(DiagnosisComponentAdapter::new)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<ItemComponent> getItem() {
    return claim.getItem().stream().map(ItemComponentAdapter::new).collect(Collectors.toList());
  }

  /** Adapter for creating R4 FHIR supporting info components. */
  public static class SupportingInfoAdapter implements SupportingInfoComponent {

    /** The eob's supporting information component. */
    private final Claim.SupportingInformationComponent supportingInfoComponent;

    /**
     * Instantiates a new SupportingInfo component adapter.
     *
     * @param supportingInfoComponent the supporting info component
     */
    public SupportingInfoAdapter(Claim.SupportingInformationComponent supportingInfoComponent) {
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

    /** The claim's procedure component. */
    private final Claim.ProcedureComponent procedureComponent;

    /**
     * Instantiates a new Procedure component adapter.
     *
     * @param procedureComponent the procedure component
     */
    public ProcedureComponentAdapter(Claim.ProcedureComponent procedureComponent) {
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

    /** The claim's diagnosis component. */
    private final Claim.DiagnosisComponent diagnosisComponent;

    /**
     * Instantiates a new Diagnosis component adapter.
     *
     * @param diagnosisComponent the diagnosis component
     */
    public DiagnosisComponentAdapter(Claim.DiagnosisComponent diagnosisComponent) {
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

    /** The claim's item component. */
    private final Claim.ItemComponent itemComponent;

    /**
     * Instantiates a new Item component adapter.
     *
     * @param itemComponent the item component
     */
    public ItemComponentAdapter(Claim.ItemComponent itemComponent) {
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

    /** The claim's codeable concept. */
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

    /** The claim's coding. */
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
