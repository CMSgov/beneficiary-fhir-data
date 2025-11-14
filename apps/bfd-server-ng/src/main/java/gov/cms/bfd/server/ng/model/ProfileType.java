package gov.cms.bfd.server.ng.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The type of profile to use. */
@RequiredArgsConstructor
@Getter
public enum ProfileType {
  /** C4BB - Carin Blue Button. */
  C4BB(
      List.of(SystemUrls.PROFILE_C4BB_PATIENT_2_1_0, SystemUrls.PROFILE_US_CORE_PATIENT_6_1_0),
      List.of(SystemUrls.PROFILE_C4BB_COVERAGE_2_1_0, SystemUrls.PROFILE_US_CORE_COVERAGE_6_1_0)),
  /** C4DIC - Carin Digital Insurance Card. */
  C4DIC(
      List.of(SystemUrls.PROFILE_C4DIC_PATIENT_1_1_0),
      List.of(SystemUrls.PROFILE_C4DIC_COVERAGE_1_1_0));

  private final List<String> patientProfiles;
  private final List<String> coverageProfiles;
}
