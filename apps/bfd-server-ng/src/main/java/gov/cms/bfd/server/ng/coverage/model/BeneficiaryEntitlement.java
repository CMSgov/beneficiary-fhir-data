package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.jetbrains.annotations.NotNull;

/** Entity representing BeneficiaryEntitlement. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_entitlement_latest", schema = "idr")
public class BeneficiaryEntitlement implements Comparable<BeneficiaryEntitlement> {

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

  @Column(name = "idr_ltst_trans_flg")
  private Optional<String> idrLatestTransactionFlag;

  private BeneficiaryEntitlementPeriod entitlementPeriod;

  /**
   * create create Medicare Status Extension.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {

    // Handle Medicare Enrollment Reason Code
    var extEnrollmentReason =
        medicareEnrollmentReasonCode.map(
            validCode ->
                new Extension(SystemUrls.EXT_BENE_ENRLMT_RSN_CD_URL)
                    .setValue(new Coding(SystemUrls.SYS_BENE_ENRLMT_RSN_CD, validCode, null)));

    // Handle Medicare Entitlement Status Code
    var extStatusCode =
        medicareEntitlementStatusCode.map(
            validCode ->
                new Extension(SystemUrls.EXT_BENE_MDCR_ENTLMT_STUS_CD_URL)
                    .setValue(
                        new Coding(SystemUrls.SYS_BENE_MDCR_ENTLMT_STUS_CD, validCode, null)));

    return Stream.of(extEnrollmentReason, extStatusCode).flatMap(Optional::stream).toList();
  }

  Period toFhirPeriod() {
    return entitlementPeriod.toFhirPeriod();
  }

  Coverage.CoverageStatus toFhirStatus() {
    return entitlementPeriod.toFhirStatus();
  }

  @Override
  public int compareTo(@NotNull BeneficiaryEntitlement o) {
    return this.id.compareTo(o.id);
  }
}
