package gov.cms.bfd.server.war.adapters.r4;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.DiagnosisComponent;
import gov.cms.bfd.server.war.adapters.FhirResource;
import gov.cms.bfd.server.war.adapters.ItemComponent;
import gov.cms.bfd.server.war.adapters.ProcedureComponent;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Claim;

public class ClaimAdapter implements FhirResource {

  private final Claim claim;

  public ClaimAdapter(Claim claim) {
    this.claim = claim;
  }

  @Override
  public List<ProcedureComponent> getProcedure() {
    return claim.getProcedure().stream()
        .map(ProcedureComponentAdapter::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<DiagnosisComponent> getDiagnosis() {
    return claim.getDiagnosis().stream()
        .map(DiagnosisComponentAdapter::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<ItemComponent> getItem() {
    return claim.getItem().stream().map(ItemComponentAdapter::new).collect(Collectors.toList());
  }

  public static class ProcedureComponentAdapter implements ProcedureComponent {

    private final Claim.ProcedureComponent procedureComponent;

    public ProcedureComponentAdapter(Claim.ProcedureComponent procedureComponent) {
      this.procedureComponent = procedureComponent;
    }

    @Override
    public CodeableConcept getProcedureCodeableConcept() {
      return new CodeableConceptAdapter(procedureComponent.getProcedureCodeableConcept());
    }
  }

  public static class DiagnosisComponentAdapter implements DiagnosisComponent {

    private final Claim.DiagnosisComponent diagnosisComponent;

    public DiagnosisComponentAdapter(Claim.DiagnosisComponent diagnosisComponent) {
      this.diagnosisComponent = diagnosisComponent;
    }

    @Override
    public CodeableConcept getDiagnosisCodeableConcept() {
      return new CodeableConceptAdapter(diagnosisComponent.getDiagnosisCodeableConcept());
    }

    @Override
    public CodeableConcept getPackageCode() {
      return new CodeableConceptAdapter(diagnosisComponent.getPackageCode());
    }
  }

  public static class ItemComponentAdapter implements ItemComponent {

    private final Claim.ItemComponent itemComponent;

    public ItemComponentAdapter(Claim.ItemComponent itemComponent) {
      this.itemComponent = itemComponent;
    }

    @Override
    public CodeableConcept getProductOrService() {
      return new CodeableConceptAdapter(itemComponent.getProductOrService());
    }
  }

  public static class CodeableConceptAdapter implements CodeableConcept {

    private final org.hl7.fhir.r4.model.CodeableConcept codeableConcept;

    public CodeableConceptAdapter(org.hl7.fhir.r4.model.CodeableConcept codeableConcept) {
      this.codeableConcept = codeableConcept;
    }

    @Override
    public List<Coding> getCoding() {
      return codeableConcept.getCoding().stream()
          .map(CodingAdapter::new)
          .collect(Collectors.toList());
    }
  }

  public static class CodingAdapter implements Coding {

    private final org.hl7.fhir.r4.model.Coding coding;

    public CodingAdapter(org.hl7.fhir.r4.model.Coding coding) {
      this.coding = coding;
    }

    @Override
    public String getSystem() {
      return coding.getSystem();
    }

    @Override
    public String getCode() {
      return coding.getCode();
    }
  }
}
