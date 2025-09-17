package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

/**
 * Represents the billable period for a claim, including the start and end dates and the query code.
 *
 * <p>This class is used to model the coverage period of a claim and provides a method to convert
 * the period to a FHIR {@link org.hl7.fhir.r4.model.Period} object, including an extension for the
 * claim query code.
 *
 * <ul>
 *   <li>{@code claimFromDate}: The start date of the claim's billable period.
 *   <li>{@code claimThroughDate}: The end date of the claim's billable period.
 *   <li>{@code claimQueryCode}: The code representing the query type for the claim.
 * </ul>
 */
@Embeddable
public class BillablePeriod {
  @Column(name = "clm_from_dt")
  private LocalDate claimFromDate;

  @Column(name = "clm_thru_dt")
  private LocalDate claimThroughDate;

  @Column(name = "clm_query_cd")
  private String claimQueryCode;

  /**
   * Returns the start date of the claim's billable period.
   *
   * @return claimFromDate
   */
  public LocalDate getClaimFromDate() {
    return claimFromDate;
  }

  /**
   * Returns the end date of the claim's billable period.
   *
   * @return claimThroughDate
   */
  public LocalDate getClaimThroughDate() {
    return claimThroughDate;
  }

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
