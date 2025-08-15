package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.IdrConstants;
import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Medicare Status Code Type. */
@RequiredArgsConstructor
@Getter
public enum MedicareStatusCode {
  /** Medicare Status Code 0. */
  CODE_0("0", IdrConstants.UNKNOWN, IdrConstants.UNKNOWN),
  /** Medicare Status Code 00. */
  CODE_00("00", IdrConstants.UNKNOWN, IdrConstants.UNKNOWN),
  /** Medicare Status Code 10. */
  CODE_10("10", IdrConstants.NO, IdrConstants.NO),
  /** Medicare Status Code 11. */
  CODE_11("11", IdrConstants.YES, IdrConstants.NO),
  /** Medicare Status Code. 20 */
  CODE_20("20", IdrConstants.NO, IdrConstants.YES),
  /** Medicare Status Code 21. */
  CODE_21("21", IdrConstants.YES, IdrConstants.YES),
  /** Medicare Status Code 31. */
  CODE_31("31", IdrConstants.YES, IdrConstants.NO),
  /** Medicare Status Code 40. */
  CODE_40("40", IdrConstants.NO, IdrConstants.NO);

  private final String code;

  private final String esrdIndicator;
  private final String disabilityIndicator;

  /**
   * Finds a {@link MedicareStatusCode} enum constant by its BENE_MDCR_STUS_CD code.
   *
   * @param beneMdcrStusCd The BENE_MDCR_STUS_CD code from the database.
   * @return An {@link Optional} containing the matching {@link MedicareStatusCode}, or {@link
   *     Optional#empty()} if no match is found.
   */
  public static Optional<MedicareStatusCode> tryFromCode(String beneMdcrStusCd) {
    if (beneMdcrStusCd == null || beneMdcrStusCd.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(MedicareStatusCode.values())
        .filter(type -> type.getCode().equals(beneMdcrStusCd))
        .findFirst();
  }

  List<Extension> toFhir() {
    return List.of(
        new Extension(SystemUrls.EXT_BENE_MDCR_STUS_CD_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_MDCR_STUS_CD, code, null)),
        new Extension(SystemUrls.EXT_BENE_ESRD_STUS_ID_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_ESRD_STUS_ID, esrdIndicator, null)),
        new Extension(SystemUrls.EXT_BENE_DSBLD_STUS_ID_URL)
            .setValue(new Coding(SystemUrls.SYS_BENE_DSBLD_STUS_ID, disabilityIndicator, null)));
  }
}
