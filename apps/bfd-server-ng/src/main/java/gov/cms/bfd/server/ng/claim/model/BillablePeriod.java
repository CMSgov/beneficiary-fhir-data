package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

@Embeddable
public class BillablePeriod {
  @Column(name = "clm_from_dt")
  private LocalDate claimFromDate;

  @Column(name = "clm_thru_dt")
  private LocalDate claimThroughDate;

  @Column(name = "clm_query_cd")
  private String claimQueryCode;

  Period toFhir() {
    var period =
        new Period()
            .setStart(DateUtil.toDate(claimFromDate))
            .setEnd(DateUtil.toDate(claimThroughDate));
    period.addExtension(
        new Extension()
            .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_QUERY_CODE)
            .setValue(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_QUERY_CODE)
                    .setCode(claimQueryCode)));
    return period;
  }
}
