package gov.cms.bfd.server.war.r4.providers.pac;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.IcdCode;
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
import org.springframework.stereotype.Component;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
@Slf4j
@Component
public class McsClaimTransformerV2 extends AbstractTransformerV2
    implements ResourceTransformer<Claim> {

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** Valid ICD Types. */
  private static final List<String> VALID_ICD_TYPES = List.of("0", "9");

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
   */
  public McsClaimTransformerV2(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
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
      return transformClaim((RdaMcsClaim) claimEntity, includeTaxNumbers);
    }
  }

  /**
   * Transforms a given {@link RdaMcsClaim} into a FHIR {@link Claim} object.
   *
   * @param claimGroup the {@link RdaMcsClaim} to transform
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @return a FHIR {@link Claim} resource that represents the specified {@link RdaMcsClaim}
   */
  private Claim transformClaim(RdaMcsClaim claimGroup, boolean includeTaxNumbers) {
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
    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));

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
   * Parses out the diagnosis data from the given {@link RdaMcsClaim} object, creating a list of
   * FHIR {@link Claim.DiagnosisComponent} objects.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The list of {@link Claim.DiagnosisComponent} objects containing the diagnosis data.
   */
  private List<Claim.DiagnosisComponent> getDiagnosis(RdaMcsClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getDiagCodes(), List.<RdaMcsDiagnosisCode>of())
        .stream()
        .map(
            diagCode -> {
              Claim.DiagnosisComponent component;

              if (Strings.isNotBlank(diagCode.getIdrDiagCode())) {
                String system;

                if (VALID_ICD_TYPES.contains(diagCode.getIdrDiagIcdType())) {
                  system =
                      diagCode.getIdrDiagIcdType().equals("0")
                          ? IcdCode.CODING_SYSTEM_ICD_10_CM
                          : IcdCode.CODING_SYSTEM_ICD_9;
                } else {
                  system = null;
                }

                component =
                    new Claim.DiagnosisComponent()
                        .setSequence(diagCode.getRdaPosition())
                        .setDiagnosis(createCodeableConcept(system, diagCode.getIdrDiagCode()));
              } else {
                component = null;
              }

              return component;
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(Claim.DiagnosisComponent::getSequence))
        .toList();
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
                  System.out.println("HERE");
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
