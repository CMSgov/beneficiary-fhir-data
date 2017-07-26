package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
import org.hl7.fhir.dstu3.model.Coverage.GroupComponent;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DetailComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestStatus;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.dstu3.model.codesystems.Adjudication;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.SharedDataManager;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.InvalidRifValueException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.UnsupportedRifFileTypeException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.UnsupportedRifRecordActionException;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup.CarrierClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup.DMEClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup.HHAClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup.HospiceClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode.IcdVersion;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup.InpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup.SNFClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.parse.InvalidRifFileFormatException;

/**
 * Handles the translation from source/CCW {@link RifRecordEvent}s data into
 * FHIR {@link TransformedBundle}s.
 */
public final class DataTransformer {
	static final String EXTENSION_US_CORE_RACE = "http://hl7.org/fhir/StructureDefinition/us-core-race";

	static final String EXTENSION_CMS_CLAIM_TYPE = "http://bluebutton.cms.hhs.gov/extensions#claimType";

	static final String EXTENSION_CMS_DIAGNOSIS_GROUP = "http://bluebutton.cms.hhs.gov/extensions#diagnosisRelatedGroupCode";

	static final String EXTENSION_CMS_ADMITTING_DIAGNOSIS = "http://bluebutton.cms.hhs.gov/extensions#admittingDiagnosis";

	static final String EXTENSION_CMS_OTHER_PHYSICIAN = "http://bluebutton.cms.hhs.gov/extensions#otherPhysician";

	static final String EXTENSION_CMS_OPERATING_PHYSICIAN = "http://bluebutton.cms.hhs.gov/extensions#operatingPhysician";

	static final String EXTENSION_CMS_ATTENDING_PHYSICIAN = "http://bluebutton.cms.hhs.gov/extensions#attendingPhysician";

	static final String EXTENSION_CMS_DIAGNOSIS_LINK_ID = "http://bluebutton.cms.hhs.gov/extensions#diagnosisLinkId";

	static final String EXTENSION_CMS_HCT_OR_HGB_RESULTS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbrs.txt";

	static final BigDecimal ZERO = new BigDecimal(0);

	static final String COVERAGE_PLAN = "Medicare";

	/**
	 * The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()}
	 * value for Part A.
	 */
	public static final String COVERAGE_PLAN_PART_A = "Part A";

	/**
	 * The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()}
	 * value for Part B.
	 */
	public static final String COVERAGE_PLAN_PART_B = "Part B";

	/**
	 * The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()}
	 * value for Part D.
	 */
	public static final String COVERAGE_PLAN_PART_D = "Part D";

	static final String CODING_SYSTEM_RACE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/race.txt";

	static final String CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orec.txt";

	static final String CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/crec.txt";

	/**
	 * A CMS-controlled standard. More info here: <a href=
	 * "https://en.wikipedia.org/wiki/Healthcare_Common_Procedure_Coding_System">
	 * Healthcare Common Procedure Coding System</a>.
	 */
	static final String CODING_SYSTEM_HCPCS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_cd.txt";

	static final String CODING_SYSTEM_HCPCS_YR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_yr.txt";

	static final String HCPCS_INITIAL_MODIFIER_CODE1 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd1.txt";

	static final String HCPCS_INITIAL_MODIFIER_CODE2 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd2.txt";

	static final String HCPCS_INITIAL_MODIFIER_CODE3 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd3.txt";

	static final String HCPCS_INITIAL_MODIFIER_CODE4 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd4.txt";

	static final String LINE_1ST_EXPNS_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/expnsdt1.txt";

	static final String LINE_LAST_EXPNS_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/expnsdt2.txt";

	static final String CODING_SYSTEM_BETOS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/betos.txt";

	static final String CODING_SYSTEM_ICD9_DIAG = "http://hl7.org/fhir/sid/icd-9-cm/diagnosis";

	/**
	 * The United States National Provider Identifier, as available at
	 * <a href="http://download.cms.gov/nppes/NPI_Files.html">NPI/NPPES File</a>
	 * .
	 */
	static final String CODING_SYSTEM_NPI_US = "http://hl7.org/fhir/sid/us-npi";

	static final String CODING_SYSTEM_CLIA_LAB_NUM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_line_clia_lab_num.txt";

	static final String CODING_SYSTEM_CARE_TEAM_ROLE = "http://build.fhir.org/valueset-claim-careteamrole.html";

	static final String CARE_TEAM_ROLE_PRIMARY = "Primary";

	static final String CARE_TEAM_ROLE_ASSISTING = "Assist";

	static final String CARE_TEAM_ROLE_OTHER = "Other";

	static final String CODING_SYSTEM_ADJUDICATION_FHIR = "http://hl7.org/fhir/adjudication";

	static final String CODING_SYSTEM_CMS_CLAIM_TYPES = "http://bluebutton.cms.hhs.gov/coding#claimType";

	static final String PROVIDER_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sup_npi.txt";

	static final String ORGANIZATION_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orgnpinm.txt";

	static final String PROVIDER_STATE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvstate.txt";

	/**
	 * The CMS-custom {@link Coding#getSystem()} value for Medicare
	 * {@link Adjudication}s.
	 */
	static final String CODING_SYSTEM_ADJUDICATION_CMS = "CMS Adjudications";

	static final String CODING_SYSTEM_PHYSICIAN_ASSISTANT_ADJUDICATION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/astnt_cd.txt";

	static final String CODING_SYSTEM_CMS_LINE_PROCESSING_INDICATOR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcngind.txt";

	static final String CODING_REVENUE_CENTER_RENDER_PHY_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rndrng_physn_npi.txt";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_id.txt">
	 * CCW Data Dictionary: BENE_ID</a>.
	 */
	public static final String CODING_SYSTEM_CCW_BENE_ID = "CCW.BENE_ID";

	/**
	 * The {@link Identifier#getSystem()} used in {@link Patient} resources to
	 * store a one-way cryptographic hash of each Medicare beneficiaries' HICN.
	 * Note that, with the SSNRI initiative, CMS is planning to move away from
	 * HICNs. However, HICNs are still the primary/only Medicare identifier for
	 * now.
	 */
	public static final String CODING_SYSTEM_CCW_BENE_HICN_HASH = "http://bluebutton.cms.hhs.gov/identifier#hicnHash";

	public static final String CODING_SYSTEM_CCW_CLAIM_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_id.txt";

	public static final String CODING_SYSTEM_CCW_CLAIM_GRP_ID = "http://bluebutton.cms.hhs.gov/identifier#claimGroup";

	public static final String CODING_SYSTEM_CCW_CLAIM_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_type.txt";

	public static final String CODING_SYSTEM_PROVIDER_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provider.txt";

	public static final String CODING_SYSTEM_QUERY_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/query_cd.txt";

	public static final String CODING_SYSTEM_PATIENT_DISCHARGE_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/stus_cd.txt";

	public static final String CODING_SYSTEM_FREQUENCY_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/freq_cd.txt";

	public static final String CODING_SYSTEM_PRIMARY_PAYER_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpay_cd.txt";

	public static final String CODING_SYSTEM_PRICING_STATE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_st.txt";

	public static final String CODING_SYSTEM_SUPPLIER_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sup_type.txt";

	public static final String CODING_SYSTEM_SCREEN_SAVINGS_AMT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/scrnsvgs.txt";

	public static final String CODING_SYSTEM_MTUS_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mtus_cnt.txt";

	public static final String CODING_SYSTEM_MTUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mtus_ind.txt";

	public static final String CODING_SYSTEM_HCT_HGB_TEST_RESULTS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbrs.txt";

	public static final String CODING_SYSTEM_HCT_HGB_TEST_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbtp.txt";

	public static final String CODING_SYSTEM_MCO_PAID_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mcopdsw.txt";

	public static final String CODING_SYSTEM_HHA_LUPA_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lupaind.txt";

	public static final String CODING_SYSTEM_HHA_REFERRAL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hha_rfrl.txt";

	public static final String CODING_SYSTEM_HHA_VISIT_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/visitcnt.txt";

	public static final String CODING_SYSTEM_HHA_CARE_START_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hhstrtdt.txt";

	public static final String CODING_SYSTEM_PATIENT_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptntstus.txt";

	public static final String CODING_SYSTEM_UTILIZATION_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/util_day.txt";

	public static final String CODING_SYSTEM_COINSURANCE_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_day.txt";

	public static final String CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nutilday.txt";

	public static final String CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bldfrnsh.txt";

	public static final String CODING_SYSTEM_BENEFICIARY_DISCHARGE_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dschrgdt.txt";

	public static final String CODING_SYSTEM_QUALIFIED_STAY_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qlfrfrom.txt";

	public static final String CODING_SYSTEM_NONCOVERED_STAY_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncovfrom.txt";

	public static final String CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/revdedcd.txt";

	public static final String CODING_SYSTEM_COVERED_CARE_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carethru.txt";

	public static final String CODING_SYSTEM_BENEFITS_EXHAUSTED_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/exhst_dt.txt";

	public static final String CODING_SYSTEM_ADMISSION_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/admsn_dt.txt";

	public static final String CODING_SYSTEM_ADMISSION_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/type_adm.txt";

	public static final String CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drg_cd.txt";

	public static final String CODING_SYSTEM_SOURCE_ADMISSION_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/src_adms.txt";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pde_id.txt">
	 * CCW Data Dictionary: PDE_ID</a>.
	 */
	public static final String CODING_SYSTEM_CCW_PDE_ID = "CCW.PDE_ID";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_srvc_rfrnc_num.txt">
	 * CCW Data Dictionary: RX_SRVC_RFRNC_NUM</a>.
	 */
	static final String CODING_SYSTEM_RX_SRVC_RFRNC_NUM = "CCW.RX_SRVC_RFRNC_NUM";

	static final String CODING_SYSTEM_RX_DAW_PRODUCT_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/daw_prod_slctn_cd.txt";

	static final String CODING_SYSTEM_RX_DISPENSE_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dspnsng_stus_cd.txt";

	static final String CODING_SYSTEM_RX_COVERAGE_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt";

	static final String CODING_SYSTEM_RX_ADJUSTMENT_DEL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/adjstmt_dltn_cd.txt";

	static final String CODING_SYSTEM_RX_NON_STD_FORMAT_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nstd_frmt_cd.txt";

	static final String CODING_SYSTEM_RX_PRICING_EXCEPTION_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_excptn_cd.txt";

	static final String CODING_SYSTEM_RX_CATASTROPHIC_COV_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ctstrphc_cvrg_cd.txt";

	static final String CODING_SYSTEM_RX_PRESCRIPTION_ORIGIN_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_orgn_cd.txt";

	static final String CODING_SYSTEM_RX_BRAND_GENERIC_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/brnd_gnrc_cd.txt";

	static final String CODING_SYSTEM_RX_PHARMACY_SVC_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/phrmcy_srvc_type_cd.txt";

	static final String CODING_SYSTEM_RX_PATIENT_RESIDENCE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptnt_rsdnc_cd.txt";

	static final String CODING_SYSTEM_RX_SUBMISSION_CLARIFICATION_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/submsn_clr_cd.txt";

	static final String CODING_SYSTEM_CCW_ESRD_INDICATOR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/esrd_ind.txt";

	static final String CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ms_cd.txt";

	static final String CODING_SYSTEM_CCW_BENE_PTA_TRMNTN_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/a_trm_cd.txt";

	static final String CODING_SYSTEM_CCW_BENE_PTB_TRMNTN_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/b_trm_cd.txt";

	static final String CODING_SYSTEM_CCW_RECORD_ID_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ric_cd.txt";

	static final String CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_num.txt";

	static final String CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtdnlcd.txt";

	static final String CLAIM_CLINICAL_TRIAL_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prv_type.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcfaspcl.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prtcptg.txt";

	static final String CODING_SYSTEM_CCW_PROCESSING_INDICATOR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcngind.txt";

	static final String CODING_SYSTEM_CCW_PAYMENT_80_100_INDICATOR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtindsw.txt";

	static final String CODING_SYSTEM_CCW_DEDUCTIBLE_INDICATOR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_sw.txt";

	static final String CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION = "Debit accepted";

	static final String CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ccltrnum.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvstate.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provzip.txt";

	static final String CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nopay_cd.txt";

	static final String CODING_SYSTEM_CCW_INP_POA_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw1.txt";

	static final String CODING_SYSTEM_CCW_FACILITY_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fac_type.txt";

	static final String CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typesrvc.txt";

	static final String CLAIM_HOSPICE_START_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hspcstrt.txt";

	static final String CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/at_npi.txt";

	static final String CODING_SYSTEM_CCW_OPERATING_PHYSICIAN_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/op_npi.txt";

	static final String CODING_SYSTEM_CCW_OTHER_PHYSICIAN_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ot_npi.txt";

	static final String CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/phrmcy_srvc_type_cd.txt";

	static final String CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/asgmntcd.txt";

	static final String CODING_SYSTEM_CCW_PRICING_LOCALITY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lclty_cd.txt";

	static final String CODING_SYSTEM_PDE_PLAN_CONTRACT_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_cntrct_rec_id.txt";

	static final String CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_pbp_rec_num.txt";

	static final String CODING_SYSTEM_FHIR_ACT = "http://hl7.org/fhir/v3/ActCode";

	static final String CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt";

	static final String CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typcsrvcb.txt";

	static final String CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_sw.txt";

	static final String CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtindsw.txt";

	static final String CODING_SYSTEM_CMS_HCT_OR_HGB_TEST_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbtp.txt";

	static final String CODED_ADJUDICATION_BENEFICIARY_PRIMARY_PAYER_PAID = "Beneficiary Primary Payer Paid Amount";

	static final String CODED_ADJUDICATION_PAYMENT = "NCH Payment Amount";

	static final String CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT = "Payment Amount to Beneficiary";

	static final String CODED_ADJUDICATION_DEDUCTIBLE = "Beneficiary Deductible Amount";

	static final String CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT = "Primary Payer Paid Amount";

	static final String CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT = "Coinsurance Amount";

	static final String CODED_ADJUDICATION_LINE_PRIMARY_PAYER_ALLOWED_CHARGE = "Primary Payer Allowed Charge Amount";

	static final String CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT = "Submitted Charge Amount";

	static final String CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT = "Purchase Price Amount";

	static final String CODED_ADJUDICATION_ALLOWED_CHARGE = "Allowed Charge Amount";

	static final String CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR = "Line Processing Indicator Code";

	static final String CODED_ADJUDICATION_PASS_THRU_PER_DIEM_AMOUNT = "Line Allowed Charge Amount";

	static final String CODED_ADJUDICATION_BLOOD_DEDUCTIBLE = "Blood Deductible Amount";

	static final String CODED_ADJUDICATION_CASH_DEDUCTIBLE = "Cash Deductible Amount";

	static final String CODED_ADJUDICATION_WAGE_ADJ_COINSURANCE_AMOUNT = "Wage Adj Coinsurance Amount";

	static final String CODED_ADJUDICATION_REDUCED_COINSURANCE_AMOUNT = "Reduced Coinsurance Amount";

	static final String CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT = "Provider Payment Amount";

	static final String CODED_ADJUDICATION_PATIENT_RESPONSIBILITY_AMOUNT = "Patient Responsibility Amount";

	static final String CODED_ADJUDICATION_PROFESSIONAL_COMP_CHARGE = "Professional Component Charge Amount";

	static final String CODED_ADJUDICATION_NONCOVERED_CHARGE = "Noncovered Charge";

	static final String CODED_ADJUDICATION_TOTAL_DEDUCTION_AMOUNT = "Total Deduction Amount";

	static final String CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT = "Line Total Charge Amount";

	static final String CODED_ADJUDICATION_PHYSICIAN_ASSISTANT = "Carrier Line Reduced Payment Physician Assistant Code";

	static final String CODED_ADJUDICATION_PAYMENT_B = "Provider Payment Amount";

	static final String CODED_ADJUDICATION_PASS_THROUGH_PER_DIEM_AMOUNT = "Claim Pass Thru Per Diem Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_INPATIENT_DEDUCTIBLE = "NCH Beneficiary Inpatient Deductible Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_A_COINSURANCE_LIABILITY = "NCH Beneficiary Part A Coinsurance Liability Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptnt_pay_amt.txt">
	 * CCW Data Dictionary: PTNT_PAY_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_PATIENT_PAY = "Patient Pay Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tot_rx_cst_amt.txt">
	 * CCW Data Dictionary: TOT_RX_CST_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_TOTAL_COST = "Total Prescription Cost";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT = "NCH Beneficiary Blood Deductible Liability Amount";

	static final String CODED_ADJUDICATION_NCH_PRIMARY_PAYER_CLAIM_PAID_AMOUNT = "NCH Primary Payer Claim Paid Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE = "NCH Beneficiary Part B Deductible Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_COINSURANCE = "NCH Beneficiary Part B Coinsurance Amount";

	static final String CODED_ADJUDICATION_1ST_ANSI_CD = "Revenue Center 1st ANSI Code";

	static final String CODED_ADJUDICATION_2ND_ANSI_CD = "Revenue Center 2nd ANSI Code";

	static final String CODED_ADJUDICATION_3RD_ANSI_CD = "Revenue Center 3rd ANSI Code";

	static final String CODED_ADJUDICATION_4TH_ANSI_CD = "Revenue Center 4th ANSI Code";

	static final String CODED_ADJUDICATION_RATE_AMOUNT = "Revenue Center Rate Amount";

	static final String CODED_ADJUDICATION_1ST_MSP_AMOUNT = "Revenue Center 1st Medicare Secondary Payer (MSP) Paid Amount";

	static final String CODED_ADJUDICATION_2ND_MSP_AMOUNT = "Revenue Center 2nd Medicare Secondary Payer (MSP) Paid Amount";

	static final String CODING_BENEFIT_BALANCE_URL = "http://build.fhir.org/explanationofbenefit-definitions.html#ExplanationOfBenefit.benefitBalance.category";

	static final String CODING_CLAIM_PASS_THRU_PER_DIEM_AMT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/per_diem.txt";

	static final String CODING_BENEFIT_DEDUCTIBLE_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_amt.txt";

	static final String BENEFIT_BALANCE_TYPE = "http://bluebutton.cms.hhs.gov/coding#benefitBalanceType";

	static final String BENEFIT_COVERAGE_DATE = "Benefit Coverage Date";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt">
	 * CCW Data Dictionary: DRUG_CVRG_STUS_CD</a>.
	 */
	static final String CODED_ADJUDICATION_PART_D_COVERED = "Part D Covered";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt">
	 * CCW Data Dictionary: DRUG_CVRG_STUS_CD</a>.
	 */
	static final String CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT = "Part D Supplemental drugs (reported by plans that provide Enhanced Alternative coverage)";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt">
	 * CCW Data Dictionary: DRUG_CVRG_STUS_CD</a>.
	 */
	static final String CODED_ADJUDICATION_PART_D_NONCOVERED_OTC = "Part D Over-the-counter drugs";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/othr_troop_amt.txt">
	 * CCW Data Dictionary: OTHR_TROOP_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_OTHER_TROOP_AMOUNT = "Other True Out-of-Pocket (TrOOP) Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lics_amt.txt">
	 * CCW Data Dictionary: LICS_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT = "Part D Low Income Subsidy (LICS) Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plro_amt.txt">
	 * CCW Data Dictionary: PLRO_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT = "Reduction in patient liability due to payments by other payers (PLRO) Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prscrbr_id.txt">
	 * CCW Data Dictionary: PRSCRBR_ID</a>.
	 */
	static final String CODED_PRESCRIBER_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prscrbr_id.txt";

	static final String CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT = "Medicare Coverage Gap Discount Amount";

	static final String CODED_ADJUDICATION_GDCB = "Gross Drug Cost Below Out-of-Pocket Threshold (GDCB)";

	static final String CODED_ADJUDICATION_GDCA = "Gross Drug Cost Above Out-of-Pocket Threshold (GDCA)";

	static final String CODING_SYSTEM_FHIR_EOB_ITEM_TYPE = "http://hl7.org/fhir/ValueSet/v3-ActInvoiceGroupCode";

	static final String CODING_SYSTEM_FHIR_CLAIM_TYPE = "http://build.fhir.org/valueset-claim-type.html";

	static final String CODING_SYSTEM_PDE_DAYS_SUPPLY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/days_suply_num.txt";

	static final String CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS = "CSPINV";

	static final String CODING_SYSTEM_MONEY = "urn:std:iso:4217";

	static final String CODING_SYSTEM_MONEY_US = "USD";

	static final String CODING_SYSTEM_REVENUE_CENTER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr.txt";

	static final String CODING_SYSTEM_NDC = "https://www.accessdata.fda.gov/scripts/cder/ndc";

	static final String CODING_SYSTEM_NDC_QLFR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr_ndc_qty_qlfr_cd.txt";

	static final String LINE_PLACE_OF_SERVICE_CODE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt";

	static final String CODING_NCH_PRIMARY_PAYER_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpayamt.txt";

	static final String CODING_NCH_BENEFIT_COIN_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_amt.txt";

	static final String CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/blddedam.txt";

	static final String CODING_NCH_BEN_PART_B_DED_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_ded.txt";

	static final String CODING_NCH_BEN_PART_B_COINSUR_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_coin.txt";

	static final String CODING_CLAIM_OUTPAT_PROVIDER_PAYMENT_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvdrpmt.txt";

	static final String CODING_CLAIM_OUTPAT_BEN__PAYMENT_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/benepmt.txt";

	static final String CODING_NCH_PROFFESIONAL_CHARGE_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pcchgamt.txt";

	static final String CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncchgamt.txt";

	static final String CODING_NCH_INPATIENT_TOTAL_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tdedamt.txt";

	static final String CODING_CLAIM_TOTAL_PPS_CAPITAL_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pps_cptl.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_fsp.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptloutl.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/disp_shr.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ime_amt.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_exp.txt";

	static final String CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hldhrmls.txt";

	static final String CODING_NCH_DRUG_OUTLIER_APPROVED_PAYMENT_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/outlrpmt.txt";

	private static final Logger LOGGER = LoggerFactory.getLogger(DataTransformer.class);

	private static SecretKeyFactory secretKeyFactory = null;

	private final LoadAppOptions options;

	/**
	 * Constructs a new {@link DataTransformer}.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 */
	public DataTransformer(LoadAppOptions options) {
		this.options = options;
	}

	/**
	 * @param rifStream
	 *            the stream of source {@link RifRecordEvent}s to be transformed
	 * @return the stream of FHIR resource {@link Bundle}s that is the result of
	 *         transforming the specified {@link RifRecordEvent}s
	 */
	@SuppressWarnings("unchecked")
	public Stream<TransformedBundle> transform(Stream<RifRecordEvent<?>> rifStream) {
		return rifStream.map(rifRecordEvent -> {
			if (rifRecordEvent.getRecord() instanceof BeneficiaryRow)
				return transformBeneficiary((RifRecordEvent<BeneficiaryRow>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof PartDEventRow)
				return transformPartDEvent((RifRecordEvent<PartDEventRow>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof CarrierClaimGroup)
				return transformCarrierClaim((RifRecordEvent<CarrierClaimGroup>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof InpatientClaimGroup)
				return transformInpatientClaim((RifRecordEvent<InpatientClaimGroup>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof OutpatientClaimGroup)
				return transformOutpatientClaim((RifRecordEvent<OutpatientClaimGroup>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof SNFClaimGroup)
				return transformSNFClaim((RifRecordEvent<SNFClaimGroup>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof HospiceClaimGroup)
				return transformHospiceClaim((RifRecordEvent<HospiceClaimGroup>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof HHAClaimGroup)
				return transformHHAClaim((RifRecordEvent<HHAClaimGroup>) rifRecordEvent);
			else if (rifRecordEvent.getRecord() instanceof DMEClaimGroup)
				return transformDMEClaim((RifRecordEvent<DMEClaimGroup>) rifRecordEvent);

			throw new UnsupportedRifFileTypeException("Unhandled record type: " + rifRecordEvent.getRecord());
		});
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformBeneficiary(RifRecordEvent<BeneficiaryRow> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("Beneficiary RIF record is null");
		BeneficiaryRow record = rifRecordEvent.getRecord();
		if (record.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(record.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		Patient beneficiary = new Patient();
		Identifier beneId = beneficiary.addIdentifier().setSystem(CODING_SYSTEM_CCW_BENE_ID)
				.setValue(record.beneficiaryId);
		String hicnHash = computeHicnHash(options, record.hicn);
		beneficiary.addIdentifier().setSystem(CODING_SYSTEM_CCW_BENE_HICN_HASH).setValue(hicnHash);
		beneficiary.addAddress().setState(record.stateCode).setDistrict(record.countyCode)
				.setPostalCode(record.postalCode);
		if (record.birthDate != null) {
			beneficiary.setBirthDate(convertToDate(record.birthDate));
		}
		switch (record.sex) {
		case ('M'):
			beneficiary.setGender((AdministrativeGender.MALE));
			break;
		case ('F'):
			beneficiary.setGender((AdministrativeGender.FEMALE));
			break;
		default:
			beneficiary.setGender((AdministrativeGender.UNKNOWN));
			break;
		}
		if (record.race.isPresent()) {
			addExtensionCoding(beneficiary, EXTENSION_US_CORE_RACE, CODING_SYSTEM_RACE, "" + record.race.get());
		}

		/*
		 * Has been decided that HICN will not be included in FHIR resources
		 */
		HumanName name = beneficiary.addName().addGiven(record.nameGiven)
				.setFamily(record.nameSurname).setUse(HumanName.NameUse.USUAL);
		if (record.nameMiddleInitial.isPresent())
			name.addGiven(String.valueOf(record.nameMiddleInitial.get()));
		Reference beneficiaryBundleReference = conditionalCreate(bundle, beneficiary, beneId);

		/*
		 * We don't have detailed enough data on this right now, so we'll just
		 * assume that everyone has Part A, B, and D.
		 */

		Coverage partA = new Coverage();
		if (record.partATerminationCode.isPresent() && record.partATerminationCode.get().equals('0'))
			partA.setStatus(CoverageStatus.ACTIVE);
		else
			partA.setStatus(CoverageStatus.CANCELLED);
		partA.getGrouping().setSubGroup(COVERAGE_PLAN).setSubPlan(COVERAGE_PLAN_PART_A);
		partA.setType(createCodeableConcept(COVERAGE_PLAN, COVERAGE_PLAN_PART_A));
		partA.addPayor(SharedDataManager.createReferenceToCms());
		partA.setBeneficiary(beneficiaryBundleReference);
		if (record.medicareEnrollmentStatusCode.isPresent()) {
			addExtensionCoding(partA, CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD, CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD,
					"" + record.medicareEnrollmentStatusCode.get());
		}
		if (record.entitlementCodeOriginal.isPresent()) {
			addExtensionCoding(partA, CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL,
					CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL, "" + record.entitlementCodeOriginal.get());
		}
		if (record.entitlementCodeCurrent.isPresent()) {
			addExtensionCoding(partA, CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT,
					CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT, "" + record.entitlementCodeCurrent.get());
		}
		if (record.endStageRenalDiseaseCode.isPresent()) {
			addExtensionCoding(partA, CODING_SYSTEM_CCW_ESRD_INDICATOR, CODING_SYSTEM_CCW_ESRD_INDICATOR,
					"" + record.endStageRenalDiseaseCode.get());
		}
		conditionalCreate(bundle, partA, referenceCoverage(record.beneficiaryId, partA.getGrouping().getSubPlan()));

		Coverage partB = new Coverage();
		if (record.partBTerminationCode.isPresent() && record.partBTerminationCode.get().equals('0'))
			partB.setStatus(CoverageStatus.ACTIVE);
		else
			partB.setStatus(CoverageStatus.CANCELLED);
		partB.getGrouping().setSubGroup(COVERAGE_PLAN).setSubPlan(COVERAGE_PLAN_PART_B);
		partB.setType(createCodeableConcept(COVERAGE_PLAN, COVERAGE_PLAN_PART_B));
		partB.addPayor(SharedDataManager.createReferenceToCms());
		partB.setBeneficiary(beneficiaryBundleReference);
		if (record.medicareEnrollmentStatusCode.isPresent()) {
			addExtensionCoding(partB, CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD, CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD,
					"" + record.medicareEnrollmentStatusCode.get());
		}
		conditionalCreate(bundle, partB, referenceCoverage(record.beneficiaryId, partB.getGrouping().getSubPlan()));

		Coverage partD = new Coverage();
		partD.getGrouping().setSubGroup(COVERAGE_PLAN).setSubPlan(COVERAGE_PLAN_PART_D);
		partD.setType(createCodeableConcept(COVERAGE_PLAN, COVERAGE_PLAN_PART_D));
		partD.addPayor(SharedDataManager.createReferenceToCms());
		partD.setBeneficiary(beneficiaryBundleReference);
		if (record.medicareEnrollmentStatusCode.isPresent()) {
			addExtensionCoding(partD, CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD, CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD,
					"" + record.medicareEnrollmentStatusCode.get());
		}
		conditionalCreate(bundle, partD, referenceCoverage(record.beneficiaryId, partD.getGrouping().getSubPlan()));

		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * Computes a one-way cryptographic hash of the specified HICN value. This
	 * is used as a secure means of identifying Medicare beneficiaries between
	 * the Blue Button API frontend and backend systems: the HICN is the only
	 * unique beneficiary identifier shared between those two systems.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @param hicn
	 *            the Medicare beneficiary HICN to be hashed
	 * @return a one-way cryptographic hash of the specified HICN value
	 */
	static String computeHicnHash(LoadAppOptions options, String hicn) {
		try {
			if (secretKeyFactory == null)
				secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}

		try {
			/*
			 * Our approach here is NOT using a salt, as salts must be randomly
			 * generated for each value to be hashed and then included in
			 * plaintext with the hash results. Random salts would prevent the
			 * Blue Button API frontend systems from being able to produce equal
			 * hashes for the same HICNs. Instead, we use a secret "pepper" that
			 * is shared out-of-band with the frontend. This value MUST be kept
			 * secret.
			 */
			byte[] salt = options.getHicnHashPepper();

			/*
			 * Bigger is better here as it reduces chances of collisions, but
			 * the equivalent Python Django hashing functions used by the
			 * frontend default to this value, so we'll go with it.
			 */
			int derivedKeyLength = 256;

			PBEKeySpec hicnKeySpec = new PBEKeySpec(hicn.toCharArray(), salt, options.getHicnHashIterations(),
					derivedKeyLength);
			SecretKey hicnSecret = secretKeyFactory.generateSecret(hicnKeySpec);
			String hexEncodedHash = Hex.encodeHexString(hicnSecret.getEncoded());

			return hexEncodedHash;
		} catch (InvalidKeySpecException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformPartDEvent(RifRecordEvent<PartDEventRow> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("PDE RIF record is null");
		PartDEventRow record = rifRecordEvent.getRecord();
		if (record.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(record.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_PDE_ID).setValue(record.partDEventId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(record.claimGroupId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_RX_SRVC_RFRNC_NUM)
				.setValue(String.valueOf(record.prescriptionReferenceNumber));

		eob.getType().addCoding(new Coding().setSystem(CODING_SYSTEM_FHIR_CLAIM_TYPE).setCode("pharmacy"));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		Reference patientRef = referencePatient(record.beneficiaryId);
		eob.setPatient(patientRef);
		if (record.paymentDate.isPresent()) {
			eob.getPayment().setDate(convertToDate(record.paymentDate.get()));
		}

		ItemComponent rxItem = eob.addItem();
		rxItem.setSequence(1);

		DetailComponent detail = new DetailComponent();
		switch (record.compoundCode) {
		case COMPOUNDED:
			/* Pharmacy dispense invoice for a compound */
			detail.getType().addCoding(new Coding().setSystem(CODING_SYSTEM_FHIR_ACT).setCode("RXCINV"));
			break;
		case NOT_COMPOUNDED:
			/*
			 * Pharmacy dispense invoice not involving a compound
			 */
			detail.getType().addCoding(new Coding().setSystem(CODING_SYSTEM_FHIR_ACT).setCode("RXDINV"));
			break;
		case NOT_SPECIFIED:
			/*
			 * Pharmacy dispense invoice not specified - do not set a value
			 */
			break;
		default:
			/*
			 * Unexpected value encountered - compound code should be either
			 * compounded or not compounded.
			 */
			throw new InvalidRifValueException(
					"Unexpected value encountered - compound code should be either compounded or not compounded: "
							+ record.compoundCode);
		}

		rxItem.addDetail(detail);

		rxItem.setServiced(new DateType().setValue(convertToDate(record.prescriptionFillDate)));

		Coding rxCategoryCoding;
		BigDecimal rxPaidAmountValue;
		switch (record.drugCoverageStatusCode) {
		/*
		 * If covered by Part D, use value from partDPlanCoveredPaidAmount
		 */
		case COVERED:
			rxCategoryCoding = new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
					.setCode(CODED_ADJUDICATION_PART_D_COVERED);
			rxPaidAmountValue = record.partDPlanCoveredPaidAmount;
			break;
		/*
		 * If not covered by Part D, use value from
		 * partDPlanNonCoveredPaidAmount. There are 2 categories of non-covered
		 * payment amounts: supplemental drugs covered by enhanced plans, and
		 * over the counter drugs that are covered only under specific
		 * circumstances.
		 */
		case SUPPLEMENTAL:
			rxCategoryCoding = new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
					.setCode(CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT);
			rxPaidAmountValue = record.partDPlanNonCoveredPaidAmount;
			break;
		case OVER_THE_COUNTER:
			rxCategoryCoding = new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
					.setCode(CODED_ADJUDICATION_PART_D_NONCOVERED_OTC);
			rxPaidAmountValue = record.partDPlanNonCoveredPaidAmount;
			break;
		default:
			/*
			 * Unexpected value encountered - drug coverage status code should
			 * be one of the three above.
			 */
			throw new InvalidRifValueException("Unexpected value encountered - drug coverage status code is invalid: "
					+ record.drugCoverageStatusCode);
		}
		CodeableConcept rxCategory = new CodeableConcept();
		rxCategory.addCoding(rxCategoryCoding);
		rxItem.addAdjudication().setCategory(rxCategory).getAmount().setSystem(CODING_SYSTEM_MONEY)
				.setCode(CODING_SYSTEM_MONEY_US).setValue(rxPaidAmountValue);

		rxItem.addAdjudication()
				.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_GDCB))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.grossCostBelowOutOfPocketThreshold);

		rxItem.addAdjudication()
				.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_GDCA))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.grossCostAboveOutOfPocketThreshold);

		rxItem.addAdjudication()
				.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PATIENT_PAY))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.patientPaidAmount);

		rxItem.addAdjudication()
				.setCategory(
						createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_OTHER_TROOP_AMOUNT))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.otherTrueOutOfPocketPaidAmount);

		rxItem.addAdjudication()
				.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
						CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.lowIncomeSubsidyPaidAmount);

		rxItem.addAdjudication()
				.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
						CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.patientLiabilityReductionOtherPaidAmount);

		rxItem.addAdjudication()
				.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_TOTAL_COST))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.totalPrescriptionCost);

		rxItem.addAdjudication()
				.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
						CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT))
				.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
				.setValue(record.gapDiscountAmount);

		if (record.prescriberIdQualifierCode == null || !record.prescriberIdQualifierCode.equalsIgnoreCase("01"))
			throw new InvalidRifValueException(
					"Prescriber ID Qualifier Code is invalid: " + record.prescriberIdQualifierCode);

		if (record.prescriberId != null) {
			addCareTeamPractitioner(eob, rxItem, CODING_SYSTEM_NPI_US, record.prescriberId, CARE_TEAM_ROLE_PRIMARY);
		}

		rxItem.setService(createCodeableConcept(CODING_SYSTEM_NDC, record.nationalDrugCode));

		SimpleQuantity quantityDispensed = new SimpleQuantity();
		quantityDispensed.setValue(record.quantityDispensed);
		rxItem.setQuantity(quantityDispensed);

		rxItem.addModifier(createCodeableConcept(CODING_SYSTEM_PDE_DAYS_SUPPLY, String.valueOf(record.daysSupply)));

		// CBBD-241 - This code was commented out because values other than "01"
		// were coming thru
		// such as "07". Need to discuss with Karl if this check needs to be
		// here - DEH

		/*
		 * if (record.serviceProviderIdQualiferCode == null ||
		 * !record.serviceProviderIdQualiferCode.equalsIgnoreCase("01")) throw
		 * new InvalidRifValueException(
		 * "Service Provider ID Qualifier Code is invalid: " +
		 * record.serviceProviderIdQualiferCode);
		 */

		if (!record.serviceProviderId.isEmpty()) {
			eob.setOrganization(createIdentifierReference(CODING_SYSTEM_NPI_US, record.serviceProviderId));
			eob.setFacility(createIdentifierReference(CODING_SYSTEM_NPI_US, record.serviceProviderId));
			addExtensionCoding(eob.getFacility(), CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD,
					CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD, record.pharmacyTypeCode);
		}

		Coverage coverage = new Coverage();
		coverage.addIdentifier().setSystem(CODING_SYSTEM_PDE_PLAN_CONTRACT_ID).setValue(record.planContractId);
		coverage.addIdentifier().setSystem(CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID)
				.setValue(record.planBenefitPackageId);
		coverage.getGrouping().setPlan(COVERAGE_PLAN).setSubPlan(COVERAGE_PLAN_PART_D);
		coverage.addPayor(SharedDataManager.createReferenceToCms());
		coverage.setBeneficiary(referencePatient(record.beneficiaryId));
		coverage.setSubscriber(referencePatient(record.beneficiaryId));
		coverage.setRelationship(createCodeableConcept("", "self"));
		eob.getInsurance().setCoverage(new Reference(coverage));

		/*
		 * Storing code values in EOB.information below
		 */

		addInformation(eob,
				createCodeableConcept(CODING_SYSTEM_RX_DAW_PRODUCT_CD, record.dispenseAsWrittenProductSelectionCode));

		if (record.dispensingStatusCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_DISPENSE_STATUS_CD,
					String.valueOf(record.dispensingStatusCode.get())));

		addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_COVERAGE_STATUS_CD,
				String.valueOf(record.drugCoverageStatusCode)));

		if (record.adjustmentDeletionCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_ADJUSTMENT_DEL_CD,
					String.valueOf(record.adjustmentDeletionCode.get())));

		if (record.nonstandardFormatCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_NON_STD_FORMAT_CD,
					String.valueOf(record.nonstandardFormatCode.get())));

		if (record.pricingExceptionCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_PRICING_EXCEPTION_CD,
					String.valueOf(record.pricingExceptionCode.get())));

		if (record.catastrophicCoverageCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_CATASTROPHIC_COV_CD,
					String.valueOf(record.catastrophicCoverageCode.get())));

		if (record.prescriptionOriginationCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_PRESCRIPTION_ORIGIN_CD,
					String.valueOf(record.prescriptionOriginationCode.get())));

		if (record.brandGenericCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_BRAND_GENERIC_CD,
					String.valueOf(record.brandGenericCode.get())));

		addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_PHARMACY_SVC_TYPE_CD, record.pharmacyTypeCode));

		addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_PATIENT_RESIDENCE_CD, record.patientResidenceCode));

		if (record.submissionClarificationCode.isPresent())
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_RX_SUBMISSION_CLARIFICATION_CD,
					record.submissionClarificationCode.get()));

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformCarrierClaim(RifRecordEvent<CarrierClaimGroup> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("Carrier RIF record is null");
		CarrierClaimGroup claimGroup = rifRecordEvent.getRecord();
		if (claimGroup.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(claimGroup.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_ID).setValue(claimGroup.claimId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(claimGroup.claimGroupId);
		eob.getInsurance().setCoverage(referenceCoverage(claimGroup.beneficiaryId, COVERAGE_PLAN_PART_B));
		eob.setPatient(referencePatient(claimGroup.beneficiaryId));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		eob.setType(createCodeableConcept(CODING_SYSTEM_CCW_CLAIM_TYPE, claimGroup.claimTypeCode));
		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_RECORD_ID_CD, CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.nearLineRecordIdCode));

		validatePeriodDates(claimGroup.dateFrom, claimGroup.dateThrough);
		setPeriodStart(eob.getBillablePeriod(), claimGroup.dateFrom);
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.dateThrough);

		eob.setDisposition(CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION);
		addExtensionCoding(eob, CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER, CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER,
				claimGroup.carrierNumber);
		addExtensionCoding(eob, CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD, CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD,
				claimGroup.paymentDenialCode);
		eob.getPayment()
				.setAmount((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.paymentAmount));

		/*
		 * Referrals are represented as contained resources, because otherwise
		 * updating them would require an extra roundtrip to the server (can't
		 * think of an intelligent client-specified ID for them).
		 */
		if (claimGroup.referringPhysicianNpi.isPresent()) {
			ReferralRequest referral = new ReferralRequest();
			referral.setStatus(ReferralRequestStatus.COMPLETED);
			referral.setSubject(referencePatient(claimGroup.beneficiaryId));
			referral.setRequester(new ReferralRequestRequesterComponent(
					referencePractitioner(claimGroup.referringPhysicianNpi.get())));
			referral.addRecipient(referencePractitioner(claimGroup.referringPhysicianNpi.get()));
			// Set the ReferralRequest as a contained resource in the EOB:
			eob.setReferral(new Reference(referral));
		}

		if (claimGroup.providerAssignmentIndicator.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT, CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
					String.valueOf(claimGroup.providerAssignmentIndicator.get()));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (!claimGroup.providerPaymentAmount.equals(ZERO)) {
			BenefitComponent providerPaymentAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PAYMENT_B));
			providerPaymentAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.providerPaymentAmount));
			benefitBalances.getFinancial().add(providerPaymentAmount);
		}

		if (!claimGroup.beneficiaryPaymentAmount.equals(ZERO)) {
			BenefitComponent beneficiaryPaymentAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT));
			beneficiaryPaymentAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.beneficiaryPaymentAmount));
			benefitBalances.getFinancial().add(beneficiaryPaymentAmount);
		}

		if (!claimGroup.submittedChargeAmount.equals(ZERO)) {
			BenefitComponent submittedChargeAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT));
			submittedChargeAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.submittedChargeAmount));
			benefitBalances.getFinancial().add(submittedChargeAmount);
		}

		if (!claimGroup.allowedChargeAmount.equals(ZERO)) {
			BenefitComponent allowedChargeAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_ALLOWED_CHARGE));
			allowedChargeAmount
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.allowedChargeAmount));
			benefitBalances.getFinancial().add(allowedChargeAmount);
		}

		if (!claimGroup.beneficiaryPartBDeductAmount.equals(ZERO)) {
			BenefitComponent beneficiaryPartBDeductAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE));
			beneficiaryPartBDeductAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.beneficiaryPartBDeductAmount));
			benefitBalances.getFinancial().add(beneficiaryPartBDeductAmount);
		}

		if (claimGroup.diagnosisPrincipal.isPresent())
			addDiagnosisCode(eob, claimGroup.diagnosisPrincipal.get());

		for (IcdCode diagnosis : claimGroup.diagnosesAdditional)
			addDiagnosisCode(eob, diagnosis);

		if (claimGroup.clinicalTrialNumber.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
					CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER, claimGroup.clinicalTrialNumber.get());
		}

		for (CarrierClaimLine claimLine : claimGroup.lines) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.number);

			addExtensionCoding(item, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			/*
			 * Per Michelle at GDIT, and also Tony Dean at OEDA, the performing
			 * provider _should_ always be present. However, we've found some
			 * examples in production where it's not for some claim lines. (This
			 * is annoying, as it's present on other lines in the same claim,
			 * and the data indicates that the same NPI probably applies to the
			 * lines where it's not specified. Still, it's not safe to guess at
			 * this, so we'll leave it blank.)
			 */
			if (claimLine.performingPhysicianNpi.isPresent()) {
				ExplanationOfBenefit.CareTeamComponent performingCareTeamMember = addCareTeamPractitioner(eob, item,
						CODING_SYSTEM_NPI_US, claimLine.performingPhysicianNpi.get(), CARE_TEAM_ROLE_PRIMARY);
				performingCareTeamMember.setResponsible(true);

				/*
				 * The provider's "specialty" and "type" code are equivalent.
				 * However, the "specialty" codes are more granular, and seem to
				 * better match the example FHIR
				 * `http://hl7.org/fhir/ex-providerqualification` code set.
				 * Accordingly, we map the "specialty" codes to the
				 * `qualification` field here, and stick the "type" code into an
				 * extension. TODO: suggest that the spec allows more than one
				 * `qualification` entry.
				 */

				performingCareTeamMember
						.setQualification(createCodeableConcept(CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD,
								"" + claimLine.providerSpecialityCode.get()));
				addExtensionCoding(performingCareTeamMember, CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD,
						CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD, "" + claimLine.providerTypeCode);

				addExtensionCoding(performingCareTeamMember, CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						"" + claimLine.providerParticipatingIndCode.get());
				if (claimLine.organizationNpi.isPresent()) {
					addExtensionCoding(performingCareTeamMember, CODING_SYSTEM_NPI_US, CODING_SYSTEM_NPI_US,
							"" + claimLine.organizationNpi.get());
				}
			}

			item.setLocation(createCodeableConcept(CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION, claimLine.placeOfServiceCode));

			if (claimLine.providerStateCode.isPresent()) {
				addExtensionCoding(item.getLocation(), CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
						CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD, claimLine.providerStateCode.get());
			}

			if (claimLine.providerZipCode.isPresent()) {
				addExtensionCoding(item.getLocation(), CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD,
						CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD, claimLine.providerZipCode.get());
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_PHYSICIAN_ASSISTANT))
					.setReason(createCodeableConcept(CODING_SYSTEM_PHYSICIAN_ASSISTANT_ADJUDICATION,
							"" + claimLine.reducedPaymentPhysicianAsstCode));

			SimpleQuantity serviceCount = new SimpleQuantity();
			serviceCount.setValue(claimLine.serviceCount);
			item.setQuantity(serviceCount);

			item.setCategory(
					createCodeableConcept(CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE, claimLine.cmsServiceTypeCode));

			addExtensionCoding(item.getLocation(), CODING_SYSTEM_CCW_PRICING_LOCALITY,
					CODING_SYSTEM_CCW_PRICING_LOCALITY, claimLine.linePricingLocalityCode);

			if (claimLine.firstExpenseDate.isPresent() && claimLine.lastExpenseDate.isPresent()) {
				validatePeriodDates(claimLine.firstExpenseDate, claimLine.lastExpenseDate);
				item.setServiced(new Period().setStart((convertToDate(claimLine.firstExpenseDate.get())),
							TemporalPrecisionEnum.DAY)
						.setEnd((convertToDate(claimLine.lastExpenseDate.get())),
							TemporalPrecisionEnum.DAY));
			}

			if (claimLine.hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(CODING_SYSTEM_HCPCS, "" + claimGroup.hcpcsYearCode.get(),
						claimLine.hcpcsCode.get()));
			}
			if (claimLine.hcpcsInitialModifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE1,
						"" + claimGroup.hcpcsYearCode.get(), claimLine.hcpcsInitialModifierCode.get()));
			}
			if (claimLine.hcpcsSecondModifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE2,
						"" + claimGroup.hcpcsYearCode.get(), claimLine.hcpcsSecondModifierCode.get()));
			}
			if (claimLine.betosCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_BETOS, CODING_SYSTEM_BETOS, claimLine.betosCode.get());
			}

			addExtensionCoding(item, CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH, CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH,
					"" + claimLine.serviceDeductibleCode.get());

			AdjudicationComponent adjudicationForPayment = item.addAdjudication();
			adjudicationForPayment
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.paymentAmount);
			addExtensionCoding(adjudicationForPayment, CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH,
					CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH, "" + claimLine.paymentCode.get());

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.beneficiaryPaymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PAYMENT_B))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.providerPaymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_DEDUCTIBLE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.beneficiaryPartBDeductAmount);

			if (claimLine.primaryPayerCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_PRIMARY_PAYER_CD, CODING_SYSTEM_PRIMARY_PAYER_CD,
						String.valueOf(claimLine.primaryPayerCode.get()));
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.primaryPayerPaidAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.coinsuranceAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.submittedChargeAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_ALLOWED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.allowedChargeAmount);

			if (claimLine.mtusCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_MTUS_CD, CODING_SYSTEM_MTUS_CD,
						String.valueOf(claimLine.mtusCode.get()));
			}

			if (!claimLine.mtusCount.equals(ZERO)) {
				addExtensionCoding(item, CODING_SYSTEM_MTUS_COUNT, CODING_SYSTEM_MTUS_COUNT,
						String.valueOf(claimLine.mtusCount));
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR))
					.setReason(createCodeableConcept(CODING_SYSTEM_CMS_LINE_PROCESSING_INDICATOR,
							claimLine.processingIndicatorCode.get()));

			if (claimLine.diagnosis.isPresent())
				addDiagnosisLink(eob, item, claimLine.diagnosis.get());

			if (claimLine.nationalDrugCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_NDC, CODING_SYSTEM_NDC, claimLine.nationalDrugCode.get());
			}

			if (claimLine.hctHgbTestTypeCode.isPresent()
					&& claimLine.hctHgbTestResult.compareTo(BigDecimal.ZERO) != 0) {
				Observation hctHgbObservation = new Observation();
				hctHgbObservation.setStatus(ObservationStatus.UNKNOWN);
				CodeableConcept hctHgbTestType = new CodeableConcept();
				hctHgbTestType.addCoding().setSystem(CODING_SYSTEM_CMS_HCT_OR_HGB_TEST_TYPE)
						.setCode(claimLine.hctHgbTestTypeCode.get());
				hctHgbObservation.setCode(hctHgbTestType);
				hctHgbObservation.setValue(new Quantity().setValue(claimLine.hctHgbTestResult));
				item.addExtension()
						.setUrl(EXTENSION_CMS_HCT_OR_HGB_RESULTS)
						.setValue(new Reference(hctHgbObservation));
			} else if (!claimLine.hctHgbTestTypeCode.isPresent()
					&& claimLine.hctHgbTestResult.compareTo(BigDecimal.ZERO) == 0) {
				// Nothing to do here; don't map a non-existent Observation.
			} else {
				throw new InvalidRifValueException(String.format(
						"Inconsistent hctHgbTestTypeCode and hctHgbTestResult" + " values for claim '%s'.",
						claimGroup.claimId));
			}

			if (claimLine.cliaLabNumber.isPresent()) {
				addExtensionCoding(item.getLocation(), CODING_SYSTEM_CLIA_LAB_NUM, CODING_SYSTEM_CLIA_LAB_NUM,
						claimLine.cliaLabNumber.get());
			}
		}

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformInpatientClaim(RifRecordEvent<InpatientClaimGroup> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("Inpatient RIF record is null");
		InpatientClaimGroup claimGroup = rifRecordEvent.getRecord();
		if (claimGroup.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(claimGroup.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_ID).setValue(claimGroup.claimId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(claimGroup.claimGroupId);
		eob.getInsurance().setCoverage(referenceCoverage(claimGroup.beneficiaryId, COVERAGE_PLAN_PART_A));
		eob.setPatient(referencePatient(claimGroup.beneficiaryId));

		eob.setType(createCodeableConcept(CODING_SYSTEM_CCW_CLAIM_TYPE, claimGroup.claimTypeCode));
		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_RECORD_ID_CD, CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.nearLineRecordIdCode));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		validatePeriodDates(claimGroup.dateFrom, claimGroup.dateThrough);
		setPeriodStart(eob.getBillablePeriod(), claimGroup.dateFrom);
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.dateThrough);

		addExtensionCoding(eob.getBillablePeriod(), CODING_SYSTEM_QUERY_CD, CODING_SYSTEM_QUERY_CD,
				String.valueOf(claimGroup.claimQueryCode));

		if (claimGroup.claimNonPaymentReasonCode.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
					CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, claimGroup.claimNonPaymentReasonCode.get());
		}

		if (!claimGroup.patientDischargeStatusCode.isEmpty()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PATIENT_DISCHARGE_STATUS_CD,
					claimGroup.patientDischargeStatusCode));
		}

		eob.getPayment()
				.setAmount((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.paymentAmount));
		eob.setTotalCost((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.totalChargeAmount));

		if (claimGroup.claimAdmissionDate.isPresent() || claimGroup.beneficiaryDischargeDate.isPresent()){
			validatePeriodDates(claimGroup.claimAdmissionDate, claimGroup.beneficiaryDischargeDate);
			Period period = new Period();
			if (claimGroup.claimAdmissionDate.isPresent()) {
				period.setStart(convertToDate(claimGroup.claimAdmissionDate.get()), TemporalPrecisionEnum.DAY);
			}
			if (claimGroup.beneficiaryDischargeDate.isPresent()) {
				period.setEnd(convertToDate(claimGroup.beneficiaryDischargeDate.get()), TemporalPrecisionEnum.DAY);
			}
			eob.setHospitalization(period);
		}

		eob.addInformation().setCategory(
				createCodeableConcept(CODING_SYSTEM_ADMISSION_TYPE_CD, String.valueOf(claimGroup.admissionTypeCd)));

		if (claimGroup.sourceAdmissionCd.isPresent()) {
			eob.addInformation().setCategory(createCodeableConcept(CODING_SYSTEM_SOURCE_ADMISSION_CD,
					String.valueOf(claimGroup.sourceAdmissionCd.get())));
		}

		if (claimGroup.patientStatusCd.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PATIENT_STATUS_CD,
					String.valueOf(claimGroup.patientStatusCd.get())));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.passThruPerDiemAmount != null) {
			BenefitComponent benefitPerDiem = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PASS_THRU_PER_DIEM_AMT));
			benefitPerDiem.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.passThruPerDiemAmount));
			benefitBalances.getFinancial().add(benefitPerDiem);
		}

		if (claimGroup.deductibleAmount != null) {
			BenefitComponent benefitInpatientDeductible = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_BENEFIT_DEDUCTIBLE_AMT_URL));
			benefitInpatientDeductible
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.deductibleAmount));
			benefitBalances.getFinancial().add(benefitInpatientDeductible);
		}

		if (claimGroup.primaryPayerPaidAmount != null) {
			BenefitComponent benefitInpatientNchPrimaryPayerAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			benefitInpatientNchPrimaryPayerAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.primaryPayerPaidAmount));
			benefitBalances.getFinancial().add(benefitInpatientNchPrimaryPayerAmt);
		}

		if (claimGroup.partACoinsuranceLiabilityAmount != null) {
			BenefitComponent benefitPartACoinsuranceLiabilityAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_BENEFIT_COIN_AMT_URL));
			benefitPartACoinsuranceLiabilityAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.partACoinsuranceLiabilityAmount));
			benefitBalances.getFinancial().add(benefitPartACoinsuranceLiabilityAmt);
		}

		if (claimGroup.bloodDeductibleLiabilityAmount != null) {
			BenefitComponent benefitInpatientNchPrimaryPayerAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL));
			benefitInpatientNchPrimaryPayerAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.bloodDeductibleLiabilityAmount));
			benefitBalances.getFinancial().add(benefitInpatientNchPrimaryPayerAmt);
		}

		if (claimGroup.professionalComponentCharge != null) {
			BenefitComponent benefitProfessionComponentAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_PROFFESIONAL_CHARGE_URL));
			benefitProfessionComponentAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.professionalComponentCharge));
			benefitBalances.getFinancial().add(benefitProfessionComponentAmt);
		}

		if (claimGroup.noncoveredCharge != null) {
			BenefitComponent benefitNonCoveredChangeAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL));
			benefitNonCoveredChangeAmt
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.noncoveredCharge));
			benefitBalances.getFinancial().add(benefitNonCoveredChangeAmt);
		}

		if (claimGroup.totalDeductionAmount != null) {
			BenefitComponent benefitTotalChangeAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_INPATIENT_TOTAL_AMT_URL));
			benefitTotalChangeAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.totalDeductionAmount));
			benefitBalances.getFinancial().add(benefitTotalChangeAmt);
		}

		if (claimGroup.claimTotalPPSCapitalAmount != null) {
			BenefitComponent claimTotalPPSAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_TOTAL_PPS_CAPITAL_AMT_URL));
			claimTotalPPSAmt.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimTotalPPSCapitalAmount.get()));
			benefitBalances.getFinancial().add(claimTotalPPSAmt);
		}

		if (claimGroup.claimPPSCapitalFSPAmount != null) {
			BenefitComponent claimPPSCapitalFSPAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL));
			claimPPSCapitalFSPAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.claimPPSCapitalFSPAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalFSPAmt);
		}

		if (claimGroup.claimPPSCapitalOutlierAmount != null) {
			BenefitComponent claimPPSCapitalOutlierAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL));
			claimPPSCapitalOutlierAmount.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSCapitalOutlierAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalOutlierAmount);
		}

		if (claimGroup.claimPPSCapitalDisproportionateShareAmt != null) {
			BenefitComponent claimPPSCapitalDisproportionateShareAmt = new BenefitComponent(createCodeableConcept(
					BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL));
			claimPPSCapitalDisproportionateShareAmt.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSCapitalDisproportionateShareAmt.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalDisproportionateShareAmt);
		}

		if (claimGroup.claimPPSCapitalIMEAmount != null) {
			BenefitComponent claimPPSCapitalIMEAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL));
			claimPPSCapitalIMEAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.claimPPSCapitalIMEAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalIMEAmount);
		}

		if (claimGroup.claimPPSCapitalExceptionAmount != null) {
			BenefitComponent claimPPSCapitalExceptionAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL));
			claimPPSCapitalExceptionAmount.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSCapitalExceptionAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalExceptionAmount);
		}

		if (claimGroup.claimPPSOldCapitalHoldHarmlessAmount != null) {
			BenefitComponent claimPPSOldCapitalHoldHarmlessAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL));
			claimPPSOldCapitalHoldHarmlessAmount.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSOldCapitalHoldHarmlessAmount.get()));
			benefitBalances.getFinancial().add(claimPPSOldCapitalHoldHarmlessAmount);
		}

		BenefitComponent utilizationDayCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_UTILIZATION_DAY_COUNT));
		utilizationDayCount.setUsed(new UnsignedIntType(claimGroup.utilizationDayCount));
		benefitBalances.getFinancial().add(utilizationDayCount);

		BenefitComponent coinsuranceDayCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_COINSURANCE_DAY_COUNT));
		coinsuranceDayCount.setUsed(new UnsignedIntType(claimGroup.coinsuranceDayCount));
		benefitBalances.getFinancial().add(coinsuranceDayCount);

		BenefitComponent nonUtilizationDayCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT));
		nonUtilizationDayCount.setAllowed(new UnsignedIntType(claimGroup.nonUtilizationDayCount));
		benefitBalances.getFinancial().add(nonUtilizationDayCount);

		BenefitComponent bloodPintsFurnishedQty = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY));
		bloodPintsFurnishedQty.setUsed(new UnsignedIntType(claimGroup.bloodPintsFurnishedQty));
		benefitBalances.getFinancial().add(bloodPintsFurnishedQty);

		if (claimGroup.noncoveredStayFromDate.isPresent() && claimGroup.noncoveredStayThroughDate.isPresent()) {
			validatePeriodDates(claimGroup.noncoveredStayFromDate, claimGroup.noncoveredStayThroughDate);
			eob.addInformation()
					.setCategory(createCodeableConcept(BENEFIT_COVERAGE_DATE, CODING_SYSTEM_NONCOVERED_STAY_DATE))
					.setTiming(new Period()
							.setStart(convertToDate((claimGroup.noncoveredStayFromDate.get())),
									TemporalPrecisionEnum.DAY)
							.setEnd(convertToDate((claimGroup.noncoveredStayThroughDate.get())),
									TemporalPrecisionEnum.DAY));
		}

		if (claimGroup.coveredCareThoughDate.isPresent()) {
			eob.addInformation()
					.setCategory(createCodeableConcept(BENEFIT_COVERAGE_DATE, CODING_SYSTEM_COVERED_CARE_DATE))
					.setTiming(new DateType(convertToDate(claimGroup.coveredCareThoughDate.get())));
		}

		if (claimGroup.medicareBenefitsExhaustedDate.isPresent()) {
			eob.addInformation()
					.setCategory(createCodeableConcept(BENEFIT_COVERAGE_DATE, CODING_SYSTEM_BENEFITS_EXHAUSTED_DATE))
					.setTiming(new DateType(convertToDate(claimGroup.medicareBenefitsExhaustedDate.get())));
		}

		if (claimGroup.diagnosisRelatedGroupCd.isPresent()) {
			eob.addInformation().setCategory(createCodeableConcept(CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD,
					claimGroup.diagnosisRelatedGroupCd.get()));
		}

		if (claimGroup.nchDrugOutlierApprovedPaymentAmount != null) {
			BenefitComponent nchDrugOutlierApprovedPaymentAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_DRUG_OUTLIER_APPROVED_PAYMENT_AMT_URL));
			nchDrugOutlierApprovedPaymentAmount.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.nchDrugOutlierApprovedPaymentAmount.get()));
			benefitBalances.getFinancial().add(nchDrugOutlierApprovedPaymentAmount);
		}

		if (claimGroup.organizationNpi.isPresent()) {
			eob.setOrganization(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			eob.setFacility(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			addExtensionCoding(eob.getFacility(), CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
					CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(claimGroup.claimFacilityTypeCode));
		}

		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claimGroup.claimServiceClassificationTypeCode));

		addInformation(eob,
				createCodeableConcept(CODING_SYSTEM_FREQUENCY_CD, String.valueOf(claimGroup.claimFrequencyCode)));

		if (claimGroup.claimPrimaryPayerCode.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PRIMARY_PAYER_CD,
					String.valueOf(claimGroup.claimPrimaryPayerCode.get())));
		}

		if (claimGroup.attendingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.attendingPhysicianNpi.get(),
					CARE_TEAM_ROLE_PRIMARY);
		}

		if (claimGroup.operatingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.operatingPhysicianNpi.get(),
					CARE_TEAM_ROLE_ASSISTING);
		}

		if (claimGroup.otherPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.otherPhysicianNpi.get(),
					CARE_TEAM_ROLE_OTHER);
		}

		if (claimGroup.mcoPaidSw.isPresent()) {
			addInformation(eob,
					createCodeableConcept(CODING_SYSTEM_MCO_PAID_CD, String.valueOf(claimGroup.mcoPaidSw.get())));
		}

		if (claimGroup.diagnosisAdmitting.isPresent())
			addDiagnosisCode(eob, claimGroup.diagnosisAdmitting.get());
		if (claimGroup.diagnosisPrincipal.isPresent())
			addDiagnosisCode(eob, claimGroup.diagnosisPrincipal.get());
		for (IcdCode diagnosis : claimGroup.diagnosesAdditional)
			addDiagnosisCode(eob, diagnosis);

		if (claimGroup.diagnosisFirstClaimExternal.isPresent()) {
			addDiagnosisCode(eob, (claimGroup.diagnosisFirstClaimExternal.get()));
		}

		for (IcdCode diagnosis : claimGroup.diagnosesExternal)
			if (!diagnosis.getCode().isEmpty()) {
				addDiagnosisCode(eob, diagnosis);
			}

		for (IcdCode procedure : claimGroup.procedureCodes)
			if (!procedure.getCode().isEmpty()) {
				addProcedureCode(eob, procedure);
			}

		for (InpatientClaimLine claimLine : claimGroup.lines) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.lineNumber);

			addExtensionCoding(item, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setRevenue(createCodeableConcept(CODING_SYSTEM_REVENUE_CENTER, claimLine.revenueCenter));

			if (claimLine.hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(CODING_SYSTEM_HCPCS, claimLine.hcpcsCode.get()));
			}

			item.setLocation(new Address().setState((claimGroup.providerStateCode)));

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.rateAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.totalChargeAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_NONCOVERED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.nonCoveredChargeAmount);

			if (claimLine.deductibleCoinsuranceCd.isPresent()) {
				addExtensionCoding(item.getRevenue(), CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						String.valueOf(claimLine.deductibleCoinsuranceCd.get()));
			}

			/*
			 * Set item quantity to Unit Count first if > 0; NDC quantity next
			 * if present; otherwise set to 0
			 */
			SimpleQuantity qty = new SimpleQuantity();
			if (!claimLine.unitCount.equals(new BigDecimal(0))) {
				qty.setValue(claimLine.unitCount);
			} else if (claimLine.nationalDrugCodeQuantity.isPresent()) {
				qty.setValue(claimLine.nationalDrugCodeQuantity.get());
			} else {
				qty.setValue(0);
			}
			item.setQuantity(qty);

			if (claimLine.nationalDrugCodeQualifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(CODING_SYSTEM_NDC_QLFR_CD,
						claimLine.nationalDrugCodeQualifierCode.get()));
			}

			if (claimLine.revenueCenterRenderingPhysicianNPI.isPresent()) {
				addCareTeamPractitioner(eob, item, CODING_SYSTEM_NPI_US,
						claimLine.revenueCenterRenderingPhysicianNPI.get(), CARE_TEAM_ROLE_PRIMARY);
			}

		}

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformOutpatientClaim(RifRecordEvent<OutpatientClaimGroup> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("Outpatient RIF record is null");
		OutpatientClaimGroup claimGroup = rifRecordEvent.getRecord();
		if (claimGroup.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(claimGroup.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_ID).setValue(claimGroup.claimId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(claimGroup.claimGroupId);
		eob.getInsurance().setCoverage(referenceCoverage(claimGroup.beneficiaryId, COVERAGE_PLAN_PART_B));
		eob.setPatient(referencePatient(claimGroup.beneficiaryId));
		eob.setType(createCodeableConcept(CODING_SYSTEM_CCW_CLAIM_TYPE, claimGroup.claimTypeCode));
		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_RECORD_ID_CD, CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.nearLineRecordIdCode));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		validatePeriodDates(claimGroup.dateFrom, claimGroup.dateThrough);
		setPeriodStart(eob.getBillablePeriod(), claimGroup.dateFrom);
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.dateThrough);

		addExtensionCoding(eob.getBillablePeriod(), CODING_SYSTEM_QUERY_CD, CODING_SYSTEM_QUERY_CD,
				String.valueOf(claimGroup.claimQueryCode));

		eob.setProvider(createIdentifierReference(CODING_SYSTEM_PROVIDER_NUMBER, claimGroup.providerNumber));

		if (claimGroup.claimNonPaymentReasonCode.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
					CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, claimGroup.claimNonPaymentReasonCode.get());
		}
		eob.getPayment()
				.setAmount((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.paymentAmount));
		eob.setTotalCost((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.totalChargeAmount));

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.primaryPayerPaidAmount != null) {
			BenefitComponent primaryPayerPaidAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			primaryPayerPaidAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.primaryPayerPaidAmount));
			benefitBalances.getFinancial().add(primaryPayerPaidAmount);
		}

		if (claimGroup.bloodDeductibleLiabilityAmount != null) {
			BenefitComponent bloodDeductibleLiabilityAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL));
			bloodDeductibleLiabilityAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.bloodDeductibleLiabilityAmount));
			benefitBalances.getFinancial().add(bloodDeductibleLiabilityAmount);
		}

		if (claimGroup.professionalComponentCharge != null) {
			BenefitComponent benefitProfessionComponentAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_PROFFESIONAL_CHARGE_URL));
			benefitProfessionComponentAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.professionalComponentCharge));
			benefitBalances.getFinancial().add(benefitProfessionComponentAmt);
		}

		if (claimGroup.deductibleAmount != null) {
			BenefitComponent deductibleAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_BEN_PART_B_DED_AMT_URL));
			deductibleAmount
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.deductibleAmount));
			benefitBalances.getFinancial().add(deductibleAmount);
		}

		if (claimGroup.coninsuranceAmount != null) {
			BenefitComponent coninsuranceAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_BEN_PART_B_COINSUR_AMT_URL));
			coninsuranceAmount
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.coninsuranceAmount));
			benefitBalances.getFinancial().add(coninsuranceAmount);
		}

		if (claimGroup.providerPaymentAmount != null) {
			BenefitComponent providerPaymentAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PAYMENT_B));
			providerPaymentAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.providerPaymentAmount));
			benefitBalances.getFinancial().add(providerPaymentAmount);
		}

		if (claimGroup.beneficiaryPaymentAmount != null) {
			BenefitComponent beneficiaryPaymentAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_OUTPAT_BEN__PAYMENT_AMT_URL));
			beneficiaryPaymentAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.beneficiaryPaymentAmount));
			benefitBalances.getFinancial().add(beneficiaryPaymentAmount);
		}

		if (claimGroup.organizationNpi.isPresent()) {
			eob.setOrganization(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			eob.setFacility(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			addExtensionCoding(eob.getFacility(), CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
					CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(claimGroup.claimFacilityTypeCode));
		}

		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claimGroup.claimServiceClassificationTypeCode));

		addInformation(eob,
				createCodeableConcept(CODING_SYSTEM_FREQUENCY_CD, String.valueOf(claimGroup.claimFrequencyCode)));

		if (claimGroup.claimPrimaryPayerCode.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PRIMARY_PAYER_CD,
					String.valueOf(claimGroup.claimPrimaryPayerCode.get())));
		}

		if (claimGroup.attendingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.attendingPhysicianNpi.get(),
					CARE_TEAM_ROLE_PRIMARY);
		}

		if (claimGroup.operatingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.operatingPhysicianNpi.get(),
					CARE_TEAM_ROLE_ASSISTING);
		}

		if (claimGroup.otherPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.otherPhysicianNpi.get(),
					CARE_TEAM_ROLE_OTHER);
		}

		if (claimGroup.mcoPaidSw.isPresent()) {
			addInformation(eob,
					createCodeableConcept(CODING_SYSTEM_MCO_PAID_CD, String.valueOf(claimGroup.mcoPaidSw.get())));
		}

		if (claimGroup.diagnosisPrincipal.isPresent())
			addDiagnosisCode(eob, claimGroup.diagnosisPrincipal.get());
		for (IcdCode diagnosis : claimGroup.diagnosesAdditional)
			addDiagnosisCode(eob, diagnosis);

		if (claimGroup.diagnosisFirstClaimExternal.isPresent()) {
			addDiagnosisCode(eob, (claimGroup.diagnosisFirstClaimExternal.get()));
		}

		for (IcdCode diagnosis : claimGroup.diagnosesExternal)
			if (!diagnosis.getCode().isEmpty()) {
				addDiagnosisCode(eob, diagnosis);
			}

		for (IcdCode procedure : claimGroup.procedureCodes)
			if (!procedure.getCode().isEmpty()) {
				addProcedureCode(eob, procedure);
			}

		for (IcdCode diagnosis : claimGroup.diagnosesReasonForVisit)
			if (!diagnosis.getCode().isEmpty()) {
				addDiagnosisCode(eob, diagnosis);
			}

		for (OutpatientClaimLine claimLine : claimGroup.lines) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.lineNumber);

			addExtensionCoding(item, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setRevenue(createCodeableConcept(CODING_SYSTEM_REVENUE_CENTER, claimLine.revenueCenter));

			item.setLocation(new Address().setState((claimGroup.providerStateCode)));

			if (claimLine.nationalDrugCode.isPresent()) {
				item.setService(createCodeableConcept(CODING_SYSTEM_NDC, claimLine.nationalDrugCode.get()));
			}

			if (claimLine.revCntr1stAnsiCd.isPresent()) {
				item.addAdjudication()
						.setCategory(
								createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_1ST_ANSI_CD))
						.setReason(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
								claimLine.revCntr1stAnsiCd.get()));
			}
			if (claimLine.revCntr2ndAnsiCd.isPresent()) {
				item.addAdjudication()
						.setCategory(
								createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_2ND_ANSI_CD))
						.setReason(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
								claimLine.revCntr2ndAnsiCd.get()));
			}
			if (claimLine.revCntr3rdAnsiCd.isPresent()) {
				item.addAdjudication()
						.setCategory(
								createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_3RD_ANSI_CD))
						.setReason(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
								claimLine.revCntr3rdAnsiCd.get()));
			}
			if (claimLine.revCntr4thAnsiCd.isPresent()) {
				item.addAdjudication()
						.setCategory(
								createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_4TH_ANSI_CD))
						.setReason(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
								claimLine.revCntr4thAnsiCd.get()));
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.rateAmount);

			if (claimLine.hcpcsCode.isPresent()) {
				item.addModifier(createCodeableConcept(CODING_SYSTEM_HCPCS, claimLine.hcpcsCode.get()));
			}
			if (claimLine.hcpcsInitialModifierCode.isPresent()) {
				item.addModifier(
						createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE1, claimLine.hcpcsInitialModifierCode.get()));
			}
			if (claimLine.hcpcsSecondModifierCode.isPresent()) {
				item.addModifier(
						createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE2, claimLine.hcpcsSecondModifierCode.get()));
			}

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_BLOOD_DEDUCTIBLE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.bloodDeductibleAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_CASH_DEDUCTIBLE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.cashDeductibleAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_WAGE_ADJ_COINSURANCE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.wageAdjustedCoinsuranceAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_REDUCED_COINSURANCE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.reducedCoinsuranceAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_1ST_MSP_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.firstMspPaidAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_2ND_MSP_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.secondMspPaidAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.providerPaymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.benficiaryPaymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_PATIENT_RESPONSIBILITY_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.patientResponsibilityAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.paymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.totalChargeAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_NONCOVERED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.nonCoveredChargeAmount);

			/*
			 * Set item quantity to Unit Count first if > 0; NDC quantity next
			 * if present; otherwise set to 0
			 */
			SimpleQuantity qty = new SimpleQuantity();
			if (!claimLine.unitCount.equals(new BigDecimal(0))) {
				qty.setValue(claimLine.unitCount);
			} else if (claimLine.nationalDrugCodeQuantity.isPresent()) {
				qty.setValue(claimLine.nationalDrugCodeQuantity.get());
			} else {
				qty.setValue(0);
			}
			item.setQuantity(qty);

			if (claimLine.nationalDrugCodeQualifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(CODING_SYSTEM_NDC_QLFR_CD,
						claimLine.nationalDrugCodeQualifierCode.get()));
			}

			if (claimLine.revenueCenterRenderingPhysicianNPI.isPresent()) {
				addCareTeamPractitioner(eob, item, CODING_SYSTEM_NPI_US,
						claimLine.revenueCenterRenderingPhysicianNPI.get(), CARE_TEAM_ROLE_PRIMARY);
			}
		}

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformSNFClaim(RifRecordEvent<SNFClaimGroup> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("SNF RIF record is null");
		SNFClaimGroup claimGroup = rifRecordEvent.getRecord();
		if (claimGroup.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(claimGroup.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_ID).setValue(claimGroup.claimId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(claimGroup.claimGroupId);
		eob.getInsurance().setCoverage(referenceCoverage(claimGroup.beneficiaryId, COVERAGE_PLAN_PART_A));
		eob.setPatient(referencePatient(claimGroup.beneficiaryId));
		eob.setType(createCodeableConcept(CODING_SYSTEM_CCW_CLAIM_TYPE, claimGroup.claimTypeCode));
		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_RECORD_ID_CD, CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.nearLineRecordIdCode));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		validatePeriodDates(claimGroup.dateFrom, claimGroup.dateThrough);
		setPeriodStart(eob.getBillablePeriod(), claimGroup.dateFrom);
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.dateThrough);

		addExtensionCoding(eob.getBillablePeriod(), CODING_SYSTEM_QUERY_CD, CODING_SYSTEM_QUERY_CD,
				String.valueOf(claimGroup.claimQueryCode));

		eob.setProvider(createIdentifierReference(CODING_SYSTEM_PROVIDER_NUMBER, claimGroup.providerNumber));

		if (claimGroup.claimNonPaymentReasonCode.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
					CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, claimGroup.claimNonPaymentReasonCode.get());
		}

		eob.getPayment()
				.setAmount((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.paymentAmount));
		eob.setTotalCost((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.totalChargeAmount));

		if (claimGroup.claimAdmissionDate.isPresent() || claimGroup.beneficiaryDischargeDate.isPresent()) {
			validatePeriodDates(claimGroup.claimAdmissionDate, claimGroup.beneficiaryDischargeDate);
			Period period = new Period();
			if (claimGroup.claimAdmissionDate.isPresent()) {
				period.setStart(convertToDate(claimGroup.claimAdmissionDate.get()), TemporalPrecisionEnum.DAY);
			}
			if (claimGroup.beneficiaryDischargeDate.isPresent()) {
				period.setEnd(convertToDate(claimGroup.beneficiaryDischargeDate.get()), TemporalPrecisionEnum.DAY);
			}
			eob.setHospitalization(period);
		}

		eob.addInformation().setCategory(
				createCodeableConcept(CODING_SYSTEM_ADMISSION_TYPE_CD, String.valueOf(claimGroup.admissionTypeCd)));

		if (claimGroup.sourceAdmissionCd.isPresent()) {
			eob.addInformation().setCategory(createCodeableConcept(CODING_SYSTEM_SOURCE_ADMISSION_CD,
					String.valueOf(claimGroup.sourceAdmissionCd.get())));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.primaryPayerPaidAmount != null) {
			BenefitComponent primaryPayerPaidAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			primaryPayerPaidAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.primaryPayerPaidAmount));
			benefitBalances.getFinancial().add(primaryPayerPaidAmount);
		}

		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claimGroup.claimServiceClassificationTypeCode));

		if (claimGroup.organizationNpi.isPresent()) {
			eob.setOrganization(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			eob.setFacility(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			addExtensionCoding(eob.getFacility(), CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
					CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(claimGroup.claimFacilityTypeCode));
		}

		addInformation(eob,
				createCodeableConcept(CODING_SYSTEM_FREQUENCY_CD, String.valueOf(claimGroup.claimFrequencyCode)));

		if (claimGroup.claimPrimaryPayerCode.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PRIMARY_PAYER_CD,
					String.valueOf(claimGroup.claimPrimaryPayerCode.get())));
		}

		if (claimGroup.attendingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.attendingPhysicianNpi.get(),
					CARE_TEAM_ROLE_PRIMARY);
		}

		if (claimGroup.operatingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.operatingPhysicianNpi.get(),
					CARE_TEAM_ROLE_ASSISTING);
		}

		if (claimGroup.otherPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.otherPhysicianNpi.get(),
					CARE_TEAM_ROLE_OTHER);
		}

		if (claimGroup.mcoPaidSw.isPresent()) {
			addInformation(eob,
					createCodeableConcept(CODING_SYSTEM_MCO_PAID_CD, String.valueOf(claimGroup.mcoPaidSw.get())));
		}

		if (claimGroup.patientStatusCd.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PATIENT_STATUS_CD,
					String.valueOf(claimGroup.patientStatusCd.get())));
		}

		if (claimGroup.deductibleAmount != null) {
			BenefitComponent benefitInpatientDeductible = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_BENEFIT_DEDUCTIBLE_AMT_URL));
			benefitInpatientDeductible
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.deductibleAmount));
			benefitBalances.getFinancial().add(benefitInpatientDeductible);
		}

		if (claimGroup.partACoinsuranceLiabilityAmount != null) {
			BenefitComponent benefitPartACoinsuranceLiabilityAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_BENEFIT_COIN_AMT_URL));
			benefitPartACoinsuranceLiabilityAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.partACoinsuranceLiabilityAmount));
			benefitBalances.getFinancial().add(benefitPartACoinsuranceLiabilityAmt);
		}

		if (claimGroup.bloodDeductibleLiabilityAmount != null) {
			BenefitComponent benefitInpatientNchPrimaryPayerAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL));
			benefitInpatientNchPrimaryPayerAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.bloodDeductibleLiabilityAmount));
			benefitBalances.getFinancial().add(benefitInpatientNchPrimaryPayerAmt);
		}

		if (claimGroup.noncoveredCharge != null) {
			BenefitComponent benefitNonCoveredChangeAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL));
			benefitNonCoveredChangeAmt
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.noncoveredCharge));
			benefitBalances.getFinancial().add(benefitNonCoveredChangeAmt);
		}

		if (claimGroup.totalDeductionAmount != null) {
			BenefitComponent benefitTotalChangeAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_NCH_INPATIENT_TOTAL_AMT_URL));
			benefitTotalChangeAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.totalDeductionAmount));
			benefitBalances.getFinancial().add(benefitTotalChangeAmt);
		}

		if (claimGroup.claimPPSCapitalFSPAmount != null) {
			BenefitComponent claimPPSCapitalFSPAmt = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL));
			claimPPSCapitalFSPAmt.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.claimPPSCapitalFSPAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalFSPAmt);
		}

		if (claimGroup.claimPPSCapitalOutlierAmount != null) {
			BenefitComponent claimPPSCapitalOutlierAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL));
			claimPPSCapitalOutlierAmount.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSCapitalOutlierAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalOutlierAmount);
		}

		if (claimGroup.claimPPSCapitalDisproportionateShareAmt != null) {
			BenefitComponent claimPPSCapitalDisproportionateShareAmt = new BenefitComponent(createCodeableConcept(
					BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL));
			claimPPSCapitalDisproportionateShareAmt.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSCapitalDisproportionateShareAmt.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalDisproportionateShareAmt);
		}

		if (claimGroup.claimPPSCapitalIMEAmount != null) {
			BenefitComponent claimPPSCapitalIMEAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL));
			claimPPSCapitalIMEAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.claimPPSCapitalIMEAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalIMEAmount);
		}

		if (claimGroup.claimPPSCapitalExceptionAmount != null) {
			BenefitComponent claimPPSCapitalExceptionAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL));
			claimPPSCapitalExceptionAmount.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSCapitalExceptionAmount.get()));
			benefitBalances.getFinancial().add(claimPPSCapitalExceptionAmount);
		}

		if (claimGroup.claimPPSOldCapitalHoldHarmlessAmount != null) {
			BenefitComponent claimPPSOldCapitalHoldHarmlessAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL));
			claimPPSOldCapitalHoldHarmlessAmount.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.claimPPSOldCapitalHoldHarmlessAmount.get()));
			benefitBalances.getFinancial().add(claimPPSOldCapitalHoldHarmlessAmount);
		}

		BenefitComponent utilizationDayCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_UTILIZATION_DAY_COUNT));
		utilizationDayCount.setUsed(new UnsignedIntType(claimGroup.utilizationDayCount));
		benefitBalances.getFinancial().add(utilizationDayCount);

		BenefitComponent coinsuranceDayCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_COINSURANCE_DAY_COUNT));
		coinsuranceDayCount.setUsed(new UnsignedIntType(claimGroup.coinsuranceDayCount));
		benefitBalances.getFinancial().add(coinsuranceDayCount);

		BenefitComponent nonUtilizationDayCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT));
		nonUtilizationDayCount.setAllowed(new UnsignedIntType(claimGroup.nonUtilizationDayCount));
		benefitBalances.getFinancial().add(nonUtilizationDayCount);

		BenefitComponent bloodPintsFurnishedQty = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY));
		bloodPintsFurnishedQty.setUsed(new UnsignedIntType(claimGroup.bloodPintsFurnishedQty));
		benefitBalances.getFinancial().add(bloodPintsFurnishedQty);

		if (claimGroup.qualifiedStayFromDate.isPresent() && claimGroup.qualifiedStayThroughDate.isPresent()) {
			validatePeriodDates(claimGroup.qualifiedStayFromDate, claimGroup.qualifiedStayThroughDate);
			eob.addInformation()
					.setCategory(createCodeableConcept(BENEFIT_COVERAGE_DATE, CODING_SYSTEM_QUALIFIED_STAY_DATE))
					.setTiming(new Period()
							.setStart(convertToDate((claimGroup.qualifiedStayFromDate.get())),
									TemporalPrecisionEnum.DAY)
							.setEnd(convertToDate((claimGroup.qualifiedStayThroughDate.get())),
									TemporalPrecisionEnum.DAY));
		}

		if (claimGroup.noncoveredStayFromDate.isPresent() && claimGroup.noncoveredStayThroughDate.isPresent()) {
			validatePeriodDates(claimGroup.noncoveredStayFromDate, claimGroup.noncoveredStayThroughDate);
			eob.addInformation()
					.setCategory(createCodeableConcept(BENEFIT_COVERAGE_DATE, CODING_SYSTEM_NONCOVERED_STAY_DATE))
					.setTiming(new Period()
							.setStart(convertToDate((claimGroup.noncoveredStayFromDate.get())),
									TemporalPrecisionEnum.DAY)
							.setEnd(convertToDate((claimGroup.noncoveredStayThroughDate.get())),
									TemporalPrecisionEnum.DAY));
		}

		if (claimGroup.coveredCareThoughDate.isPresent()) {
			eob.addInformation()
					.setCategory(createCodeableConcept(BENEFIT_COVERAGE_DATE, CODING_SYSTEM_COVERED_CARE_DATE))
					.setTiming(new DateType(convertToDate(claimGroup.coveredCareThoughDate.get())));
		}

		if (claimGroup.medicareBenefitsExhaustedDate.isPresent()) {
			eob.addInformation()
					.setCategory(createCodeableConcept(BENEFIT_COVERAGE_DATE, CODING_SYSTEM_BENEFITS_EXHAUSTED_DATE))
					.setTiming(new DateType(convertToDate(claimGroup.medicareBenefitsExhaustedDate.get())));
		}

		if (claimGroup.diagnosisRelatedGroupCd.isPresent()) {
			eob.addInformation().setCategory(createCodeableConcept(CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD,
					claimGroup.diagnosisRelatedGroupCd.get()));
		}

		eob.addInformation().setCategory(
				createCodeableConcept(CODING_SYSTEM_ADMISSION_TYPE_CD, String.valueOf(claimGroup.admissionTypeCd)));

		if (claimGroup.diagnosisAdmitting.isPresent())
			addDiagnosisCode(eob, claimGroup.diagnosisAdmitting.get());
		addDiagnosisCode(eob, claimGroup.diagnosisPrincipal);
		for (IcdCode diagnosis : claimGroup.diagnosesAdditional)
			addDiagnosisCode(eob, diagnosis);

		if (claimGroup.diagnosisFirstClaimExternal.isPresent()) {
			addDiagnosisCode(eob, (claimGroup.diagnosisFirstClaimExternal.get()));
		}

		for (IcdCode diagnosis : claimGroup.diagnosesExternal)
			if (!diagnosis.getCode().isEmpty()) {
				addDiagnosisCode(eob, diagnosis);
			}

		for (IcdCode procedure : claimGroup.procedureCodes)
			if (!procedure.getCode().isEmpty()) {
				addProcedureCode(eob, procedure);
			}

		for (SNFClaimLine claimLine : claimGroup.lines) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.lineNumber);

			addExtensionCoding(item, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setRevenue(createCodeableConcept(CODING_SYSTEM_REVENUE_CENTER, claimLine.revenueCenter));

			item.setLocation(new Address().setState((claimGroup.providerStateCode)));

			if (claimLine.hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(CODING_SYSTEM_HCPCS, claimLine.hcpcsCode.get()));
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.rateAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.totalChargeAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_NONCOVERED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.nonCoveredChargeAmount);

			if (claimLine.deductibleCoinsuranceCd.isPresent()) {
				addExtensionCoding(item.getRevenue(), CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						String.valueOf(claimLine.deductibleCoinsuranceCd.get()));
			}

			/*
			 * Set item quantity to Unit Count first if > 0; NDC quantity next
			 * if present; otherwise set to 0
			 */
			SimpleQuantity qty = new SimpleQuantity();
			if (!claimLine.unitCount.equals(new BigDecimal(0))) {
				qty.setValue(claimLine.unitCount);
			} else if (claimLine.nationalDrugCodeQuantity.isPresent()) {
				qty.setValue(claimLine.nationalDrugCodeQuantity.get());
			} else {
				qty.setValue(0);
			}
			item.setQuantity(qty);

			if (claimLine.nationalDrugCodeQualifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(CODING_SYSTEM_NDC_QLFR_CD,
						claimLine.nationalDrugCodeQualifierCode.get()));
			}

			if (claimLine.revenueCenterRenderingPhysicianNPI.isPresent()) {
				addCareTeamPractitioner(eob, item, CODING_SYSTEM_NPI_US,
						claimLine.revenueCenterRenderingPhysicianNPI.get(), CARE_TEAM_ROLE_PRIMARY);
			}

		}

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformHospiceClaim(RifRecordEvent<HospiceClaimGroup> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("Hospice RIF record is null");
		HospiceClaimGroup claimGroup = rifRecordEvent.getRecord();
		if (claimGroup.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(claimGroup.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_ID).setValue(claimGroup.claimId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(claimGroup.claimGroupId);
		eob.getInsurance().setCoverage(referenceCoverage(claimGroup.beneficiaryId, COVERAGE_PLAN_PART_A));
		eob.setPatient(referencePatient(claimGroup.beneficiaryId));
		eob.setType(createCodeableConcept(CODING_SYSTEM_CCW_CLAIM_TYPE, claimGroup.claimTypeCode));
		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_RECORD_ID_CD, CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.nearLineRecordIdCode));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		validatePeriodDates(claimGroup.dateFrom, claimGroup.dateThrough);
		setPeriodStart(eob.getBillablePeriod(), claimGroup.dateFrom);
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.dateThrough);

		eob.setProvider(createIdentifierReference(CODING_SYSTEM_PROVIDER_NUMBER, claimGroup.providerNumber));

		if (claimGroup.claimNonPaymentReasonCode.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
					CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, claimGroup.claimNonPaymentReasonCode.get());
		}

		if (!claimGroup.patientDischargeStatusCode.isEmpty()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PATIENT_DISCHARGE_STATUS_CD,
					claimGroup.patientDischargeStatusCode));
		}

		eob.getPayment()
				.setAmount((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.paymentAmount));
		eob.setTotalCost((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.totalChargeAmount));

		if (claimGroup.patientStatusCd.isPresent()) {
			eob.addInformation().setCategory(
					createCodeableConcept(CODING_SYSTEM_PATIENT_STATUS_CD,
							String.valueOf(claimGroup.patientStatusCd.get())));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		BenefitComponent utilizationDayCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_UTILIZATION_DAY_COUNT));
		utilizationDayCount.setUsed(new UnsignedIntType(claimGroup.utilizationDayCount));
		benefitBalances.getFinancial().add(utilizationDayCount);

		if (claimGroup.primaryPayerPaidAmount != null) {
			BenefitComponent primaryPayerPaidAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			primaryPayerPaidAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.primaryPayerPaidAmount));
			benefitBalances.getFinancial().add(primaryPayerPaidAmount);
		}

		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claimGroup.claimServiceClassificationTypeCode));

		if (claimGroup.claimHospiceStartDate.isPresent() || claimGroup.beneficiaryDischargeDate.isPresent()) {
			validatePeriodDates(claimGroup.claimHospiceStartDate, claimGroup.beneficiaryDischargeDate);
			Period period = new Period();
			if (claimGroup.claimHospiceStartDate.isPresent()) {
				period.setStart(convertToDate(claimGroup.claimHospiceStartDate.get()), TemporalPrecisionEnum.DAY);
			}
			if (claimGroup.beneficiaryDischargeDate.isPresent()) {
				period.setEnd(convertToDate(claimGroup.beneficiaryDischargeDate.get()), TemporalPrecisionEnum.DAY);
			}
			eob.setHospitalization(period);
		}

		if (claimGroup.organizationNpi.isPresent()) {
			eob.setOrganization(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			eob.setFacility(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			addExtensionCoding(eob.getFacility(), CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
					CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(claimGroup.claimFacilityTypeCode));
		}

		addInformation(eob,
				createCodeableConcept(CODING_SYSTEM_FREQUENCY_CD, String.valueOf(claimGroup.claimFrequencyCode)));

		if (claimGroup.claimPrimaryPayerCode.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PRIMARY_PAYER_CD,
					String.valueOf(claimGroup.claimPrimaryPayerCode.get())));
		}

		if (claimGroup.attendingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.attendingPhysicianNpi.get(),
					CARE_TEAM_ROLE_PRIMARY);
		}

		if (claimGroup.diagnosisPrincipal.isPresent())
			addDiagnosisCode(eob, claimGroup.diagnosisPrincipal.get());
		for (IcdCode diagnosis : claimGroup.diagnosesAdditional)
			addDiagnosisCode(eob, diagnosis);

		if (claimGroup.diagnosisFirstClaimExternal.isPresent()) {
			addDiagnosisCode(eob, (claimGroup.diagnosisFirstClaimExternal.get()));
		}

		for (IcdCode diagnosis : claimGroup.diagnosesExternal)
			if (!diagnosis.getCode().isEmpty()) {
				addDiagnosisCode(eob, diagnosis);
			}

		for (HospiceClaimLine claimLine : claimGroup.lines) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.lineNumber);

			item.setRevenue(createCodeableConcept(CODING_SYSTEM_REVENUE_CENTER, claimLine.revenueCenter));

			if (claimLine.hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(CODING_SYSTEM_HCPCS, claimLine.hcpcsCode.get()));
			}

			item.setLocation(new Address().setState((claimGroup.providerStateCode)));

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.rateAmount);

			if (claimLine.hcpcsInitialModifierCode.isPresent()) {
				item.addModifier(
						createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE1, claimLine.hcpcsInitialModifierCode.get()));
			}
			if (claimLine.hcpcsSecondModifierCode.isPresent()) {
				item.addModifier(
						createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE2, claimLine.hcpcsSecondModifierCode.get()));
			}

			addExtensionCoding(item, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.providerPaymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.benficiaryPaymentAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.paymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.totalChargeAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_NONCOVERED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.nonCoveredChargeAmount);

			if (claimLine.deductibleCoinsuranceCd.isPresent()) {
				addExtensionCoding(item.getRevenue(), CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						String.valueOf(claimLine.deductibleCoinsuranceCd.get()));
			}

			/*
			 * Set item quantity to Unit Count first if > 0; NDC quantity next
			 * if present; otherwise set to 0
			 */
			SimpleQuantity qty = new SimpleQuantity();
			if (!claimLine.unitCount.equals(new BigDecimal(0))) {
				qty.setValue(claimLine.unitCount);
			} else if (claimLine.nationalDrugCodeQuantity.isPresent()) {
				qty.setValue(claimLine.nationalDrugCodeQuantity.get());
			} else {
				qty.setValue(0);
			}
			item.setQuantity(qty);

			if (claimLine.nationalDrugCodeQualifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(CODING_SYSTEM_NDC_QLFR_CD,
						claimLine.nationalDrugCodeQualifierCode.get()));
			}

			if (claimLine.revenueCenterRenderingPhysicianNPI.isPresent()) {
				addCareTeamPractitioner(eob, item, CODING_SYSTEM_NPI_US,
						claimLine.revenueCenterRenderingPhysicianNPI.get(), CARE_TEAM_ROLE_PRIMARY);
			}
		}

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformHHAClaim(RifRecordEvent<HHAClaimGroup> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("HHA RIF record is null");
		HHAClaimGroup claimGroup = rifRecordEvent.getRecord();
		if (claimGroup.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(claimGroup.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_ID).setValue(claimGroup.claimId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(claimGroup.claimGroupId);
		eob.getInsurance().setCoverage(referenceCoverage(claimGroup.beneficiaryId, COVERAGE_PLAN_PART_A));
		eob.setPatient(referencePatient(claimGroup.beneficiaryId));
		eob.setType(createCodeableConcept(CODING_SYSTEM_CCW_CLAIM_TYPE, claimGroup.claimTypeCode));
		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_RECORD_ID_CD, CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.nearLineRecordIdCode));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		validatePeriodDates(claimGroup.dateFrom, claimGroup.dateThrough);
		setPeriodStart(eob.getBillablePeriod(), claimGroup.dateFrom);
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.dateThrough);

		eob.setProvider(createIdentifierReference(CODING_SYSTEM_PROVIDER_NUMBER, claimGroup.providerNumber));

		if (claimGroup.claimNonPaymentReasonCode.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
					CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, claimGroup.claimNonPaymentReasonCode.get());
		}

		if (!claimGroup.patientDischargeStatusCode.isEmpty()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PATIENT_DISCHARGE_STATUS_CD,
					claimGroup.patientDischargeStatusCode));
		}

		eob.getPayment()
				.setAmount((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.paymentAmount));
		eob.setTotalCost((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.totalChargeAmount));

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.primaryPayerPaidAmount != null) {
			BenefitComponent primaryPayerPaidAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			primaryPayerPaidAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.primaryPayerPaidAmount));
			benefitBalances.getFinancial().add(primaryPayerPaidAmount);
		}

		if (claimGroup.organizationNpi.isPresent()) {
			eob.setOrganization(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			eob.setFacility(createIdentifierReference(CODING_SYSTEM_NPI_US, claimGroup.organizationNpi.get()));
			addExtensionCoding(eob.getFacility(), CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
					CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(claimGroup.claimFacilityTypeCode));
		}

		addInformation(eob,
				createCodeableConcept(CODING_SYSTEM_FREQUENCY_CD, String.valueOf(claimGroup.claimFrequencyCode)));

		if (claimGroup.claimPrimaryPayerCode.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_PRIMARY_PAYER_CD,
					String.valueOf(claimGroup.claimPrimaryPayerCode.get())));
		}

		if (claimGroup.attendingPhysicianNpi.isPresent()) {
			addCareTeamPractitioner(eob, null, CODING_SYSTEM_NPI_US, claimGroup.attendingPhysicianNpi.get(),
					CARE_TEAM_ROLE_PRIMARY);
		}

		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claimGroup.claimServiceClassificationTypeCode));

		addDiagnosisCode(eob, claimGroup.diagnosisPrincipal);
		for (IcdCode diagnosis : claimGroup.diagnosesAdditional)
			addDiagnosisCode(eob, diagnosis);

		if (claimGroup.diagnosisFirstClaimExternal.isPresent()) {
			addDiagnosisCode(eob, (claimGroup.diagnosisFirstClaimExternal.get()));
		}

		for (IcdCode diagnosis : claimGroup.diagnosesExternal)
			if (!diagnosis.getCode().isEmpty()) {
				addDiagnosisCode(eob, diagnosis);
			}

		if (claimGroup.claimLUPACode.isPresent()) {
			addInformation(eob,
					createCodeableConcept(CODING_SYSTEM_HHA_LUPA_CD, String.valueOf(claimGroup.claimLUPACode.get())));
		}
		if (claimGroup.claimReferralCode.isPresent()) {
			addInformation(eob, createCodeableConcept(CODING_SYSTEM_HHA_REFERRAL_CD,
					String.valueOf(claimGroup.claimReferralCode.get())));
		}

		BenefitComponent totalVisitCount = new BenefitComponent(
				createCodeableConcept(BENEFIT_BALANCE_TYPE, CODING_SYSTEM_HHA_VISIT_COUNT));
		totalVisitCount.setUsed(new UnsignedIntType(claimGroup.totalVisitCount));
		benefitBalances.getFinancial().add(totalVisitCount);

		if (claimGroup.careStartDate.isPresent()){
			eob.setHospitalization(
					new Period().setStart(convertToDate(claimGroup.careStartDate.get()), TemporalPrecisionEnum.DAY));
			}

		for (HHAClaimLine claimLine : claimGroup.lines) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.lineNumber);

			item.setRevenue(createCodeableConcept(CODING_SYSTEM_REVENUE_CENTER, claimLine.revenueCenter));

			addExtensionCoding(item, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setLocation(new Address().setState((claimGroup.providerStateCode)));

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.rateAmount);

			if (claimLine.revCntr1stAnsiCd.isPresent()) {
				item.addAdjudication()
						.setCategory(
								createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_1ST_ANSI_CD))
						.setReason(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
								claimLine.revCntr1stAnsiCd.get()));
			}

			if (claimLine.hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(CODING_SYSTEM_HCPCS, claimLine.hcpcsCode.get()));
			}

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.paymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.totalChargeAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_NONCOVERED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.nonCoveredChargeAmount);

			if (claimLine.deductibleCoinsuranceCd.isPresent()) {
				addExtensionCoding(item.getRevenue(), CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						String.valueOf(claimLine.deductibleCoinsuranceCd.get()));
			}

			if (claimLine.hcpcsInitialModifierCode.isPresent()) {
				item.addModifier(
						createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE1, claimLine.hcpcsInitialModifierCode.get()));
			}

			if (claimLine.hcpcsSecondModifierCode.isPresent()) {
				item.addModifier(
						createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE2, claimLine.hcpcsSecondModifierCode.get()));
			}

			/*
			 * Set item quantity to Unit Count first if > 0; NDC quantity next
			 * if present; otherwise set to 0
			 */
			SimpleQuantity qty = new SimpleQuantity();
			if (!claimLine.unitCount.equals(new BigDecimal(0))) {
				qty.setValue(claimLine.unitCount);
			} else if (claimLine.nationalDrugCodeQuantity.isPresent()) {
				qty.setValue(claimLine.nationalDrugCodeQuantity.get());
			} else {
				qty.setValue(0);
			}
			item.setQuantity(qty);

			if (claimLine.nationalDrugCodeQualifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(CODING_SYSTEM_NDC_QLFR_CD,
						claimLine.nationalDrugCodeQualifierCode.get()));
			}

			if (claimLine.revenueCenterRenderingPhysicianNPI.isPresent()) {
				addCareTeamPractitioner(eob, item, CODING_SYSTEM_NPI_US,
						claimLine.revenueCenterRenderingPhysicianNPI.get(), CARE_TEAM_ROLE_PRIMARY);
			}

		}

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param rifRecordEvent
	 *            the source {@link RifRecordEvent} to be transformed
	 * @return the {@link TransformedBundle} that is the result of transforming
	 *         the specified {@link RifRecordEvent}
	 */
	private TransformedBundle transformDMEClaim(RifRecordEvent<DMEClaimGroup> rifRecordEvent) {
		if (rifRecordEvent == null)
			throw new InvalidRifFileFormatException("DME RIF record is null");
		DMEClaimGroup claimGroup = rifRecordEvent.getRecord();
		if (claimGroup.recordAction != RecordAction.INSERT)
			// Will need refactoring to support other ops.
			throw new UnsupportedRifRecordActionException(
					claimGroup.recordAction);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		Identifier eobId = eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_ID).setValue(claimGroup.claimId);
		eob.addIdentifier().setSystem(CODING_SYSTEM_CCW_CLAIM_GRP_ID).setValue(claimGroup.claimGroupId);
		eob.getInsurance().setCoverage(referenceCoverage(claimGroup.beneficiaryId, COVERAGE_PLAN_PART_B));
		eob.setPatient(referencePatient(claimGroup.beneficiaryId));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		if (claimGroup.clinicalTrialNumber.isPresent()) {
			addExtensionCoding(eob, CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
					CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER, claimGroup.clinicalTrialNumber.get());
		}

		eob.setType(createCodeableConcept(CODING_SYSTEM_CCW_CLAIM_TYPE, claimGroup.claimTypeCode));
		addExtensionCoding(eob.getType(), CODING_SYSTEM_CCW_RECORD_ID_CD, CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.nearLineRecordIdCode));

		validatePeriodDates(claimGroup.dateFrom, claimGroup.dateThrough);
		setPeriodStart(eob.getBillablePeriod(), claimGroup.dateFrom);
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.dateThrough);

		eob.setDisposition(CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION);
		addExtensionCoding(eob, CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER, CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER,
				claimGroup.carrierNumber);
		addExtensionCoding(eob, CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD, CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD,
				claimGroup.paymentDenialCode);
		eob.getPayment()
				.setAmount((Money) new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.paymentAmount));

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.primaryPayerPaidAmount != null) {
			BenefitComponent primaryPayerPaidAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			primaryPayerPaidAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.primaryPayerPaidAmount));
			benefitBalances.getFinancial().add(primaryPayerPaidAmount);
		}

		/*
		 * Referrals are represented as contained resources, because otherwise
		 * updating them would require an extra roundtrip to the server (can't
		 * think of an intelligent client-specified ID for them).
		 */
		if (claimGroup.referringPhysicianNpi.isPresent()) {
			ReferralRequest referral = new ReferralRequest();
			referral.setStatus(ReferralRequestStatus.COMPLETED);
			referral.setSubject(referencePatient(claimGroup.beneficiaryId));
			referral.setRequester(new ReferralRequestRequesterComponent(
					referencePractitioner(claimGroup.referringPhysicianNpi.get())));
			// Set the ReferralRequest as a contained resource in the EOB:
			eob.setReferral(new Reference(referral));
		}

		addExtensionCoding(eob, CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT, CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
					String.valueOf(claimGroup.providerAssignmentIndicator));

		if (!claimGroup.providerPaymentAmount.equals(ZERO)) {
			BenefitComponent providerPaymentAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_PAYMENT_B));
			providerPaymentAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.providerPaymentAmount));
			benefitBalances.getFinancial().add(providerPaymentAmount);
		}

		if (!claimGroup.beneficiaryPaymentAmount.equals(ZERO)) {
			BenefitComponent beneficiaryPaymentAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT));
			beneficiaryPaymentAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.beneficiaryPaymentAmount));
			benefitBalances.getFinancial().add(beneficiaryPaymentAmount);
		}

		if (!claimGroup.submittedChargeAmount.equals(ZERO)) {
			BenefitComponent submittedChargeAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT));
			submittedChargeAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.submittedChargeAmount));
			benefitBalances.getFinancial().add(submittedChargeAmount);
		}

		if (!claimGroup.allowedChargeAmount.equals(ZERO)) {
			BenefitComponent allowedChargeAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_ALLOWED_CHARGE));
			allowedChargeAmount
					.setAllowed(new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.allowedChargeAmount));
			benefitBalances.getFinancial().add(allowedChargeAmount);
		}

		if (!claimGroup.beneficiaryPartBDeductAmount.equals(ZERO)) {
			BenefitComponent beneficiaryPartBDeductAmount = new BenefitComponent(
					createCodeableConcept(BENEFIT_BALANCE_TYPE, CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE));
			beneficiaryPartBDeductAmount.setAllowed(
					new Money().setSystem(CODING_SYSTEM_MONEY_US).setValue(claimGroup.beneficiaryPartBDeductAmount));
			benefitBalances.getFinancial().add(beneficiaryPartBDeductAmount);
		}

		if (claimGroup.diagnosisPrincipal.isPresent())
			addDiagnosisCode(eob, claimGroup.diagnosisPrincipal.get());
		for (IcdCode diagnosis : claimGroup.diagnosesAdditional)
			addDiagnosisCode(eob, diagnosis);

		addExtensionCoding(eob, CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
				CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER, claimGroup.clinicalTrialNumber.get());

		for (DMEClaimLine claimLine : claimGroup.lines) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.number);

			/*
			 * Per Michelle at GDIT, and also Tony Dean at OEDA, the performing
			 * provider _should_ always be present. However, we've found some
			 * examples in production where it's not for some claim lines. (This
			 * is annoying, as it's present on other lines in the same claim,
			 * and the data indicates that the same NPI probably applies to the
			 * lines where it's not specified. Still, it's not safe to guess at
			 * this, so we'll leave it blank.)
			 */
			if (claimLine.providerNPI.isPresent()) {
				ExplanationOfBenefit.CareTeamComponent performingCareTeamMember = addCareTeamPractitioner(eob, item,
						CODING_SYSTEM_NPI_US, claimLine.providerNPI.get(), CARE_TEAM_ROLE_PRIMARY);
				performingCareTeamMember.setResponsible(true);

				/*
				 * The provider's "specialty" and "type" code are equivalent.
				 * However, the "specialty" codes are more granular, and seem to
				 * better match the example FHIR
				 * `http://hl7.org/fhir/ex-providerqualification` code set.
				 * Accordingly, we map the "specialty" codes to the
				 * `qualification` field here, and stick the "type" code into an
				 * extension. TODO: suggest that the spec allows more than one
				 * `qualification` entry.
				 */
				performingCareTeamMember
						.setQualification(createCodeableConcept(CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD,
								"" + claimLine.providerSpecialityCode.get()));

				addExtensionCoding(performingCareTeamMember, CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						"" + claimLine.providerParticipatingIndCode.get());
			}

			addExtensionCoding(item, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE, CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setCategory(
					createCodeableConcept(CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE, claimLine.cmsServiceTypeCode));

			item.setLocation(
					createCodeableConcept(CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION, claimLine.placeOfServiceCode));
			if (!claimLine.providerStateCode.isEmpty()) {
				addExtensionCoding(item.getLocation(), CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
						CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD, claimLine.providerStateCode);
			}

			if (claimLine.firstExpenseDate.isPresent() && claimLine.lastExpenseDate.isPresent()) {
				validatePeriodDates(claimLine.firstExpenseDate, claimLine.lastExpenseDate);
				item.setServiced(new Period()
					.setStart((convertToDate(claimLine.firstExpenseDate.get())),
							TemporalPrecisionEnum.DAY)
					.setEnd((convertToDate(claimLine.lastExpenseDate.get())),
							TemporalPrecisionEnum.DAY));
			}

			if (claimLine.hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(CODING_SYSTEM_HCPCS, "" + claimGroup.hcpcsYearCode.get(),
						claimLine.hcpcsCode.get()));
			}
			if (claimLine.hcpcsInitialModifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE1,
						"" + claimGroup.hcpcsYearCode.get(), claimLine.hcpcsInitialModifierCode.get()));
			}
			if (claimLine.hcpcsSecondModifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE2,
						"" + claimGroup.hcpcsYearCode.get(), claimLine.hcpcsSecondModifierCode.get()));
			}

			if (claimLine.betosCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_BETOS, CODING_SYSTEM_BETOS, claimLine.betosCode.get());
			}

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.paymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.beneficiaryPaymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_PAYMENT_B))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.providerPaymentAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_DEDUCTIBLE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.beneficiaryPartBDeductAmount);

			if (claimLine.primaryPayerCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_PRIMARY_PAYER_CD,
						CODING_SYSTEM_PRIMARY_PAYER_CD,
						String.valueOf(claimLine.primaryPayerCode.get()));
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.primaryPayerPaidAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.coinsuranceAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_LINE_PRIMARY_PAYER_ALLOWED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.primaryPayerAllowedChargeAmount);

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.submittedChargeAmount);

			item.addAdjudication()
					.setCategory(
							createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS, CODED_ADJUDICATION_ALLOWED_CHARGE))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.allowedChargeAmount);

			if (claimLine.processingIndicatorCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_CCW_PROCESSING_INDICATOR_CD,
						CODING_SYSTEM_CCW_PROCESSING_INDICATOR_CD, claimLine.processingIndicatorCode.get());
			}

			if (claimLine.paymentCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_CCW_PAYMENT_80_100_INDICATOR_CD,
						CODING_SYSTEM_CCW_PAYMENT_80_100_INDICATOR_CD, String.valueOf(claimLine.paymentCode.get()));
			}

			if (claimLine.serviceDeductibleCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_CCW_DEDUCTIBLE_INDICATOR_CD,
						CODING_SYSTEM_CCW_DEDUCTIBLE_INDICATOR_CD,
						String.valueOf(claimLine.serviceDeductibleCode.get()));
			}

			if (claimLine.diagnosis.isPresent())
				addDiagnosisLink(eob, item, claimLine.diagnosis.get());

			item.addAdjudication()
					.setCategory(createCodeableConcept(CODING_SYSTEM_ADJUDICATION_CMS,
							CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT))
					.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.purchasePriceAmount);

			if (claimLine.pricingStateCode.isPresent()) {
				addExtensionCoding(item.getLocation(), CODING_SYSTEM_PRICING_STATE_CD, CODING_SYSTEM_PRICING_STATE_CD,
						claimLine.pricingStateCode.get());
			}

			if (claimLine.supplierTypeCode.isPresent()) {
				addExtensionCoding(item.getLocation(), CODING_SYSTEM_SUPPLIER_TYPE_CD, CODING_SYSTEM_SUPPLIER_TYPE_CD,
						String.valueOf(claimLine.supplierTypeCode.get()));
			}

			if (claimLine.hcpcsThirdModifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE3,
						"" + claimGroup.hcpcsYearCode.get(), claimLine.hcpcsThirdModifierCode.get()));
			}
			if (claimLine.hcpcsFourthModifierCode.isPresent()) {
				item.addModifier(createCodeableConcept(HCPCS_INITIAL_MODIFIER_CODE4,
						"" + claimGroup.hcpcsYearCode.get(), claimLine.hcpcsFourthModifierCode.get()));
			}

			if (!claimLine.screenSavingsAmount.equals(ZERO)) {
				addExtensionCoding(item, CODING_SYSTEM_SCREEN_SAVINGS_AMT, CODING_SYSTEM_SCREEN_SAVINGS_AMT,
						String.valueOf(claimLine.screenSavingsAmount));
			}

			if (claimLine.mtusCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_MTUS_CD, CODING_SYSTEM_MTUS_CD,
						String.valueOf(claimLine.mtusCode.get()));
			}

			if (!claimLine.mtusCount.equals(ZERO)) {
				addExtensionCoding(item, CODING_SYSTEM_MTUS_COUNT, CODING_SYSTEM_MTUS_COUNT,
						String.valueOf(claimLine.mtusCount));
			}

			if (claimLine.hctHgbTestTypeCode.isPresent()
					&& claimLine.hctHgbTestResult.compareTo(BigDecimal.ZERO) != 0) {
				Observation hctHgbObservation = new Observation();
				hctHgbObservation.setStatus(ObservationStatus.UNKNOWN);
				CodeableConcept hctHgbTestType = new CodeableConcept();
				hctHgbTestType.addCoding().setSystem(CODING_SYSTEM_CMS_HCT_OR_HGB_TEST_TYPE)
						.setCode(claimLine.hctHgbTestTypeCode.get());
				hctHgbObservation.setCode(hctHgbTestType);
				hctHgbObservation.setValue(new Quantity().setValue(claimLine.hctHgbTestResult));
				item.addExtension().setUrl(EXTENSION_CMS_HCT_OR_HGB_RESULTS).setValue(new Reference(hctHgbObservation));
			} else if (!claimLine.hctHgbTestTypeCode.isPresent()
					&& claimLine.hctHgbTestResult.compareTo(BigDecimal.ZERO) == 0) {
				// Nothing to do here; don't map a non-existent Observation.
			} else {
				throw new InvalidRifValueException(String.format(
						"Inconsistent hctHgbTestTypeCode and hctHgbTestResult" + " values for claim '%s'.",
						claimGroup.claimId));
			}

			if (claimLine.nationalDrugCode.isPresent()) {
				addExtensionCoding(item, CODING_SYSTEM_NDC, CODING_SYSTEM_NDC, claimLine.nationalDrugCode.get());
			}

		}

		conditionalCreate(bundle, eob, eobId);
		return new TransformedBundle(rifRecordEvent, bundle);
	}

	/**
	 * @param urlText
	 *            the URL or URL portion to be encoded
	 * @return a URL-encoded version of the specified text
	 */
	public static String urlEncode(String urlText) {
		try {
			return URLEncoder.encode(urlText, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * Adds the specified {@link Resource}s to the specified {@link Bundle},
	 * setting it as a <a href="https://www.hl7.org/fhir/http.html#ccreate">FHIR
	 * "conditional create" operation</a>.
	 * 
	 * @param bundle
	 *            the {@link Bundle} to include the resource in
	 * @param resource
	 *            the FHIR {@link Resource} to be created (as part of the
	 *            specified transaction {@link Bundle})
	 * @param conditionQuery
	 *            the value to use for
	 *            {@link BundleEntryRequestComponent#setIfNoneExist(String)},
	 *            which must be a search query of the form
	 *            "<code>fhirType/?field=value</code>"
	 * @return a {@link Reference} instance, which must be used for any
	 *         references to the {@link Resource} within the same {@link Bundle}
	 */
	public static Reference conditionalCreate(Bundle bundle, Resource resource, String conditionQuery) {
		if (bundle == null)
			throw new IllegalArgumentException("Bundle is null");
		if (resource == null)
			throw new IllegalArgumentException("Resource is null");
		if (conditionQuery == null)
			throw new IllegalArgumentException("Condition query is null");

		/*
		 * See the `dev/design-decisions-readme.md` file for a more complete
		 * explanation, but due to limitations imposed by the FHIR spec we
		 * cannot specify resources' logical ID and also create them
		 * idempotently. Idempotency is more important for our architecture, so
		 * we disallow setting IDs here (and compliant FHIR servers will ignore
		 * them, anyways).
		 */
		if (resource.getId() != null)
			throw new IllegalArgumentException();

		BundleEntryComponent bundleEntry = bundle.addEntry();
		bundleEntry.setFullUrl(IdType.newRandomUuid().getValue()).setResource(resource).getRequest()
				.setMethod(HTTPVerb.POST).setIfNoneExist(conditionQuery);

		Reference bundleEntryReference = new Reference(bundleEntry.getFullUrl());
		return bundleEntryReference;
	}

	/**
	 * Adds the specified {@link Resource}s to the specified {@link Bundle},
	 * setting it as a <a href="https://www.hl7.org/fhir/http.html#ccreate">FHIR
	 * "conditional create" operation</a>.
	 * 
	 * @param bundle
	 *            the {@link Bundle} to include the resource in
	 * @param resource
	 *            the FHIR {@link Resource} to be created (as part of the
	 *            specified transaction {@link Bundle})
	 * @param uniqueId
	 *            the value to use for
	 *            {@link BundleEntryRequestComponent#setIfNoneExist(String)},
	 *            which will be converted to a search query of the form
	 *            "<code>fhirType/?field=value</code>"
	 * @return a {@link Reference} instance, which must be used for any
	 *         references to the {@link Resource} within the same {@link Bundle}
	 */
	public static Reference conditionalCreate(Bundle bundle, Resource resource, Identifier uniqueId) {
		if (uniqueId == null)
			throw new IllegalArgumentException("Unique ID is null");

		String conditionQuery = String.format("%s?identifier=%s|%s", resource.fhirType(), uniqueId.getSystem(),
				uniqueId.getValue());
		return conditionalCreate(bundle, resource, conditionQuery);
	}

	/**
	 * Adds the specified {@link Resource}s to the specified {@link Bundle},
	 * setting it as a <a href="https://www.hl7.org/fhir/http.html#ccreate">FHIR
	 * "conditional create" operation</a>.
	 * 
	 * @param bundle
	 *            the {@link Bundle} to include the resource in
	 * @param resource
	 *            the FHIR {@link Resource} to be created (as part of the
	 *            specified transaction {@link Bundle})
	 * @param conditionQuery
	 *            the {@link Reference} to use for
	 *            {@link BundleEntryRequestComponent#setIfNoneExist(String)}
	 * @return a {@link Reference} instance, which must be used for any
	 *         references to the {@link Resource} within the same {@link Bundle}
	 */
	public static Reference conditionalCreate(Bundle bundle, Resource resource, Reference conditionQuery) {
		return conditionalCreate(bundle, resource, conditionQuery.getReference());
	}

	/**
	 * @param organizationNpi
	 *            the {@link Organization#getIdentifier()} value to match (where
	 *            {@link Identifier#getSystem()} is
	 *            {@value #CODING_SYSTEM_NPI_US})
	 * @return a {@link Reference} to the {@link Organization} resource that
	 *         matches the specified parameters
	 */
	static Reference referenceOrganizationByNpi(String organizationNpi) {
		return new Reference(String.format("Organization?identifier=%s|%s", CODING_SYSTEM_NPI_US, organizationNpi));
	}

	/**
	 * @param subPlan
	 *            the {@link Coverage#getSubPlan()} value to match
	 * @param beneficiaryPatientId
	 *            the {@link #CODING_SYSTEM_CCW_BENE_ID} ID value for the
	 *            {@link Coverage#getBeneficiary()} value to match
	 * @return a {@link Reference} to the {@link Coverage} resource where
	 *         {@link Coverage#getPlan()} matches {@link #COVERAGE_PLAN} and the
	 *         other parameters specified also match
	 */
	static Reference referenceCoverage(String beneficiaryPatientId, String subPlan) {
		return new Reference(String.format("Coverage?beneficiary.identifier=%s|%s&subplan=%s",
				CODING_SYSTEM_CCW_BENE_ID, urlEncode(beneficiaryPatientId), urlEncode(subPlan)));
	}

	/**
	 * @param patientId
	 *            the {@link #CODING_SYSTEM_CCW_BENE_ID} ID value for the
	 *            beneficiary to match
	 * @return a {@link Reference} to the {@link Patient} resource that matches
	 *         the specified parameters
	 */
	static Reference referencePatient(String patientId) {
		return new Reference(
				String.format("Patient?identifier=%s|%s", CODING_SYSTEM_CCW_BENE_ID, urlEncode(patientId)));
	}

	/**
	 * @param practitionerNpi
	 *            the {@link Practitioner#getIdentifier()} value to match (where
	 *            {@link Identifier#getSystem()} is
	 *            {@value #CODING_SYSTEM_NPI_US})
	 * @return a {@link Reference} to the {@link Practitioner} resource that
	 *         matches the specified parameters
	 */
	static Reference referencePractitioner(String practitionerNpi) {
		return createIdentifierReference(CODING_SYSTEM_NPI_US, practitionerNpi);
	}

	/**
	 * @param period
	 *            the {@link Period} to adjust
	 * @param date
	 *            the {@link LocalDate} to set the {@link Period#getStart()}
	 *            value with/to
	 */
	private static void setPeriodStart(Period period, LocalDate date) {
		period.setStart(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), TemporalPrecisionEnum.DAY);
	}

	/**
	 * @param period
	 *            the {@link Period} to adjust
	 * @param date
	 *            the {@link LocalDate} to set the {@link Period#getEnd()} value
	 *            with/to
	 */
	private static void setPeriodEnd(Period period, LocalDate date) {
		period.setEnd(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), TemporalPrecisionEnum.DAY);
	}

	/**
	 * <p>
	 * Adds an {@link Extension} to the specified {@link DomainResource}.
	 * {@link Extension#getValue()} will be set to a {@link CodeableConcept}
	 * containing a single {@link Coding}, with the specified system and code.
	 * </p>
	 * <p>
	 * Data Architecture Note: The {@link CodeableConcept} might seem extraneous
	 * -- why not just add the {@link Coding} directly to the {@link Extension}?
	 * The main reason for doing it this way is consistency: this is what FHIR
	 * seems to do everywhere.
	 * </p>
	 * 
	 * @param fhirElement
	 *            the FHIR element to add the {@link Extension} to
	 * @param extensionUrl
	 *            the {@link Extension#getUrl()} to use
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 */
	private static void addExtensionCoding(IBaseHasExtensions fhirElement, String extensionUrl, String codingSystem,
			String codingCode) {
		IBaseExtension<?, ?> extension = fhirElement.addExtension();
		extension.setUrl(extensionUrl);
		extension.setValue(new Coding());

		CodeableConcept codeableConcept = new CodeableConcept();
		extension.setValue(codeableConcept);

		Coding coding = codeableConcept.addCoding();
		coding.setSystem(codingSystem).setCode(codingCode);
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to (possibly) modify
	 * @param diagnosis
	 *            the {@link IcdCode} to add, if it's not already present
	 * @return the {@link DiagnosisComponent#getSequence()} of the existing or
	 *         newly-added entry
	 */
	private static int addDiagnosisCode(ExplanationOfBenefit eob, IcdCode diagnosis) {
		Optional<DiagnosisComponent> existingDiagnosis = eob.getDiagnosis().stream()
				.filter(d -> d.getDiagnosis() instanceof CodeableConcept)
				.filter(d -> isCodeInConcept((CodeableConcept) d.getDiagnosis(), computeFhirSystem(diagnosis),
						diagnosis.getCode()))
				.findAny();
		if (existingDiagnosis.isPresent())
			return existingDiagnosis.get().getSequenceElement().getValue();

		DiagnosisComponent diagnosisComponent = new DiagnosisComponent().setSequence(eob.getDiagnosis().size() + 1);
		diagnosisComponent
				.setDiagnosis(createCodeableConcept(computeFhirSystem(diagnosis), diagnosis.getCode()));
		if (!diagnosis.getPresentOnAdmission().isEmpty()) {
			diagnosisComponent
					.addType(createCodeableConcept(CODING_SYSTEM_CCW_INP_POA_CD, diagnosis.getPresentOnAdmission()));
		}
		eob.getDiagnosis().add(diagnosisComponent);
		return diagnosisComponent.getSequenceElement().getValue();
	}

	/**
	 * This method computes {@link IcdVersion#getFhirSystem()} values for
	 * {@link IcdCode}s in a way that reasonably handles unparseable RIF values
	 * (as we may see in the dummy data).
	 * 
	 * @param icdCode
	 *            the {@link IcdCode} to compute an
	 *            {@link IcdVersion#getFhirSystem()} value for
	 * @return an {@link IcdVersion#getFhirSystem()} value for the specified
	 *         IcdCode
	 */
	static String computeFhirSystem(IcdCode icdCode) {
		if (icdCode.getVersion().getDecodedValue().isPresent())
			return icdCode.getVersion().getDecodedValue().get().getFhirSystem();
		else
			return String.format("http://hl7.org/fhir/sid/unknown-icd-version/%s", icdCode.getVersion().getRawValue());
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that the specified
	 *            {@link ItemComponent} is a child of
	 * @param item
	 *            the {@link ItemComponent} to add an
	 *            {@link ItemComponent#getDiagnosisLinkId()} entry to
	 * @param diagnosis
	 *            the diagnosis code to add a link for
	 */
	private static void addDiagnosisLink(ExplanationOfBenefit eob, ItemComponent item, IcdCode diagnosis) {
		int diagnosisSequence = addDiagnosisCode(eob, diagnosis);
		item.addDiagnosisLinkId(diagnosisSequence);
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to (possibly) modify
	 * @param infoCategory
	 *            the {@link CodeableConcept} to use as a
	 *            {@link SupportingInformationComponent#getCategory()} value, if
	 *            such an entry is not already present
	 * @return the {@link SupportingInformationComponent#getSequence()} of the
	 *         existing or newly-added entry
	 */
	private static int addInformation(ExplanationOfBenefit eob, CodeableConcept infoCategory) {
		Optional<SupportingInformationComponent> existingInfo = eob.getInformation().stream()
				.filter(d -> infoCategory.equalsDeep(d.getCategory())).findAny();
		if (existingInfo.isPresent())
			return existingInfo.get().getSequenceElement().getValue();

		SupportingInformationComponent infoComponent = new SupportingInformationComponent()
				.setSequence(eob.getInformation().size() + 1);
		infoComponent.setCategory(infoCategory);
		eob.getInformation().add(infoComponent);

		return infoComponent.getSequenceElement().getValue();
	}

	/**
	 * Ensures that the specified {@link ExplanationOfBenefit} has the specified
	 * {@link CareTeamComponent}, and links the specified {@link ItemComponent}
	 * to that {@link CareTeamComponent} (via
	 * {@link ItemComponent#addCareTeamLinkId(int)}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that the
	 *            {@link CareTeamComponent} should be part of
	 * @param eobItem
	 *            the {@link ItemComponent} that should be linked to the
	 *            {@link CareTeamComponent}
	 * @param practitionerIdSystem
	 *            the {@link Identifier#getSystem()} of the practitioner to
	 *            reference in {@link CareTeamComponent#getProvider()}
	 * @param practitionerIdValue
	 *            the {@link Identifier#getValue()} of the practitioner to
	 *            reference in {@link CareTeamComponent#getProvider()}
	 * @return the {@link CareTeamComponent} that was created/linked
	 */
	private static CareTeamComponent addCareTeamPractitioner(ExplanationOfBenefit eob, ItemComponent eobItem,
			String practitionerIdSystem, String practitionerIdValue, String practitionerRole) {
		// Try to find a matching pre-existing entry.
		CareTeamComponent careTeamEntry = eob.getCareTeam().stream()
				.filter(ctc -> ctc.getProvider().hasIdentifier())
				.filter(ctc -> practitionerIdSystem.equals(ctc.getProvider().getIdentifier().getSystem())
						&& practitionerIdValue.equals(ctc.getProvider().getIdentifier().getValue()))
				.findAny().orElse(null);

		// If no match was found, add one to the EOB.
		if (careTeamEntry == null) {
			careTeamEntry = eob.addCareTeam();
		careTeamEntry.setSequence(eob.getCareTeam().size() + 1);
		careTeamEntry.setProvider(new Reference()
					.setIdentifier(new Identifier().setSystem(practitionerIdSystem).setValue(practitionerIdValue)));
			careTeamEntry.setRole(createCodeableConcept(CODING_SYSTEM_CARE_TEAM_ROLE, practitionerRole));
		}

		// care team entry is at eob level so no need to create item link id
		if (eobItem == null) {
			return careTeamEntry;
		}

		// Link the EOB.item to the care team entry (if it isn't already).
		if (!eobItem.getCareTeamLinkId().contains(careTeamEntry.getSequence())) {
			eobItem.addCareTeamLinkId(careTeamEntry.getSequence());
		}

		return careTeamEntry;
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to (possibly) modify
	 * @param diagnosis
	 *            the {@link IcdCode} to add, if it's not already present
	 * @return the {@link ProcedureComponent#getSequence()} of the existing or
	 *         newly-added entry
	 */
	private static int addProcedureCode(ExplanationOfBenefit eob, IcdCode procedure) {
		Optional<ProcedureComponent> existingProcedure = eob.getProcedure().stream()
				.filter(pc -> pc.getProcedure() instanceof CodeableConcept)
				.filter(pc -> isCodeInConcept((CodeableConcept) pc.getProcedure(),
						computeFhirSystem(procedure), procedure.getCode()))
				.findAny();
		if (existingProcedure.isPresent())
			return existingProcedure.get().getSequenceElement().getValue();

		ProcedureComponent procedureComponent = new ProcedureComponent().setSequence(eob.getProcedure().size() + 1);
		procedureComponent
				.setProcedure(createCodeableConcept(computeFhirSystem(procedure), procedure.getCode()));
		procedureComponent
				.setDate(convertToDate(procedure.getProcedureDate()));

		eob.getProcedure().add(procedureComponent);
		return procedureComponent.getSequenceElement().getValue();
	}

	/**
	 * @param concept
	 *            the {@link CodeableConcept} to check
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to match
	 * @param codingCode
	 *            the {@link Coding#getCode()} to match
	 * @return <code>true</code> if the specified {@link CodeableConcept}
	 *         contains the specified {@link Coding}, <code>false</code> if it
	 *         does not
	 */
	static boolean isCodeInConcept(CodeableConcept concept, String codingSystem, String codingCode) {
		return isCodeInConcept(concept, codingSystem, null, codingCode);
	}

	/**
	 * @param concept
	 *            the {@link CodeableConcept} to check
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to match
	 * @param codingSystem
	 *            the {@link Coding#getVersion()} to match
	 * @param codingCode
	 *            the {@link Coding#getCode()} to match
	 * @return <code>true</code> if the specified {@link CodeableConcept}
	 *         contains the specified {@link Coding}, <code>false</code> if it
	 *         does not
	 */
	static boolean isCodeInConcept(CodeableConcept concept, String codingSystem, String codingVersion,
			String codingCode) {
		return concept.getCoding().stream().anyMatch(c -> {
			if (!codingSystem.equals(c.getSystem()))
				return false;
			if (codingVersion != null && !codingVersion.equals(c.getVersion()))
				return false;
			if (!codingCode.equals(c.getCode()))
				return false;

			return true;
		});
	}

	/**
	 * @param localDate
	 *            the {@link LocalDate} to convert
	 * @return a {@link Date} version of the specified {@link LocalDate}
	 */
	private static Date convertToDate(LocalDate localDate) {
		/*
		 * We use the system TZ here to ensure that the date doesn't shift at
		 * all, as FHIR will just use this as an unzoned Date (I think, and if
		 * not, it's almost certainly using the same TZ as this system).
		 */
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 * @return a {@link CodeableConcept} with the specified {@link Coding}
	 */
	private static CodeableConcept createCodeableConcept(String codingSystem, String codingCode) {
		return createCodeableConcept(codingSystem, null, codingCode);
	}

	/**
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingVersion
	 *            the {@link Coding#getVersion()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 * @return a {@link CodeableConcept} with the specified {@link Coding}
	 */
	private static CodeableConcept createCodeableConcept(String codingSystem, String codingVersion, String codingCode) {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding().setSystem(codingSystem).setCode(codingCode);
		if (codingVersion != null)
			coding.setVersion(codingVersion);
		return codeableConcept;
	}

	/**
	 * @param identifierSystem
	 *            the {@link Identifier#getSystem()} to use in
	 *            {@link Reference#getIdentifier()}
	 * @param identifierValue
	 *            the {@link Identifier#getValue()} to use in
	 *            {@link Reference#getIdentifier()}
	 * @return a {@link Reference} with the specified {@link Identifier}
	 */
	private static Reference createIdentifierReference(String identifierSystem, String identifierValue) {
		return new Reference().setIdentifier(new Identifier().setSystem(identifierSystem).setValue(identifierValue));
	}

	/**
	 * validate the from/thru dates to ensure the from date is before or the
	 * same as the thru date
	 * 
	 * @param dateFrom
	 *            start date {@link LocalDate}
	 * @param dateThrough
	 *            through date {@link LocalDate} to verify
	 */
	private static void validatePeriodDates(LocalDate dateFrom, LocalDate dateThrough) {
		if (dateFrom == null)
			return;
		if (dateThrough == null)
			return;
		// FIXME see CBBD-236 (ETL service fails on some Hospice claims "From
		// date is after the Through Date")
		// We are seeing this scenario in production where the from date is
		// after the through date so we are just logging the error for now.
		if (dateFrom.isAfter(dateThrough))
			LOGGER.debug(String.format("Error - From Date '%s' is after the Through Date '%s'", dateFrom, dateThrough));
	}

	/**
	 * validate the <Optional>from/<Optional>thru dates to ensure the from date
	 * is before or the same as the thru date
	 * 
	 * @param <Optional>dateFrom
	 *            start date {@link <Optional>LocalDate}
	 * @param <Optional>dateThrough
	 *            through date {@link <Optional>LocalDate} to verify
	 */
	private static void validatePeriodDates(Optional<LocalDate> dateFrom, Optional<LocalDate> dateThrough) {
		if (!dateFrom.isPresent())
			return;
		if (!dateThrough.isPresent())
			return;
		validatePeriodDates(dateFrom.get(), dateThrough.get());
	}
}
