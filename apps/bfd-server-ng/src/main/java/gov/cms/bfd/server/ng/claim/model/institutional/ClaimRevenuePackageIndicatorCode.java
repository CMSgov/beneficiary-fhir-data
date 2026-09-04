package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** The "Revenue Package Indicator Code" for a claim. */
public class ClaimRevenuePackageIndicatorCode {
  @Column(name = "clm_rev_packg_ind_cd")
  private Optional<String> revenuePackageIndicatorCode;

  Optional<Extension> toFhir() {
    return revenuePackageIndicatorCode.map(
        s ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_REVENUE_PACKAGE_INDICATOR_CODE)
                .setValue(
                    new Coding()
                        .setSystem(
                            SystemUrls.BLUE_BUTTON_CODE_SYSTEM_REVENUE_PACKAGE_INDICATOR_CODE)
                        .setCode(s)
                        .setDisplay(ValidCodes.getByCode(s).getDisplay())));
  }

  private enum ValidCodes {
    NOT_PACKAGE("NOT PACKAGED", "0"),
    PACKAGED("PACKAGED SERVICE (SERVICE INDICATOR N)", "1"),
    ARTIFICIAL("ARTIFICIAL CHARGES FOR SURGICAL PROCEDURE (EFF. 7/2004)", "2"),
    PARTIAL(
        "PACKAGED AS PART OF PARTIAL HOSPITALIZATION PER DIEM OR DAILY MENTAL HEALTH SERVICE PER DIEM",
        "3"),
    DRUG_ADMIN("Drug Admin", "4"),
    FQHC("Federally Qualified Health Centers (FQHC) DIEM", "5"),
    NOCOIN("FQHC NOCOIN", "6"),
    UNKNOWN("", "");

    private final String display;
    private final String code;

    ValidCodes(String display, String code) {
      this.display = display;
      this.code = code;
    }

    String getDisplay() {
      return display;
    }

    static ValidCodes getByCode(String code) {
      for (ValidCodes validCode : ValidCodes.values()) {
        if (validCode.code.equals(code)) {
          return validCode;
        }
      }
      return UNKNOWN;
    }
  }
}
