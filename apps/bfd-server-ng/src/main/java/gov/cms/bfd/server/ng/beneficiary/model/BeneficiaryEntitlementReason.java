package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Entity representing BeneficiaryEntitlementReason. */
@Entity
@Getter
@Table(name = "beneficiary_entitlement_reason", schema = "idr")
public class BeneficiaryEntitlementReason {

  @Id
  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_rng_bgn_dt")
  private LocalDate benefitRangeBeginDate;

  @Column(name = "bene_rng_end_dt")
  private LocalDate benefitRangeEndDate;

  @Column(name = "bene_mdcr_entlmt_rsn_cd")
  private String medicareEntitlementReasonCode;

  @Column(name = "idr_trans_obslt_ts")
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts")
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts")
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  /**
   * create entitlement Status Extension.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {
    List<Extension> extensions = new ArrayList<>();

    Optional.ofNullable(this.getMedicareEntitlementReasonCode())
        .filter(code -> !code.isBlank())
        .ifPresent(
            validCode ->
                extensions.add(
                    new Extension(SystemUrls.EXT_BENE_MDCR_ENTLMT_RSN_CD_URL)
                        .setValue(
                            new Coding(SystemUrls.SYS_BENE_MDCR_ENTLMT_RSN_CD, validCode, null))));
    return extensions;
  }
}
