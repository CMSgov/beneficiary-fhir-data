package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

/** Entity representing BeneficiaryEntitlement. */
@Entity
@Getter
@Table(name = "beneficiary_entitlement_latest", schema = "idr")
public class BeneficiaryEntitlement {

  @EmbeddedId private BeneficiaryEntitlementId id;

  @Column(name = "bene_mdcr_enrlmt_rsn_cd")
  private Optional<String> medicareEnrollmentReasonCode;

  @Column(name = "bene_mdcr_entlmt_stus_cd")
  private Optional<String> medicareEntitlementStatusCode;

  @Column(name = "idr_trans_obslt_ts")
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts")
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts")
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  /**
   * create create Medicare Status Extension.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {
    List<Extension> extensions = new ArrayList<>();

    // Handle Medicare Enrollment Reason Code
    medicareEnrollmentReasonCode.ifPresent(
        validCode ->
            extensions.add(
                new Extension(SystemUrls.EXT_BENE_ENRLMT_RSN_CD_URL)
                    .setValue(new Coding(SystemUrls.SYS_BENE_ENRLMT_RSN_CD, validCode, null))));

    // Handle Medicare Entitlement Status Code
    medicareEntitlementStatusCode.ifPresent(
        validCode ->
            extensions.add(
                new Extension(SystemUrls.EXT_BENE_MDCR_ENTLMT_STUS_CD_URL)
                    .setValue(
                        new Coding(SystemUrls.SYS_BENE_MDCR_ENTLMT_STUS_CD, validCode, null))));

    return extensions;
  }

  Period toFhirPeriod() {
    return id.toFhirPeriod();
  }

  Coverage.CoverageStatus toFhirStatus() {
    return id.toFhirStatus();
  }
}
