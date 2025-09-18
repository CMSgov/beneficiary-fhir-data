package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

@Embeddable
class BillablePeriod {
  @Column(name = "clm_from_dt")
  private LocalDate claimFromDate;

  @Column(name = "clm_thru_dt")
  private LocalDate claimThroughDate;

  @Column(name = "clm_query_cd")
  private String claimQueryCode;

  Period toFhir() {
    var period = new Period();
    period.setStartElement(DateUtil.toFhirDate(claimFromDate));
    period.setEndElement(DateUtil.toFhirDate(claimThroughDate));
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
