package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;
import lombok.Builder;

@Builder
public record PatientMatch(
    PatientMatchEntry firstName,
    PatientMatchEntry lastName,
    PatientMatchEntry address,
    PatientMatchEntry zipCode,
    PatientMatchEntry ssnLastFourDigits,
    PatientMatchEntry birthDate,
    PatientMatchEntry mbi) {

  public List<List<PatientMatchEntry>> getValidScenarios() {
    var scenarios =
        List.of(
            // #1
            List.of(firstName, lastName, birthDate, address, zipCode),
            // #2 - requires phone
            // #3 - requires email
            // #4
            List.of(firstName, lastName, birthDate, ssnLastFourDigits),
            // #5
            List.of(firstName, lastName, birthDate, ssnLastFourDigits),
            // #6 - requires ITIN
            // #7 - requires ITIN
            // #8
            List.of(firstName, birthDate, mbi));
    return scenarios.stream().filter(this::allFieldsPresent).toList();
  }

  boolean allFieldsPresent(List<PatientMatchEntry> entries) {
    return entries.stream().allMatch(e -> e.value().isPresent());
  }
}
