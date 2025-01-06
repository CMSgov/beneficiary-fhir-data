package gov.cms.bfd.server.war.r4.providers.pac;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.model.rda.samhsa.McsTag;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.LookUpSamhsaSecurityTags;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.AbstractTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.McsTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
@Slf4j
@Component
public class McsClaimTransformerV2 extends AbstractTransformerV2
    implements ResourceTransformer<Claim> {

  /** Injecting lookUpSamhsaSecurityTags. */
  @Autowired private LookUpSamhsaSecurityTags lookUpSamhsaSecurityTags;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** ICD 9. */
  private static final String ICD_TYPE_9 = "9";

  /** ICD 10. */
  private static final String ICD_TYPE_10 = "0";

  /**
   * Map used to calculate {@link Claim.ClaimStatus} from {@link RdaMcsClaim#getIdrStatusCode()} Any
   * item in this list is considered {@link Claim.ClaimStatus#CANCELLED}, every other value is
   * considered {@link Claim.ClaimStatus#ACTIVE}.
   */
  private static final List<String> CANCELED_STATUS_CODES = List.of("r", "z", "9");

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param lookUpSamhsaSecurityTags SamhsaSecurityTags lookup
   */
  public McsClaimTransformerV2(
      MetricRegistry metricRegistry, LookUpSamhsaSecurityTags lookUpSamhsaSecurityTags) {
    this.metricRegistry = metricRegistry;
    this.lookUpSamhsaSecurityTags = requireNonNull(lookUpSamhsaSecurityTags);
  }

  /**
   * Transforms a given {@link RdaMcsClaim} into a FHIR {@link Claim} object, recording metrics with
   * the given {@link MetricRegistry}.
   *
   * @param claimEntity the MCS {@link RdaMcsClaim} to transform
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  @Trace
  public Claim transform(Object claimEntity, boolean includeTaxNumbers) {
    if (!(claimEntity instanceof RdaMcsClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      RdaMcsClaim claim = (RdaMcsClaim) claimEntity;
      String securityTag =
          lookUpSamhsaSecurityTags.getClaimSecurityLevel(claim.getIdrClmHdIcn(), McsTag.class);
      return transformClaim(claim, includeTaxNumbers, securityTag);
    }
  }

  /**
   * Transforms a given {@link RdaMcsClaim} into a FHIR {@link Claim} object.
   *
   * @param claimGroup the {@link RdaMcsClaim} to transform
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @param securityTag securityTag tag of a claim
   * @return a FHIR {@link Claim} resource that represents the specified {@link RdaMcsClaim}
   */
  private Claim transformClaim(
      RdaMcsClaim claimGroup, boolean includeTaxNumbers, String securityTag) {
    Claim claim = new Claim();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
    claim.setContained(
        List.of(
            McsTransformerV2.getContainedPatient(claimGroup),
            getContainedProvider(claimGroup, includeTaxNumbers)));
    claim
        .getIdentifier()
        .add(createClaimIdentifier(BBCodingSystems.MCS.ICN, claimGroup.getIdrClmHdIcn()));
    addExtension(
        claim.getExtension(), BBCodingSystems.MCS.CLAIM_TYPE, claimGroup.getIdrClaimType());
    claim.setStatus(getStatus(claimGroup));
    claim.setType(createCodeableConcept(ClaimType.PROFESSIONAL));
    claim.setBillablePeriod(
        createPeriod(claimGroup.getIdrHdrFromDateOfSvc(), claimGroup.getIdrHdrToDateOfSvc()));
    claim.setUse(Claim.Use.CLAIM);
    claim.setPriority(createCodeableConcept(ProcessPriority.NORMAL));
    claim.setTotal(createTotalChargeAmount(claimGroup.getIdrTotBilledAmt()));
    claim.setProvider(new Reference("#provider-org"));
    claim.setPatient(new Reference("#patient"));
    claim.setDiagnosis(getDiagnosis(claimGroup));
    claim.setItem(getItems(claimGroup));

    claim.setCreated(new Date());

    List<Coding> securityTags = new ArrayList<>();

    // Create a Coding object for the security level
    Coding securityTagCoding =
        new Coding()
            .setSystem("https://terminology.hl7.org/6.1.0/CodeSystem-v3-Confidentiality.html")
            .setCode(securityTag)
            .setDisplay(securityTag);

    // Add the Coding to the list
    securityTags.add(securityTagCoding);
    Meta meta = new Meta();
    claim.setMeta(meta.setSecurity(securityTags));
    claim.setMeta(meta.setLastUpdated(Date.from(claimGroup.getLastUpdated())));

    return claim;
  }

  /**
   * Parses out provider data from the given {@link RdaMcsClaim} object, creating a FHIR {@link
   * Organization} object containing the provider data.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @return The generated {@link Organization} object with the parsed patient data.
   */
  private Resource getContainedProvider(RdaMcsClaim claimGroup, boolean includeTaxNumbers) {
    Organization organization = new Organization();
    List<Extension> extensions = organization.getExtension();
    addExtension(extensions, BBCodingSystems.MCS.BILL_PROV_TYPE, claimGroup.getIdrBillProvType());
    addExtension(extensions, BBCodingSystems.MCS.BILL_PROV_SPEC, claimGroup.getIdrBillProvSpec());

    if (includeTaxNumbers) {
      addFedTaxNumberIdentifier(
          organization, BBCodingSystems.MCS.BILL_PROV_EIN, claimGroup.getIdrBillProvEin());
    }
    addNpiIdentifier(organization, claimGroup.getIdrBillProvNpi());
    organization.setId("provider-org");

    return organization;
  }

  /**
   * Parses out the claim status from the given {@link RdaMcsClaim} object, mapping it to a
   * corresponding {@link Claim.ClaimStatus} value.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The {@link Claim.ClaimStatus} that corresponds to the given {@link
   *     RdaMcsClaim#getIdrStatusCode()}.
   */
  private Claim.ClaimStatus getStatus(RdaMcsClaim claimGroup) {
    return claimGroup.getIdrStatusCode() == null
            || !CANCELED_STATUS_CODES.contains(claimGroup.getIdrStatusCode().toLowerCase())
        ? Claim.ClaimStatus.ACTIVE
        : Claim.ClaimStatus.CANCELLED;
  }

  /**
   * Return the list of diagnosis codes wrapped in a {@link McsDiagnosisAdapterV2}.
   *
   * @param claimGroup claim group
   * @return diagnosis adapter
   */
  private static Stream<McsDiagnosisAdapterV2> getDiagnosisCodes(RdaMcsClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getDiagCodes(), List.<RdaMcsDiagnosisCode>of())
        .stream()
        .sorted(Comparator.comparing(RdaMcsDiagnosisCode::getRdaPosition))
        .map(McsDiagnosisAdapterV2::new);
  }

  /**
   * Return the list of primary diagnosis codes wrapped in a {@link McsDiagnosisAdapterV2}.
   *
   * @param claimGroup claim group
   * @return diagnosis adapter
   */
  private static Stream<McsDiagnosisAdapterV2> getPrimaryDiagnosisCodes(RdaMcsClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getDetails(), List.<RdaMcsDetail>of()).stream()
        .sorted(Comparator.comparing(RdaMcsDetail::getIdrDtlNumber))
        .map(McsDiagnosisAdapterV2::new);
  }

  /**
   * Map the {@link McsDiagnosisAdapterV2} to a {@link Claim.DiagnosisComponent}.
   *
   * @param diagnosisAdapter adapter
   * @return component
   */
  private static Claim.DiagnosisComponent getDiagnosisComponent(
      McsDiagnosisAdapterV2 diagnosisAdapter) {

    if (Strings.isNotBlank(diagnosisAdapter.getDiagnosisCode())) {

      String system = null;
      if (diagnosisAdapter.getIcdType() != null) {
        system =
            switch (diagnosisAdapter.getIcdType()) {
              case ICD_TYPE_9 -> IcdCode.CODING_SYSTEM_ICD_9;
              case ICD_TYPE_10 -> IcdCode.CODING_SYSTEM_ICD_10_CM;
              default -> null;
            };
      }

      return new Claim.DiagnosisComponent()
          .setDiagnosis(createCodeableConcept(system, diagnosisAdapter.getDiagnosisCode()));
    }

    return null;
  }

  /**
   * Parses out the diagnosis data from the given {@link RdaMcsClaim} object, creating a list of
   * FHIR {@link Claim.DiagnosisComponent} objects.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The list of {@link Claim.DiagnosisComponent} objects containing the diagnosis data.
   */
  private List<Claim.DiagnosisComponent> getDiagnosis(RdaMcsClaim claimGroup) {

    Stream<McsDiagnosisAdapterV2> distinctDiagnosisCodes =
        Stream.concat(getDiagnosisCodes(claimGroup), getPrimaryDiagnosisCodes(claimGroup))
            .distinct();

    List<Claim.DiagnosisComponent> diagnoses =
        distinctDiagnosisCodes
            .map(McsClaimTransformerV2::getDiagnosisComponent)
            .filter(Objects::nonNull)
            .toList();

    // Certain records may be filtered out, so we need to set the sequence after we finish
    // processing.
    for (int i = 0; i < diagnoses.size(); i++) {
      diagnoses.get(i).setSequence(i + 1);
    }
    return diagnoses;
  }

  /**
   * Parses out the line item data from the given {@link RdaMcsClaim} object, creating a list of
   * FHIR {@link Claim.ItemComponent} objects.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The list of {@link Claim.ItemComponent} objects containing the line item data.
   */
  private List<Claim.ItemComponent> getItems(RdaMcsClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getDetails(), List.<RdaMcsDetail>of()).stream()
        .map(
            detail -> {
              Claim.ItemComponent item;

              if (Strings.isNotBlank(detail.getIdrProcCode())) {
                List<Coding> codings = new ArrayList<>();
                CodeableConcept productOrService = new CodeableConcept();

                codings.add(
                    new Coding(
                        TransformerConstants.CODING_SYSTEM_CARIN_HCPCS,
                        detail.getIdrProcCode(),
                        null));
                productOrService.setCoding(codings);
                item =
                    new Claim.ItemComponent()
                        .setSequence(detail.getIdrDtlNumber())
                        // The FHIR spec requires productOrService to exist even if there is
                        // no product code, so printing out the system regardless because
                        // HAPI won't serialize it unless there is some sort of value inside.
                        .setProductOrService(productOrService)
                        .setServiced(
                            createPeriod(detail.getIdrDtlFromDate(), detail.getIdrDtlToDate()))
                        .setModifier(getModifiers(detail));

                // Set the DiagnosisSequence only if the detail Dx Code is not null and present in
                // the Dx table.
                if (detail.getIdrDtlPrimaryDiagCode() != null) {
                  String detailDxCode = detail.getIdrDtlPrimaryDiagCode();

                  Optional<RdaMcsDiagnosisCode> matchingCode =
                      claimGroup.getDiagCodes().stream()
                          .filter(dxCode -> codesAreEqual(dxCode.getIdrDiagCode(), detailDxCode))
                          .findFirst();

                  matchingCode.ifPresent(
                      dxCode ->
                          item.setDiagnosisSequence(
                              List.of(new PositiveIntType(dxCode.getRdaPosition()))));
                }

                if (Strings.isNotBlank(detail.getIdrDtlNdc())
                    && Strings.isNotBlank(detail.getIdrDtlNdcUnitCount())) {
                  Claim.DetailComponent detailComponent = new Claim.DetailComponent();
                  int sequenceNumber = 1;
                  detailComponent.setSequence(sequenceNumber);
                  detailComponent.setProductOrService(
                      createCodeableConcept(
                          TransformerConstants.CODING_NDC, detail.getIdrDtlNdc()));

                  try {
                    detailComponent.setQuantity(
                        new Quantity(Double.parseDouble(detail.getIdrDtlNdcUnitCount())));
                  } catch (NumberFormatException ex) {
                    // If the NDC_UNIT_COUNT isn't a valid number, do not set quantity value.
                    // Awaiting upstream RDA test data changes
                    log.error(
                        "Failed to parse IdrDtlNdcUnitCount as a number: message={}",
                        ex.getMessage(),
                        ex);
                  }
                  item.addDetail(detailComponent);
                }
              } else {
                item = null;
              }

              return item;
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(Claim.ItemComponent::getSequence))
        .toList();
  }

  /**
   * Parses out the modifier data from the given {@link RdaMcsDetail} object, creating a list of
   * FHIR {@link CodeableConcept} objects.
   *
   * @param detail the {@link RdaMcsDetail} to parse.
   * @return The list of {@link CodeableConcept} objects containing the modifier data.
   */
  private List<CodeableConcept> getModifiers(RdaMcsDetail detail) {
    List<Optional<String>> mods =
        List.of(
            Optional.ofNullable(detail.getIdrModOne()),
            Optional.ofNullable(detail.getIdrModTwo()),
            Optional.ofNullable(detail.getIdrModThree()),
            Optional.ofNullable(detail.getIdrModFour()));

    // OptionalGetWithoutIsPresent - IsPresent used in filter
    //noinspection OptionalGetWithoutIsPresent
    return IntStream.range(0, mods.size())
        .filter(i -> mods.get(i).isPresent())
        .mapToObj(
            index ->
                new CodeableConcept(
                    new Coding(
                            TransformerConstants.CODING_SYSTEM_CARIN_HCPCS,
                            mods.get(index).get(),
                            null)
                        .setVersion(String.valueOf(index + 1))))
        .toList();
  }
}
