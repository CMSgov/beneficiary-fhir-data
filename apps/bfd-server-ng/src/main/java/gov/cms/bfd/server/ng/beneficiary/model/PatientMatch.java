package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;
import lombok.Builder;

@Builder
public record PatientMatch(
    PatientMatchEntry firstName,
    PatientMatchEntry lastName,
    PatientMatchEntry addresses,
    PatientMatchEntry ssnLastFourDigits,
    PatientMatchEntry birthDate,
    PatientMatchEntry mbi) {

  public List<List<PatientMatchEntry>> getValidScenarios() {
    var scenarios =
        List.of(
            // #1
            List.of(firstName, lastName, birthDate, addresses),
            // #2 - requires phone
            // #3 - requires email
            // #4
            List.of(firstName, lastName, birthDate, ssnLastFourDigits),
            // #5 - same as #4 but fuzzy (not implemented yet)
            // #6 - requires ITIN
            // #7 - requires ITIN
            // #8
            List.of(firstName, birthDate, mbi)
            // #9 - requires legal ID
            // #10 - requires legal ID
            // #11 - requires phone
            // #12 - requires email
            // #13 - requires phone
            // #14 - requires ITIN
            // #15 - requires email
            // #16 - requires ITIN
            // #17 - requires phone
            // #18 - requires ITIN
            // #19 - requires email
            // #20 - requires ITIN
            // #21 - requires phone
            // #22 - requires legal ID
            // #23 - requires email
            // #24 - requires email
            // #25 - requires legal ID
            // #26 - requires UUID
            // #27 - requires patient ID
            );
    return scenarios.stream().filter(this::allFieldsPresent).toList();
  }

  boolean allFieldsPresent(List<PatientMatchEntry> entries) {
    return entries.stream().noneMatch(e -> e.values().isEmpty());
  }
}
