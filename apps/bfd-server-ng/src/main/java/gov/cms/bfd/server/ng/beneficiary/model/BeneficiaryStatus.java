package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.SystemUrls;
import gov.cms.bfd.server.ng.coverage.MedicareStatusCodeType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Entity representing Beneficiary Status. */
@Entity
@Getter
@Table(name = "beneficiary_status", schema = "idr")
public class BeneficiaryStatus {

  @Id
  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "mdcr_stus_bgn_dt")
  private LocalDate medicareStatusBeginDate;

  @Column(name = "mdcr_stus_end_dt")
  private LocalDate medicareStatusEndDate;

  @Column(name = "bene_mdcr_stus_cd")
  private String medicareStatusCode;

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

    Optional.ofNullable(this.getMedicareStatusCode())
        .filter(code -> !code.isBlank())
        .ifPresent(
            validStatusCode -> {
              extensions.add(
                  new Extension(SystemUrls.EXT_BENE_MDCR_STUS_CD_URL)
                      .setValue(
                          new Coding(SystemUrls.SYS_BENE_MDCR_STUS_CD, validStatusCode, null)));

              MedicareStatusCodeType.fromCode(this.medicareStatusCode)
                  .ifPresent(
                      statusCodeType -> {
                        Optional.ofNullable(statusCodeType.getEsrdIndicator())
                            .filter(indicator -> !indicator.isBlank())
                            .ifPresent(
                                validIndicator ->
                                    extensions.add(
                                        new Extension(SystemUrls.EXT_BENE_ESRD_STUS_ID_URL)
                                            .setValue(
                                                new Coding(
                                                    SystemUrls.SYS_BENE_ESRD_STUS_ID,
                                                    validIndicator,
                                                    null))));

                        Optional.ofNullable(statusCodeType.getDisabilityIndicator())
                            .filter(indicator -> !indicator.isBlank())
                            .ifPresent(
                                validIndicator ->
                                    extensions.add(
                                        new Extension(SystemUrls.EXT_BENE_DSBLD_STUS_ID_URL)
                                            .setValue(
                                                new Coding(
                                                    SystemUrls.SYS_BENE_DSBLD_STUS_ID,
                                                    validIndicator,
                                                    null))));
                      });
            });

    return extensions;
  }
}
