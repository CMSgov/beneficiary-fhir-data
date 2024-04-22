package gov.cms.bfd.server.war.r4.providers.pac;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissPayer;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBSupportingInfoType;
import gov.cms.bfd.server.war.r4.providers.pac.common.AbstractTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.FissTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
import org.springframework.stereotype.Component;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
@Component
public class FissClaimTransformerV2 extends AbstractTransformerV2
    implements ResourceTransformer<Claim> {

  /** Date used to determine if an ICD code is ICD9 (before date) or ICD10 (on or after date). */
  private static final LocalDate ICD_9_CUTOFF_DATE = LocalDate.of(2015, 10, 1);

  /** The MEDICARE constant. */
  private static final String MEDICARE = "MEDICARE";

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

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
   */
  public FissClaimTransformerV2(MetricRegistry metricRegistry) {
    this.metricRegistry = requireNonNull(metricRegistry);
  }

  /**
   * Transforms a claim entity into a FHIR {@link Claim}.
   *
   * @param claimEntity the FISS {@link RdaFissClaim} to transform
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  @Trace
  public Claim transform(Object claimEntity, boolean includeTaxNumbers) {
    if (!(claimEntity instanceof RdaFissClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((RdaFissClaim) claimEntity, includeTaxNumbers);
    }
  }

  /**
   * Transforms a {@link RdaFissClaim} into a FHIR {@link Claim}.
   *
   * @param claimGroup the {@link RdaFissClaim} to transform
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @return a FHIR {@link Claim} resource that represents the specified {@link RdaFissClaim}
   */
  private Claim transformClaim(RdaFissClaim claimGroup, boolean includeTaxNumbers) {
    Claim claim = new Claim();

    boolean isIcd9 =
        claimGroup.getStmtCovToDate() != null
            && claimGroup.getStmtCovToDate().isBefore(ICD_9_CUTOFF_DATE);

    claim.setId("f-" + claimGroup.getClaimId());
    claim.setContained(
        List.of(
            FissTransformerV2.getContainedPatient(claimGroup),
            getContainedProvider(claimGroup, includeTaxNumbers)));
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
    if (Strings.isNotBlank(claimGroup.getAdmTypCd())) {
      claim.setSubType(
          createCodeableConcept(
              CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_IP_ADMSN_TYPE_CD),
              claimGroup.getAdmTypCd()));
    }

    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));
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
    }

    return supportingInfo;
  }

  /**
   * Parses data from the {@link RdaFissClaim} to create a provider {@link Resource}.
   *
   * @param claimGroup The {@link RdaFissClaim} object to parse data from.
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @return The provider {@link Resource} object created from the parsed data.
   */
  private Resource getContainedProvider(RdaFissClaim claimGroup, boolean includeTaxNumbers) {
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

    if (includeTaxNumbers) {
      addFedTaxNumberIdentifier(
          organization, BBCodingSystems.FISS.TAX_NUM, claimGroup.getFedTaxNumber());
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

    return ObjectUtils.defaultIfNull(claimGroup.getDiagCodes(), List.<RdaFissDiagnosisCode>of())
        .stream()
        .map(
            diagnosisCode -> {
              Claim.DiagnosisComponent component;

              if (Strings.isNotBlank(diagnosisCode.getDiagCd2())) {
                component =
                    new Claim.DiagnosisComponent()
                        .setSequence(diagnosisCode.getRdaPosition())
                        .setDiagnosis(createCodeableConcept(icdSystem, diagnosisCode.getDiagCd2()));

                if (Strings.isNotBlank(diagnosisCode.getDiagPoaInd())) { // Present on Admission
                  component.setOnAdmission(
                      createCodeableConcept(
                          BBCodingSystems.FISS.DIAG_POA_IND,
                          diagnosisCode.getDiagPoaInd().toLowerCase()));
                }

                if (codesAreEqual(claimGroup.getAdmitDiagCode(), diagnosisCode.getDiagCd2())) {
                  component.setType(List.of(createCodeableConcept(ExDiagnosistype.ADMITTING)));
                }
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

                if (Strings.isNotBlank(revenueLine.getHcpcCd())) {
                  productOrService.setCoding(
                      List.of(
                          new Coding(
                              CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD),
                              revenueLine.getHcpcCd(),
                              null)));
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

                if (Strings.isNotBlank(revenueLine.getNdc())) {
                  productOrService.setCoding(
                      List.of(
                          new Coding(TransformerConstants.CODING_NDC, revenueLine.getNdc(), null)));
                }

                itemComponent.setProductOrService(productOrService);
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

              if (Strings.isNotBlank(revenueLine.getNdcQty())
                  && Strings.isNotBlank(revenueLine.getNdcQtyQual())) {
                Quantity quantity = new Quantity();
                quantity.setValue((long) Double.parseDouble(revenueLine.getNdcQty()));
                quantity.setUnit(revenueLine.getNdcQtyQual());
                quantity.setSystem(TransformerConstants.CODING_SYSTEM_UCUM);
                quantity.setCode(revenueLine.getNdcQtyQual());

                itemComponent.setQuantity(quantity);
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
