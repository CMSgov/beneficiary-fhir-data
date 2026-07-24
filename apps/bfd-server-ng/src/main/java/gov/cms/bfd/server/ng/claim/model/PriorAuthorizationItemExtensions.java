package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

@Embeddable
class PriorAuthorizationItemExtensions {

  @Getter
  @Column(name = "pa_decision")
  private Optional<PriorAuthorizationDecision> decision;

  @Column(name = "pa_dt_added")
  private LocalDate priorAuthDateAdded;

  @Getter
  @Column(name = "pa_dt_updated")
  private Optional<LocalDate> priorAuthDateUpdated;

  @Column(name = "pa_req_sub_dt")
  private LocalDate requestSubmitDate;

  @Column(name = "pa_req_rec_dt")
  private LocalDate requestReceivedDate;

  @Column(name = "pa_decision_dt")
  private Optional<LocalDate> decisionDate;

  @Column(name = "pa_decision_exp_dt")
  private Optional<LocalDate> decisionExpiredDate;

  @Column(name = "service_cnts")
  private int numberOfServices;

  @Column(name = "svc_render_st")
  private String servicingOrRenderingStatus;

  @Column(name = "mr_count_ind")
  private int mrCountIndicator;

  @Column(name = "mr_count_st_dt")
  private Optional<LocalDate> mrCountStartDate;

  @Column(name = "mr_count_end_dt")
  private Optional<LocalDate> mrCountEndDate;

  @Column(name = "rrb_excl_ind")
  private Optional<String> rrbExcludeIndicator;

  List<Extension> toFhir() {
    var priorAuthDateAddedExtension =
        new Extension(SystemUrls.EXT_PA_DT_ADDED_URL)
            .setValue(DateUtil.toFhirDate(priorAuthDateAdded));

    var priorAuthDateUpdatedExtension =
        priorAuthDateUpdated.map(
            updatedDate ->
                new Extension(SystemUrls.EXT_PA_DT_UPDATED_URL)
                    .setValue(DateUtil.toFhirDate(updatedDate)));

    var requestSubmitDateExtension =
        new Extension(SystemUrls.EXT_PA_REQ_SUB_DT_URL)
            .setValue(DateUtil.toFhirDate(requestSubmitDate));

    var requestReceivedDateExtension =
        new Extension(SystemUrls.EXT_PA_REQ_REC_DT_URL)
            .setValue(DateUtil.toFhirDate(requestReceivedDate));

    var decisionDateExtension =
        decisionDate.map(
            dt ->
                new Extension(SystemUrls.EXT_PA_DECISION_DT_URL).setValue(DateUtil.toFhirDate(dt)));

    var decisionExpiredDateExtension =
        decisionExpiredDate.map(
            dt ->
                new Extension(SystemUrls.EXT_PA_DECISION_EXP_DT_URL)
                    .setValue(DateUtil.toFhirDate(dt)));

    var numberOfServicesExtension =
        new Extension(SystemUrls.EXT_PA_SERVICE_CNTS_URL)
            .setValue(new IntegerType(numberOfServices));

    var mrCountIndicatorExtension =
        new Extension(SystemUrls.EXT_PA_MR_COUNT_INDICATOR_URL)
            .setValue(new IntegerType(mrCountIndicator));

    var mrCountStartDateExtension =
        mrCountStartDate.map(
            dt ->
                new Extension(SystemUrls.EXT_MR_COUNT_ST_DT_URL).setValue(DateUtil.toFhirDate(dt)));

    var mrCountEndDateExtension =
        mrCountEndDate.map(
            dt ->
                new Extension(SystemUrls.EXT_MR_COUNT_END_DT_URL)
                    .setValue(DateUtil.toFhirDate(dt)));

    var rrbExcludeIndicatorExtension =
        rrbExcludeIndicator
            .filter(rrbExcludeInd -> rrbExcludeInd.equals("Y"))
            .map(
                _ ->
                    new Extension(SystemUrls.EXT_RRB_EXCL_IND_URL)
                        .setValue(new StringType("true")));

    var servicingOrRenderingStatusExtension =
        new Extension()
            .setUrl(SystemUrls.EXT_SVC_RENDER_ST_URL)
            .setValue(new Coding(SystemUrls.USPS, servicingOrRenderingStatus, null));

    return Stream.of(
            decision.map(PriorAuthorizationDecision::toFhir),
            Optional.of(priorAuthDateAddedExtension),
            priorAuthDateUpdatedExtension,
            Optional.of(requestSubmitDateExtension),
            Optional.of(requestReceivedDateExtension),
            decisionDateExtension,
            decisionExpiredDateExtension,
            Optional.of(numberOfServicesExtension),
            Optional.of(mrCountIndicatorExtension),
            mrCountStartDateExtension,
            mrCountEndDateExtension,
            rrbExcludeIndicatorExtension,
            Optional.of(servicingOrRenderingStatusExtension))
        .flatMap(Optional::stream)
        .toList();
  }
}
