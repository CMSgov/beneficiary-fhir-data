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

public class ExplanationOfBenefitAdapter implements FhirResource {

  private final ExplanationOfBenefit eob;

  public ExplanationOfBenefitAdapter(ExplanationOfBenefit eob) {
    this.eob = eob;
  }

  @Override
  public List<ProcedureComponent> getProcedure() {
    return eob.getProcedure().stream()
        .map(ProcedureComponentAdapter::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<DiagnosisComponent> getDiagnosis() {
    return eob.getDiagnosis().stream()
        .map(DiagnosisComponentAdapter::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<ItemComponent> getItem() {
    return eob.getItem().stream().map(ItemComponentAdapter::new).collect(Collectors.toList());
  }

  public static class ProcedureComponentAdapter implements ProcedureComponent {

    private final ExplanationOfBenefit.ProcedureComponent procedureComponent;

    public ProcedureComponentAdapter(ExplanationOfBenefit.ProcedureComponent procedureComponent) {
      this.procedureComponent = procedureComponent;
    }

    @Override
    public CodeableConcept getProcedureCodeableConcept() {
      // The STU3 versions seem to have less protections for null items, so we need a null check
      return procedureComponent.getProcedureCodeableConcept() == null
          ? null
          : new CodeableConceptAdapter(procedureComponent.getProcedureCodeableConcept());
    }
  }

  public static class DiagnosisComponentAdapter implements DiagnosisComponent {

    private final ExplanationOfBenefit.DiagnosisComponent diagnosisComponent;

    public DiagnosisComponentAdapter(ExplanationOfBenefit.DiagnosisComponent diagnosisComponent) {
      this.diagnosisComponent = diagnosisComponent;
    }

    @Override
    public CodeableConcept getDiagnosisCodeableConcept() {
      // The STU3 versions seem to have less protections for null items, so we need a null check
      return diagnosisComponent.getDiagnosisCodeableConcept() == null
          ? null
          : new CodeableConceptAdapter(diagnosisComponent.getDiagnosisCodeableConcept());
    }

    @Override
    public CodeableConcept getPackageCode() {
      return new CodeableConceptAdapter(diagnosisComponent.getPackageCode());
    }
  }

  public static class ItemComponentAdapter implements ItemComponent {

    private final ExplanationOfBenefit.ItemComponent itemComponent;

    public ItemComponentAdapter(ExplanationOfBenefit.ItemComponent itemComponent) {
      this.itemComponent = itemComponent;
    }

    @Override
    public CodeableConcept getProductOrService() {
      return new CodeableConceptAdapter(itemComponent.getService());
    }
  }

  public static class CodeableConceptAdapter implements CodeableConcept {

    private final org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept;

    public CodeableConceptAdapter(org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept) {
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

    private final org.hl7.fhir.dstu3.model.Coding coding;

    public CodingAdapter(org.hl7.fhir.dstu3.model.Coding coding) {
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
