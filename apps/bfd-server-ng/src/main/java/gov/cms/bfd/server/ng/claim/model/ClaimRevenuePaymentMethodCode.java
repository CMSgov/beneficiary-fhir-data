package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

public class ClaimRevenuePaymentMethodCode {
  @Column(name = "clm_rev_pmt_mthd_cd")
  private Optional<String> revenuePaymentMethodCode;

  Optional<Extension> toFhir() {
    return revenuePaymentMethodCode.map(
        s ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_REVENUE_PAYMENT_METHOD_CODE)
                .setValue(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_REVENUE_PAYMENT_METHOD_CODE)
                        .setCode(s)));
  }
}
