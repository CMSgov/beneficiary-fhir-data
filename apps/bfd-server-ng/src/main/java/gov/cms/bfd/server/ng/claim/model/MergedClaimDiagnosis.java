package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Diagnosis record consolidated from multiple source records with the same ICD code. */
@RequiredArgsConstructor
@Getter
public class MergedClaimDiagnosis {
  private final int sequenceNumber;
  private IcdIndicator icdIndicator;
  private final Set<ClaimDiagnosisType> diagnosisTypes = new HashSet<>();
  private final Set<String> claimPoaIndicators = new HashSet<>();
  private String diagnosisCode = "";

  static List<MergedClaimDiagnosis> fromProcedures(Stream<ClaimProcedure> procedures) {
    // Find duplicates by grouping records according to their ICD code
    var groupedProcedures =
        procedures
            .filter(p -> p.getDiagnosisKey().isPresent())
            .collect(Collectors.groupingBy(p -> p.getDiagnosisKey().get()));

    var sequenceGenerator = new SequenceGenerator();

    // Sort by diagnosis code for consistent output ordering
    var sortedGroups =
        groupedProcedures.values().stream()
            .sorted((Comparator.comparing(c -> c.getFirst().getDiagnosisCode().get())));
    var mergedDiagnoses =
        sortedGroups.map(c -> MergedClaimDiagnosis.createFromGroup(sequenceGenerator, c));

    return mergedDiagnoses.toList();
  }

  private static MergedClaimDiagnosis createFromGroup(
      SequenceGenerator sequenceGenerator, List<ClaimProcedure> diagnosisGroup) {
    var mergedDiagnoses = new MergedClaimDiagnosis(sequenceGenerator.next());
    var allDiagnosisTypes =
        diagnosisGroup.stream().map(p -> p.getDiagnosisType().get()).collect(Collectors.toSet());
    for (var procedure : diagnosisGroup) {
      mergedDiagnoses.icdIndicator = procedure.getIcdIndicator().get();
      mergedDiagnoses.diagnosisCode = procedure.getDiagnosisCode().get();
      var diagnosisType = procedure.getDiagnosisType().get();

      if (diagnosisType.shouldAdd(allDiagnosisTypes)) {
        mergedDiagnoses.diagnosisTypes.add(diagnosisType);
      }
      procedure.getClaimPoaIndicator().ifPresent(mergedDiagnoses.claimPoaIndicators::add);
    }
    return mergedDiagnoses;
  }

  Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis(ClaimContext claimContext) {
    var diagnosis = new ExplanationOfBenefit.DiagnosisComponent();
    diagnosis.setSequence(sequenceNumber);

    diagnosisTypes.stream()
        .sorted(Comparator.comparing(d -> d.getFhirCode(claimContext)))
        .forEach(
            d ->
                diagnosis.addType(
                    new CodeableConcept(
                        new Coding()
                            .setSystem(d.getSystem())
                            .setCode(d.getFhirCode(claimContext)))));

    var formattedCode = icdIndicator.formatDiagnosisCode(diagnosisCode);
    diagnosis.setDiagnosis(
        new CodeableConcept(
            new Coding().setSystem(icdIndicator.getDiagnosisSystem()).setCode(formattedCode)));

    var onAdmissionConcept = new CodeableConcept();
    claimPoaIndicators.forEach(
        p -> onAdmissionConcept.addCoding().setSystem(SystemUrls.POA_CODING).setCode(p));
    diagnosis.setOnAdmission(onAdmissionConcept);

    return Optional.of(diagnosis);
  }
}
