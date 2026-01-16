package gov.cms.bfd.server.war.r4.providers.pac;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissPayer;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBSupportingInfoType;
import gov.cms.bfd.server.war.r4.providers.pac.common.AbstractTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.r4.providers.pac.common.FissTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.codesystems.ClaimInformationcategory;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ExDiagnosistype;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
@Slf4j
@Component
public class FissClaimTransformerV2 extends AbstractTransformerV2
    implements ResourceTransformer<Claim> {

  /** Date used to determine if an ICD code is ICD9 (before date) or ICD10 (on or after date). */
  private static final LocalDate ICD_9_CUTOFF_DATE = LocalDate.of(2015, 10, 1);

  /** The MEDICARE constant. */
  private static final String MEDICARE = "MEDICARE";

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  private final boolean samhsaV2Enabled;

  /** The METRIC_NAME constant. */
  private static final String METRIC_NAME =
      MetricRegistry.name(FissClaimTransformerV2.class.getSimpleName(), "transform");

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param securityTagManager SamhsaSecurityTags lookup
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public FissClaimTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a claim entity into a FHIR {@link Claim}.
   *
   * @param claimEntity the FISS {@link RdaFissClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  public Claim transform(ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof RdaFissClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      RdaFissClaim rdaFissClaim = (RdaFissClaim) claim;
      return transformClaim(rdaFissClaim, securityTags);
    }
  }

  /**
   * Transforms a {@link RdaFissClaim} into a FHIR {@link Claim}.
   *
   * @param claimGroup the {@link RdaFissClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link Claim} resource that represents the specified {@link RdaFissClaim}
   */
  private Claim transformClaim(RdaFissClaim claimGroup, List<Coding> securityTags) {
    Claim claim = new Claim();

    boolean isIcd9 =
        claimGroup.getStmtCovToDate() != null
            && claimGroup.getStmtCovToDate().isBefore(ICD_9_CUTOFF_DATE);

    claim.setId("f-" + claimGroup.getClaimId());
    claim.setContained(
        List.of(
            FissTransformerV2.getContainedPatient(claimGroup), getContainedProvider(claimGroup)));
    claim.getIdentifier().add(createClaimIdentifier(BBCodingSystems.FISS.DCN, claimGroup.getDcn()));
    addExtension(
        claim.getExtension(), BBCodingSystems.FISS.SERV_TYP_CD, claimGroup.getServTypeCd());
    claim.setStatus(Claim.ClaimStatus.ACTIVE);
    claim.setType(createCodeableConcept(ClaimType.INSTITUTIONAL));
    claim.setSupportingInfo(getSupportingInfo(claimGroup));
    claim.setBillablePeriod(
        createPeriod(claimGroup.getStmtCovFromDate(), claimGroup.getStmtCovToDate()));
    claim.setUse(Claim.Use.CLAIM);
    claim.setPriority(createCodeableConcept(ProcessPriority.NORMAL));
    claim.setTotal(createTotalChargeAmount(claimGroup.getTotalChargeAmount()));
    claim.setProvider(new Reference("#provider-org"));
    claim.setPatient(new Reference("#patient"));
    claim.setFacility(getFacility(claimGroup));
    claim.setDiagnosis(getDiagnosis(claimGroup, isIcd9));
    claim.setProcedure(getProcedure(claimGroup, isIcd9));
    claim.setInsurance(getInsurance(claimGroup));
    claim.setItem(getClaimItems(claimGroup));

    Meta meta = new Meta();
    meta.setLastUpdated(Date.from(claimGroup.getLastUpdated()));

    if (samhsaV2Enabled) {
      meta.setSecurity(securityTags);
    }
    claim.setMeta(meta);
    claim.setCreated(new Date());

    return claim;
  }

  /**
   * Parses data from the {@link RdaFissClaim} to create a {@link
   * Claim.SupportingInformationComponent} list.
   *
   * @param claimGroup The {@link RdaFissClaim} object to parse data from.
   * @return The {@link Claim.SupportingInformationComponent} object list created from the parsed
   *     data.
   */
  private List<Claim.SupportingInformationComponent> getSupportingInfo(RdaFissClaim claimGroup) {
    List<Claim.SupportingInformationComponent> supportingInfo = new ArrayList<>();
    int sequenceNumber = 1;

    if (Strings.isNotBlank(claimGroup.getFreqCd())) {
      supportingInfo.add(
          new Claim.SupportingInformationComponent()
              .setSequence(sequenceNumber)
              .setCategory(createCodeableConcept(C4BBSupportingInfoType.TYPE_OF_BILL))
              .setCode(
                  new CodeableConcept(
                      new Coding(BBCodingSystems.FISS.FREQ_CD, claimGroup.getFreqCd(), null))));

      ++sequenceNumber;
    }

    if (Strings.isNotBlank(claimGroup.getDrgCd())) {
      supportingInfo.add(
          new Claim.SupportingInformationComponent()
              .setSequence(sequenceNumber)
              .setCategory(
                  new CodeableConcept()
                      .setCoding(
                          List.of(
                              new Coding(
                                  ClaimInformationcategory.INFO.getSystem(),
                                  ClaimInformationcategory.INFO.toCode(),
                                  ClaimInformationcategory.INFO.getDisplay()),
                              new Coding(
                                  TransformerConstants.CODING_BBAPI_INFORMATION_CATEGORY,
                                  CCWUtils.calculateVariableReferenceUrl(
                                      CcwCodebookVariable.CLM_DRG_CD),
                                  "Claim Diagnosis Related Group Code (or MS-DRG Code)"))))
              .setCode(
                  new CodeableConcept(
                      new Coding(
                          CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD),
                          claimGroup.getDrgCd(),
                          null))));

      ++sequenceNumber;
    }

    if (Strings.isNotBlank(claimGroup.getAdmTypCd())) {
      supportingInfo.add(
          new Claim.SupportingInformationComponent()
              .setSequence(sequenceNumber)
              .setCategory(
                  createCodeableConceptForCategory(
                      TransformerConstants.CODING_BBAPI_INFORMATION_CATEGORY,
                      CcwCodebookVariable.CLM_IP_ADMSN_TYPE_CD))
              .setCode(
                  createCodeableConcept(
                      CCWUtils.calculateVariableReferenceUrl(
                          CcwCodebookVariable.CLM_IP_ADMSN_TYPE_CD),
                      claimGroup.getAdmTypCd())));
    }

    return supportingInfo;
  }

  /**
   * Parses data from the {@link RdaFissClaim} to create a provider {@link Resource}.
   *
   * @param claimGroup The {@link RdaFissClaim} object to parse data from.
   * @return The provider {@link Resource} object created from the parsed data.
   */
  private Resource getContainedProvider(RdaFissClaim claimGroup) {
    Organization organization = new Organization();

    if (claimGroup.getMedaProv_6() != null) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(createCodeableConcept(C4BBOrganizationIdentifierType.PRN))
                  .setSystem(BBCodingSystems.FISS.MEDA_PROV_6)
                  .setValue(claimGroup.getMedaProv_6()));
    }

    addNpiIdentifier(organization, claimGroup.getNpiNumber());
    organization.setId("provider-org");

    return organization;
  }

  /**
   * Parses data from the {@link RdaFissClaim} to create a facility {@link Reference}.
   *
   * @param claimGroup The {@link RdaFissClaim} object to parse data from.
   * @return The facility {@link Reference} object created from the parsed data.
   */
  private Reference getFacility(RdaFissClaim claimGroup) {
    Reference reference;

    if (Strings.isNotBlank(claimGroup.getLobCd())) {
      reference = new Reference();
      addExtension(reference.getExtension(), BBCodingSystems.FISS.LOB_CD, claimGroup.getLobCd());
    } else {
      reference = null;
    }

    return reference;
  }

  /**
   * Parses data from the {@link RdaFissClaim} to create a {@link Claim.DiagnosisComponent} list.
   *
   * @param claimGroup The {@link RdaFissClaim} object to parse data from.
   * @param isIcd9 if {@code true} sets the icd system to {@link IcdCode#CODING_SYSTEM_ICD_9}
   * @return The {@link Claim.DiagnosisComponent} object list created from the parsed data.
   */
  private List<Claim.DiagnosisComponent> getDiagnosis(RdaFissClaim claimGroup, boolean isIcd9) {
    final String icdSystem = isIcd9 ? IcdCode.CODING_SYSTEM_ICD_9 : IcdCode.CODING_SYSTEM_ICD_10_CM;

    Stream<FissDiagnosisAdapterV2> diagnosisCodes =
        ObjectUtils.defaultIfNull(claimGroup.getDiagCodes(), List.<RdaFissDiagnosisCode>of())
            .stream()
            .sorted(Comparator.comparing(RdaFissDiagnosisCode::getRdaPosition))
            .map(FissDiagnosisAdapterV2::new);

    Stream<FissDiagnosisAdapterV2> combinedDiagnosisCodes =
        getAllDistinctDiagnosisCodes(claimGroup, diagnosisCodes);

    List<Claim.DiagnosisComponent> diagnoses =
        combinedDiagnosisCodes
            .map(diagnosisCode -> getDiagnosisComponent(diagnosisCode, claimGroup, icdSystem))
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
   * Creates a distinct list of all diagnosis codes for the claim.
   *
   * @param claimGroup FISS claim.
   * @param diagnosisCodes List of codes.
   * @return All diagnosis codes wrapped in adapters.
   */
  private static Stream<FissDiagnosisAdapterV2> getAllDistinctDiagnosisCodes(
      RdaFissClaim claimGroup, Stream<FissDiagnosisAdapterV2> diagnosisCodes) {
    Stream<FissDiagnosisAdapterV2> extraCodes =
        Stream.of(
            new FissDiagnosisAdapterV2(claimGroup.getAdmitDiagCode()),
            new FissDiagnosisAdapterV2(claimGroup.getPrincipleDiag()));

    // Most of the time, the principal and admit codes should already be included in the list
    // returned by getDiagCodes(),
    // but if they're not, we need to make sure they're included in the final result.
    // Here we'll concatenate them all together and filter out any duplicates.
    return Stream.concat(diagnosisCodes, extraCodes).distinct();
  }

  /**
   * Creates a {@link Claim.DiagnosisComponent} for the claim.
   *
   * @param diagnosisCodeAdapter Diagnosis code adapter.
   * @param claimGroup FISS claim.
   * @param icdSystem ICD system.
   * @return Diagnosis component.
   */
  private Claim.DiagnosisComponent getDiagnosisComponent(
      FissDiagnosisAdapterV2 diagnosisCodeAdapter, RdaFissClaim claimGroup, String icdSystem) {

    String diagnosisCode = diagnosisCodeAdapter.getDiagnosisCode();
    if (Strings.isBlank(diagnosisCode)) {
      return null;
    }

    Claim.DiagnosisComponent component =
        new Claim.DiagnosisComponent()
            .setDiagnosis(createCodeableConcept(icdSystem, diagnosisCode));

    // Present on Admission
    if (Strings.isNotBlank(diagnosisCodeAdapter.getPoaIndicator())) {
      component.setOnAdmission(
          createCodeableConcept(
              BBCodingSystems.FISS.DIAG_POA_IND,
              diagnosisCodeAdapter.getPoaIndicator().toLowerCase()));
    }

    if (Strings.isNotBlank(claimGroup.getAdmitDiagCode())
        && codesAreEqual(claimGroup.getAdmitDiagCode(), diagnosisCode)) {
      component.addType(createCodeableConcept(ExDiagnosistype.ADMITTING));
    }

    if (Strings.isNotBlank(claimGroup.getPrincipleDiag())
        && codesAreEqual(claimGroup.getPrincipleDiag(), diagnosisCode)) {
      component.addType(createCodeableConcept(ExDiagnosistype.PRINCIPAL));
    }

    return component;
  }

  /**
   * Parses data from the {@link RdaFissClaim} to create a {@link Claim.ProcedureComponent} list.
   *
   * @param claimGroup The {@link RdaFissClaim} object to parse data from.
   * @param isIcd9 if {@code true} sets the icd system to {@link IcdCode#CODING_SYSTEM_ICD_9}
   * @return The {@link Claim.ProcedureComponent} object list created from the parsed data.
   */
  private List<Claim.ProcedureComponent> getProcedure(RdaFissClaim claimGroup, boolean isIcd9) {
    final String icdSystem =
        isIcd9 ? IcdCode.CODING_SYSTEM_ICD_9_MEDICARE : IcdCode.CODING_SYSTEM_ICD_10_MEDICARE;

    return ObjectUtils.defaultIfNull(claimGroup.getProcCodes(), List.<RdaFissProcCode>of()).stream()
        .map(
            procCode ->
                new Claim.ProcedureComponent()
                    .setSequence(procCode.getRdaPosition())
                    .setDate(localDateToDate(procCode.getProcDate()))
                    .setProcedure(createCodeableConcept(icdSystem, procCode.getProcCode())))
        .sorted(Comparator.comparing(Claim.ProcedureComponent::getSequence))
        .toList();
  }

  /**
   * Parses data from the {@link RdaFissClaim} to create a {@link Claim.InsuranceComponent} list.
   *
   * @param claimGroup The {@link RdaFissClaim} object to parse data from.
   * @return The {@link Claim.InsuranceComponent} object list created from the parsed data.
   */
  private List<Claim.InsuranceComponent> getInsurance(RdaFissClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getPayers(), List.<RdaFissPayer>of()).stream()
        .map(
            payer -> {
              Claim.InsuranceComponent component;

              if (Strings.isNotBlank(payer.getPayersName())) {
                component =
                    new Claim.InsuranceComponent()
                        .setSequence(payer.getRdaPosition())
                        .setFocal(Objects.equals(payer.getPayersName(), MEDICARE));

                component.setCoverage(
                    new Reference()
                        .setIdentifier(
                            new Identifier()
                                .setSystem(BBCodingSystems.FISS.PAYERS_NAME)
                                .setValue(payer.getPayersName())));
              } else {
                component = null;
              }

              return component;
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(Claim.InsuranceComponent::getSequence))
        .toList();
  }

  /**
   * Maps {@link RdaFissRevenueLine} data to appropriate FHIR components.
   *
   * @param claimGroup The claim data to map.
   * @return The created FHIR component with given claim data.
   */
  private List<Claim.ItemComponent> getClaimItems(RdaFissClaim claimGroup) {
    return claimGroup.getRevenueLines().stream()
        .sorted(Comparator.comparing(RdaFissRevenueLine::getRdaPosition))
        .map(
            revenueLine -> {
              Claim.ItemComponent itemComponent = new Claim.ItemComponent();

              itemComponent.setSequence(revenueLine.getRdaPosition());

              if (Strings.isNotBlank(revenueLine.getNonBillRevCode())
                  || Strings.isNotBlank(revenueLine.getRevCd())) {
                CodeableConcept revenue = new CodeableConcept();

                if (Strings.isNotBlank(revenueLine.getNonBillRevCode())) {
                  revenue.setCoding(
                      List.of(
                          new Coding(
                              BBCodingSystems.FISS.REV_CD, revenueLine.getNonBillRevCode(), null)));
                }

                if (Strings.isNotBlank(revenueLine.getRevCd())) {
                  revenue.addExtension(
                      BBCodingSystems.FISS.NON_BILL_REV_CODE,
                      new CodeableConcept(
                          new Coding(
                              BBCodingSystems.FISS.NON_BILL_REV_CODE,
                              revenueLine.getRevCd(),
                              null)));
                }

                itemComponent.setRevenue(revenue);
              }

              if (revenueLine.getRevUnitsBilled() != null) {
                itemComponent.setQuantity(new Quantity(revenueLine.getRevUnitsBilled()));
              }

              if (revenueLine.getRevServUnitCnt() != null) {
                itemComponent.addExtension(
                    BBCodingSystems.FISS.REV_SERV_UNIT_CNT,
                    new Quantity(revenueLine.getRevServUnitCnt()));
              }

              LocalDate serviceDate = revenueLine.getServiceDate();
              if (serviceDate != null) {
                itemComponent.setServiced(new DateType(localDateToDate(serviceDate)));
              }

              if (Strings.isNotBlank(revenueLine.getHcpcCd())
                  || Strings.isNotBlank(revenueLine.getApcHcpcsApc())
                  || Strings.isNotBlank(revenueLine.getNdc())) {
                CodeableConcept productOrService = new CodeableConcept();
                List<Coding> codings = new ArrayList<>();

                if (Strings.isNotBlank(revenueLine.getHcpcCd())) {
                  codings.add(
                      new Coding(
                          TransformerConstants.CODING_SYSTEM_CARIN_HCPCS,
                          revenueLine.getHcpcCd(),
                          null));
                  productOrService.setCoding(codings);
                }

                if (Strings.isNotBlank(revenueLine.getApcHcpcsApc())) {
                  productOrService.addExtension(
                      BBCodingSystems.FISS.APC_HCPCS_APC,
                      new CodeableConcept(
                          new Coding(
                              BBCodingSystems.FISS.APC_HCPCS_APC,
                              revenueLine.getApcHcpcsApc(),
                              null)));
                }

                itemComponent.setProductOrService(productOrService);

                if (Strings.isNotBlank(revenueLine.getNdc())) {
                  Claim.DetailComponent detailComponent = new Claim.DetailComponent();
                  int sequenceNumber = 1;
                  detailComponent.setSequence(sequenceNumber);
                  detailComponent.setProductOrService(
                      new CodeableConcept(
                          new Coding(TransformerConstants.CODING_NDC, revenueLine.getNdc(), null)));
                }
              }

              if (Strings.isNotBlank(revenueLine.getHcpcInd())) {
                itemComponent.addExtension(
                    BBCodingSystems.FISS.HCPC_IND,
                    new CodeableConcept(
                        new Coding(BBCodingSystems.FISS.HCPC_IND, revenueLine.getHcpcInd(), null)));
              }

              itemComponent.addModifier(createModifierCoding(revenueLine.getHcpcModifier(), "1"));
              itemComponent.addModifier(createModifierCoding(revenueLine.getHcpcModifier2(), "2"));
              itemComponent.addModifier(createModifierCoding(revenueLine.getHcpcModifier3(), "3"));
              itemComponent.addModifier(createModifierCoding(revenueLine.getHcpcModifier4(), "4"));

              if (Strings.isNotBlank(revenueLine.getNdc())
                  && Strings.isNotBlank(revenueLine.getNdcQty())) {
                Claim.DetailComponent detailComponent = new Claim.DetailComponent();
                Quantity quantity = new Quantity();
                int sequenceNumber = 1;
                detailComponent.setSequence(sequenceNumber);
                detailComponent.setProductOrService(
                    new CodeableConcept(
                        new Coding(TransformerConstants.CODING_NDC, revenueLine.getNdc(), null)));

                try {
                  quantity.setValue(Double.parseDouble(revenueLine.getNdcQty()));
                } catch (NumberFormatException ex) {
                  // If the NDC_QTY isn't a valid number, do not set quantity value. Awaiting
                  // upstream RDA test data changes
                  log.error("Failed to parse ndcQty as a number: message={}", ex.getMessage(), ex);
                }

                if (Strings.isNotBlank(revenueLine.getNdcQtyQual())) {
                  quantity.setUnit(revenueLine.getNdcQtyQual());
                  quantity.setSystem(TransformerConstants.CODING_SYSTEM_UCUM);

                  switch (revenueLine.getNdcQtyQual()) {
                    case TransformerConstants.CODING_SYSTEM_UCUM_F2 ->
                        quantity.setCode(TransformerConstants.CODING_SYSTEM_UCUM_F2_CODE);
                    case TransformerConstants.CODING_SYSTEM_UCUM_GR ->
                        quantity.setCode(TransformerConstants.CODING_SYSTEM_UCUM_GR_CODE);
                    case TransformerConstants.CODING_SYSTEM_UCUM_ML ->
                        quantity.setCode(TransformerConstants.CODING_SYSTEM_UCUM_ML_CODE);
                    case TransformerConstants.CODING_SYSTEM_UCUM_ME ->
                        quantity.setCode(TransformerConstants.CODING_SYSTEM_UCUM_ME_CODE);
                    case TransformerConstants.CODING_SYSTEM_UCUM_UN ->
                        quantity.setCode(TransformerConstants.CODING_SYSTEM_UCUM_UN_CODE);
                  }
                }

                detailComponent.setQuantity(quantity);
                itemComponent.addDetail(detailComponent);
              }

              return itemComponent;
            })
        .toList();
  }

  /**
   * Creates FHIR components for given modifier data.
   *
   * @param modifierValue The modifier code value to use.
   * @param version The modifier version to use.
   * @return The created FHIR component for modifier data.
   */
  private CodeableConcept createModifierCoding(String modifierValue, String version) {
    CodeableConcept modifier;

    if (Strings.isNotBlank(modifierValue)) {
      modifier =
          new CodeableConcept(
              new Coding(TransformerConstants.CODING_SYSTEM_HCPCS, modifierValue, null)
                  .setVersion(version));
    } else {
      modifier = null;
    }

    return modifier;
  }
}
