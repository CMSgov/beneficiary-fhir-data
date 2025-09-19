package gov.cms.bfd.server.ng.claim.model;

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
                        .setCode(s)));
  }
}
