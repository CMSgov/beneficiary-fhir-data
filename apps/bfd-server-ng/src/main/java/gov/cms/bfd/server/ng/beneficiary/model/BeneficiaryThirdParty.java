package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.IdrConstants;
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
import org.hl7.fhir.r4.model.Period;

/** Entity representing beneficiary_third_party. */
@Entity
@Getter
@Table(name = "beneficiary_third_party", schema = "idr")
public class BeneficiaryThirdParty {

  @EmbeddedId private BeneficiaryThirdPartyId id;

  @Column(name = "bene_buyin_cd")
  private String buyInCode;

  @Column(name = "idr_trans_obslt_ts")
  private ZonedDateTime idrTransObsoleteTimestamp;

  @Column(name = "idr_updt_ts")
  private ZonedDateTime idrUpdateTimestamp;

  @Column(name = "bfd_created_ts")
  private ZonedDateTime bfdCreatedTimestamp;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  /**
   * Creates a FHIR {@link Period} object based on this BeneficiaryThirdParty's benefit date range
   * if a start date is present.
   *
   * @return An {@link Optional} containing the FHIR {@link Period}, or {@link Optional#empty()} if
   *     no period can be constructed (e.g., no start date).
   */
  public Optional<Period> createFhirPeriod() {
    Optional<LocalDate> optStartDate = Optional.ofNullable(this.getId().getBenefitRangeBeginDate());
    Optional<LocalDate> optEndDate = Optional.ofNullable(this.getId().getBenefitRangeEndDate());

    return optStartDate.map(
        startDate -> {
          Period period = new Period();
          period.setStartElement(DateUtil.toFhirDate(startDate));

          optEndDate
              .filter(endDate -> endDate.isBefore(IdrConstants.DEFAULT_DATE))
              .ifPresent(validEndDate -> period.setEndElement(DateUtil.toFhirDate(validEndDate)));

          return period;
        });
  }

  /**
   * create BuyIn Code Extension.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {
    List<Extension> extensions = new ArrayList<>();

    Optional.ofNullable(this.getBuyInCode())
        .filter(code -> !code.isBlank())
        .ifPresent(
            validCode ->
                extensions.add(
                    new Extension(SystemUrls.EXT_BENE_BUYIN_CD_URL)
                        .setValue(new Coding(SystemUrls.SYS_BENE_BUYIN_CD, validCode, null))));
    return extensions;
  }
}
