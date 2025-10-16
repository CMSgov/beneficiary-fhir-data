package gov.cms.bfd.pipeline.rda.grpc.server;

import static gov.cms.bfd.pipeline.rda.grpc.server.AbstractRandomClaimGeneratorTest.countDistinctFieldValues;
import static gov.cms.bfd.pipeline.rda.grpc.server.AbstractRandomClaimGeneratorTest.maxFieldLength;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link RandomFissClaimGenerator}. */
public class RandomFissClaimGeneratorTest {

  /**
   * Asserts that given a set seed and clock, the random generator creates an expected claim for
   * ensuring that tests using a randomly generated claim will be repeatable.
   *
   * @throws InvalidProtocolBufferException if there is an issue turning the generated claim into
   *     json for comparison
   */
  @Test
  public void randomClaim() throws InvalidProtocolBufferException {
    final Clock july1 = Clock.fixed(Instant.ofEpochMilli(1625172944844L), ZoneOffset.UTC);
    final RandomFissClaimGenerator generator =
        new RandomFissClaimGenerator(
            RandomClaimGeneratorConfig.builder()
                .seed(1)
                .optionalOverride(true)
                .clock(july1)
                .build());
    final FissClaim claim = generator.randomClaim();
    final String json = JsonFormat.printer().print(claim);
    assertEquals(1, generator.getPreviousSequenceNumber());
    assertEquals(
"""
{
  "dcn": "65686447345742655016681",
  "hicNo": "897538270399",
  "currStatusEnum": "CLAIM_STATUS_BLANK",
  "provStateCd": "m4",
  "provTypFacilCd": "n",
  "provEmerInd": "j",
  "provDeptId": "c01",
  "totalChargeAmount": "8878.88",
  "recdDtCymd": "2021-01-15",
  "currTranDtCymd": "2021-05-07",
  "admDiagCode": "sbzzv",
  "principleDiag": "pkvrk",
  "npiNumber": "3207789530",
  "mbi": "wkh52g7nbkf",
  "fedTaxNb": "9738554920",
  "fissProcCodes": [{
    "procCd": "pkvrk",
    "rdaPosition": 1,
    "procFlag": "htx",
    "procDt": "2021-04-02"
  }, {
    "procCd": "cmhfc",
    "rdaPosition": 2,
    "procFlag": "k",
    "procDt": "2021-06-26"
  }, {
    "procCd": "frq",
    "rdaPosition": 3,
    "procFlag": "f",
    "procDt": "2021-03-12"
  }],
  "medaProvId": "wkhcjxjwqp8bq",
  "pracLocAddr1": "vvv1m6fpjbn33zzp75s2wc9dnqvpcscm7458n7t7hjndfh2v843q3sm",
  "pracLocAddr2": "194hz2pmknchmbs45zzz",
  "pracLocCity": "6wvgbtc3mcghzjz0kzcwxm9scfsq9xpwnx490p0twzrzxc",
  "pracLocState": "fc",
  "pracLocZip": "062130",
  "currLoc1Unrecognized": "r",
  "currLoc2Unrecognized": "jzsq",
  "stmtCovFromCymd": "2021-03-24",
  "stmtCovToCymd": "2021-03-18",
  "medaProv6": "wkhcjx",
  "lobCdEnum": "BILL_FACILITY_TYPE_RELIGIOUS_NON_MEDICAL_EXTENDED_CARE",
  "servTypeCdForClinicsEnum": "BILL_CLASSIFICATION_FOR_CLINICS_COMPREHENSIVE_OUTPATIENT_REHABILITATION_FACILITY",
  "freqCdUnrecognized": "d",
  "billTypCd": "rnh",
  "fissPayers": [{
    "beneZPayer": {
      "rdaPosition": 1,
      "payersIdEnum": "PAYERS_CODE_WORKERS_COMPENSATION",
      "payersName": "db4dq8",
      "relIndEnum": "RELEASE_OF_INFORMATION_SIGNED_STATEMENT_WAS_OBTAINED",
      "assignIndEnum": "ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED",
      "providerNumber": "4p",
      "adjDcnIcn": "s6m075",
      "priorPmt": "89196.37",
      "estAmtDue": "152.69",
      "beneRelUnrecognized": "71",
      "beneLastName": "mfjp",
      "beneFirstName": "qdbpbhfkfm",
      "beneMidInit": "z",
      "beneSsnHic": "dkkgpn36g0dg7xnk1w3",
      "insuredGroupName": "zfwnnzvxgw",
      "beneDob": "2021-02-08",
      "beneSexEnum": "BENEFICIARY_SEX_UNKNOWN",
      "treatAuthCd": "x",
      "insuredSexEnum": "BENEFICIARY_SEX_FEMALE",
      "insuredRelX12Unrecognized": "74"
    }
  }, {
    "beneZPayer": {
      "rdaPosition": 2,
      "payersIdEnum": "PAYERS_CODE_CONDITIONAL_PAYMENT",
      "payersName": "80gcv323nqt5trhn89b",
      "relIndUnrecognized": "z",
      "assignIndEnum": "ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED",
      "providerNumber": "zt398xkjmk5g6",
      "adjDcnIcn": "b7dz5rbmtb0045p95tq",
      "priorPmt": "20666.98",
      "estAmtDue": "12.23",
      "beneRelEnum": "PATIENT_RELATIONSHIP_CODE_RESERVED_FOR_NATIONAL_ASSIGNMENT_77",
      "beneLastName": "b",
      "beneFirstName": "xnmzvkbwh",
      "beneMidInit": "r",
      "beneSsnHic": "vf2zg8gpj91jv4jrcjx",
      "insuredGroupName": "sftr",
      "beneDob": "2021-01-13",
      "beneSexUnrecognized": "d",
      "treatAuthCd": "x",
      "insuredSexEnum": "BENEFICIARY_SEX_UNKNOWN",
      "insuredRelX12Enum": "PATIENT_RELATIONSHIP_CODE_LIFE_PARTNER"
    }
  }, {
    "beneZPayer": {
      "rdaPosition": 3,
      "payersIdEnum": "PAYERS_CODE_WORKERS_COMPENSATION",
      "payersName": "f4gmk4w72bkjz3smccstmpsg80s8",
      "relIndEnum": "RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE",
      "assignIndEnum": "ASSIGNMENT_OF_BENEFITS_INDICATOR_NO_BENEFITS_ASSIGNED",
      "providerNumber": "r9d3c54fgwhd9",
      "adjDcnIcn": "d0jmmb7n",
      "priorPmt": "48704.98",
      "estAmtDue": "61751.08",
      "beneRelEnum": "PATIENT_RELATIONSHIP_CODE_RESERVED_FOR_NATIONAL_ASSIGNMENT_74",
      "beneLastName": "sgxkjmtxmmnm",
      "beneFirstName": "svbkg",
      "beneMidInit": "b",
      "beneSsnHic": "k842jj",
      "insuredGroupName": "fzhtjjrwtznbqb",
      "beneDob": "2021-06-02",
      "beneSexEnum": "BENEFICIARY_SEX_UNKNOWN",
      "treatAuthCd": "q",
      "insuredSexEnum": "BENEFICIARY_SEX_UNKNOWN",
      "insuredRelX12Unrecognized": "95"
    }
  }, {
    "beneZPayer": {
      "rdaPosition": 4,
      "payersIdUnrecognized": "p",
      "payersName": "ktz52n4qdbjchsm50crttb78gg",
      "relIndUnrecognized": "p",
      "assignIndUnrecognized": "h",
      "providerNumber": "fqr2mtz5",
      "adjDcnIcn": "3t46kt",
      "priorPmt": "2642.52",
      "estAmtDue": "9823.82",
      "beneRelUnrecognized": "13",
      "beneLastName": "mtc",
      "beneFirstName": "kmgr",
      "beneMidInit": "r",
      "beneSsnHic": "fpgb1k2",
      "insuredGroupName": "zczrfwnvj",
      "beneDob": "2021-01-26",
      "beneSexUnrecognized": "p",
      "treatAuthCd": "d",
      "insuredSexEnum": "BENEFICIARY_SEX_MALE",
      "insuredRelX12Unrecognized": "18"
    }
  }, {
    "beneZPayer": {
      "rdaPosition": 5,
      "payersIdUnrecognized": "q",
      "payersName": "gkzg426s4qh5tc296xmdx3spzt9",
      "relIndUnrecognized": "f",
      "assignIndUnrecognized": "n",
      "providerNumber": "gdxv543gdk",
      "adjDcnIcn": "535680wxr8",
      "priorPmt": "516.92",
      "estAmtDue": "4.00",
      "beneRelUnrecognized": "77",
      "beneLastName": "qsfvjn",
      "beneFirstName": "ggrj",
      "beneMidInit": "q",
      "beneSsnHic": "wgtf2xxhj0mqh",
      "insuredGroupName": "wtdxqpvfxxhvptz",
      "beneDob": "2021-03-11",
      "beneSexEnum": "BENEFICIARY_SEX_UNKNOWN",
      "treatAuthCd": "f",
      "insuredSexUnrecognized": "p",
      "insuredRelX12Unrecognized": "03"
    }
  }],
  "fissAuditTrail": [{
    "badtStatusEnum": "CLAIM_STATUS_MOVE",
    "badtLoc": "f0b",
    "badtOperId": "dk3qkj8",
    "badtReas": "c1rg",
    "badtCurrDateCymd": "2021-01-25",
    "rdaPosition": 1
  }, {
    "badtStatusUnrecognized": "c",
    "badtLoc": "f461",
    "badtOperId": "pz9kx",
    "badtReas": "5n",
    "badtCurrDateCymd": "2021-06-14",
    "rdaPosition": 2
  }, {
    "badtStatusEnum": "CLAIM_STATUS_SUSPEND",
    "badtLoc": "f",
    "badtOperId": "g4348xj",
    "badtReas": "21t",
    "badtCurrDateCymd": "2021-06-05",
    "rdaPosition": 3
  }, {
    "badtStatusUnrecognized": "s",
    "badtLoc": "f80d",
    "badtOperId": "f",
    "badtReas": "t",
    "badtCurrDateCymd": "2021-01-31",
    "rdaPosition": 4
  }, {
    "badtStatusUnrecognized": "m",
    "badtLoc": "d0m13",
    "badtOperId": "vhjc5",
    "badtReas": "jwf2",
    "badtCurrDateCymd": "2021-06-07",
    "rdaPosition": 5
  }, {
    "badtStatusEnum": "CLAIM_STATUS_ROUTING",
    "badtLoc": "pfhhf",
    "badtOperId": "3vqx4q0",
    "badtReas": "h6hb",
    "badtCurrDateCymd": "2021-03-12",
    "rdaPosition": 6
  }, {
    "badtStatusEnum": "CLAIM_STATUS_RTP",
    "badtLoc": "3",
    "badtOperId": "gsxvnqj",
    "badtReas": "cf",
    "badtCurrDateCymd": "2021-05-14",
    "rdaPosition": 7
  }, {
    "badtStatusUnrecognized": "8",
    "badtLoc": "ww",
    "badtOperId": "fcgmpskq",
    "badtReas": "k7th8",
    "badtCurrDateCymd": "2021-03-11",
    "rdaPosition": 8
  }, {
    "badtStatusUnrecognized": "0",
    "badtLoc": "d",
    "badtOperId": "r616",
    "badtReas": "5f2dm",
    "badtCurrDateCymd": "2021-03-22",
    "rdaPosition": 9
  }, {
    "badtStatusEnum": "CLAIM_STATUS_FORCE",
    "badtLoc": "99hj",
    "badtOperId": "j843h15ms",
    "badtReas": "s",
    "badtCurrDateCymd": "2021-01-16",
    "rdaPosition": 10
  }, {
    "badtStatusEnum": "CLAIM_STATUS_MOVE",
    "badtLoc": "1cv05",
    "badtOperId": "g",
    "badtReas": "1v",
    "badtCurrDateCymd": "2021-03-17",
    "rdaPosition": 11
  }, {
    "badtStatusUnrecognized": "2",
    "badtLoc": "jg9",
    "badtOperId": "dpnk1pf",
    "badtReas": "z8",
    "badtCurrDateCymd": "2021-04-17",
    "rdaPosition": 12
  }, {
    "badtStatusEnum": "CLAIM_STATUS_RETURN_TO_PRO",
    "badtLoc": "8j",
    "badtOperId": "f0q5tnw",
    "badtReas": "9v",
    "badtCurrDateCymd": "2021-01-13",
    "rdaPosition": 13
  }],
  "rejectCd": "87",
  "fullPartDenInd": "f",
  "nonPayInd": "zq",
  "xrefDcnNbr": "0jn2sgcvmdwz",
  "adjReqCdUnrecognized": "m",
  "adjReasCd": "tv",
  "cancelXrefDcn": "92141675m",
  "cancelDateCymd": "2021-02-03",
  "cancAdjCdEnum": "CANCEL_ADJUSTMENT_CODE_PROVIDER_CANCELLED_EPISODE",
  "originalXrefDcn": "v8xh",
  "paidDtCymd": "2021-06-03",
  "admDateCymd": "2021-06-17",
  "admSourceUnrecognized": "7",
  "primaryPayerCodeEnum": "PAYERS_CODE_WORKING_AGE",
  "attendPhysId": "tq3hcd1",
  "attendPhysLname": "0v4b0n",
  "attendPhysFname": "g4k4kbjfgrhgrs5",
  "attendPhysMint": "t",
  "attendPhysFlagEnum": "PHYSICIAN_FLAG_NO",
  "operatingPhysId": "dqkq4t7kz7r",
  "operPhysLname": "h9pps",
  "operPhysFname": "thqzvzsm014jsdk",
  "operPhysMint": "d",
  "operPhysFlagEnum": "PHYSICIAN_FLAG_NO",
  "othPhysId": "9r79nmd598",
  "othPhysLname": "q94s0r",
  "othPhysFname": "8dt9w",
  "othPhysMint": "0",
  "othPhysFlagUnrecognized": "r",
  "xrefHicNbr": "3xbtpc",
  "procNewHicIndEnum": "PROCESS_NEW_HIC_INDICATOR_Y",
  "newHic": "d",
  "reposIndUnrecognized": "c",
  "reposHic": "qnv80",
  "mbiSubmBeneIndUnrecognized": "7",
  "adjMbiIndEnum": "ADJUSTMENT_MBI_INDICATOR_HIC_SUBMITTED_ON_ADJUSTMENT_OR_CANCEL_CLAIM",
  "adjMbi": "wnkf643w5",
  "medicalRecordNo": "z3t566hsc3s17",
  "stmtCovFromCymdText": "2021-03-24",
  "stmtCovToCymdText": "2021-03-18",
  "admDateCymdText": "2021-06-17",
  "fissRevenueLines": [{
    "rdaPosition": 1,
    "nonBillRevCodeEnum": "NON_BILL_S",
    "revCd": "26kn",
    "revUnitsBilled": 21,
    "revServUnitCnt": 7,
    "servDtCymd": "2021-01-19",
    "servDtCymdText": "2021-01-19",
    "hcpcCd": "9d",
    "hcpcInd": "w",
    "hcpcModifier": "6w",
    "hcpcModifier2": "5",
    "hcpcModifier3": "6",
    "hcpcModifier4": "r",
    "hcpcModifier5": "f",
    "acoRedRarc": "1x4",
    "acoRedCarc": "g",
    "acoRedCagc": "nz",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_GR",
    "ndc": "jcmf6sx",
    "ndcQty": "7936840010"
  }, {
    "rdaPosition": 2,
    "nonBillRevCodeUnrecognized": "m",
    "revCd": "t",
    "revUnitsBilled": 39,
    "revServUnitCnt": 15,
    "servDtCymd": "2021-01-15",
    "servDtCymdText": "2021-01-15",
    "hcpcCd": "jqq",
    "hcpcInd": "v",
    "hcpcModifier": "22",
    "hcpcModifier2": "zw",
    "hcpcModifier3": "t6",
    "hcpcModifier4": "j7",
    "hcpcModifier5": "df",
    "acoRedRarc": "p",
    "acoRedCarc": "t0",
    "acoRedCagc": "tm",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_F2",
    "ndc": "dnmvqsj",
    "ndcQty": "1309901"
  }, {
    "rdaPosition": 3,
    "nonBillRevCodeEnum": "NON_BILL_INVALID_REV_CODE",
    "revCd": "mrjt",
    "revUnitsBilled": 12,
    "revServUnitCnt": 19,
    "servDtCymd": "2021-01-20",
    "servDtCymdText": "2021-01-20",
    "hcpcCd": "0",
    "hcpcInd": "0",
    "hcpcModifier": "b",
    "hcpcModifier2": "n",
    "hcpcModifier3": "b",
    "hcpcModifier4": "7",
    "hcpcModifier5": "8",
    "acoRedRarc": "c1c7",
    "acoRedCarc": "67",
    "acoRedCagc": "tv",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_F2",
    "ndc": "nt4chn640z0",
    "ndcQty": "6727587507"
  }, {
    "rdaPosition": 4,
    "nonBillRevCodeEnum": "NON_BILL_INVALID_HCPCS_HC",
    "revCd": "cfb",
    "revUnitsBilled": 45,
    "revServUnitCnt": 48,
    "servDtCymd": "2021-06-27",
    "servDtCymdText": "2021-06-27",
    "hcpcCd": "d",
    "hcpcInd": "d",
    "hcpcModifier": "s",
    "hcpcModifier2": "mr",
    "hcpcModifier3": "g1",
    "hcpcModifier4": "68",
    "hcpcModifier5": "rg",
    "acoRedRarc": "ntq",
    "acoRedCarc": "z7",
    "acoRedCagc": "3x",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_UN",
    "ndc": "p4g",
    "ndcQty": "4149"
  }, {
    "rdaPosition": 5,
    "nonBillRevCodeEnum": "NON_BILL_S",
    "revCd": "2zb",
    "revUnitsBilled": 17,
    "revServUnitCnt": 23,
    "servDtCymd": "2021-05-27",
    "servDtCymdText": "2021-05-27",
    "hcpcCd": "tnr5",
    "hcpcInd": "j",
    "hcpcModifier": "48",
    "hcpcModifier2": "7",
    "hcpcModifier3": "w",
    "hcpcModifier4": "r",
    "hcpcModifier5": "f",
    "acoRedRarc": "x",
    "acoRedCarc": "s95",
    "acoRedCagc": "k2",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_F2",
    "ndc": "612jg4h6gx",
    "ndcQty": "253"
  }, {
    "rdaPosition": 6,
    "nonBillRevCodeEnum": "NON_BILL_ESRD",
    "revCd": "vmtw",
    "revUnitsBilled": 9,
    "revServUnitCnt": 23,
    "servDtCymd": "2021-02-17",
    "servDtCymdText": "2021-02-17",
    "hcpcCd": "s",
    "hcpcInd": "8",
    "hcpcModifier": "6p",
    "hcpcModifier2": "s",
    "hcpcModifier3": "c",
    "hcpcModifier4": "b",
    "hcpcModifier5": "x",
    "acoRedRarc": "svf7",
    "acoRedCarc": "5",
    "acoRedCagc": "xv",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_ME",
    "ndc": "ps",
    "ndcQty": "14975409"
  }, {
    "rdaPosition": 7,
    "nonBillRevCodeEnum": "NON_BILL_T",
    "revCd": "nzjd",
    "revUnitsBilled": 38,
    "revServUnitCnt": 9,
    "servDtCymd": "2021-03-06",
    "servDtCymdText": "2021-03-06",
    "hcpcCd": "866",
    "hcpcInd": "s",
    "hcpcModifier": "p",
    "hcpcModifier2": "1v",
    "hcpcModifier3": "p3",
    "hcpcModifier4": "j0",
    "hcpcModifier5": "8m",
    "acoRedRarc": "sh1zj",
    "acoRedCarc": "054",
    "acoRedCagc": "93",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_ML",
    "ndc": "9nrj",
    "ndcQty": "98448896"
  }, {
    "rdaPosition": 8,
    "nonBillRevCodeEnum": "NON_BILL_INVALID_HCPCS_EMC",
    "revCd": "42v4",
    "revUnitsBilled": 12,
    "revServUnitCnt": 33,
    "servDtCymd": "2021-05-21",
    "servDtCymdText": "2021-05-21",
    "hcpcCd": "r7p",
    "hcpcInd": "3",
    "hcpcModifier": "6",
    "hcpcModifier2": "r",
    "hcpcModifier3": "m",
    "hcpcModifier4": "0",
    "hcpcModifier5": "6",
    "acoRedRarc": "df57b",
    "acoRedCarc": "rt",
    "acoRedCagc": "q",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_ME",
    "ndc": "36g",
    "ndcQty": "36115325290"
  }, {
    "rdaPosition": 9,
    "nonBillRevCodeEnum": "NON_BILL_INVALID_REV_CODE",
    "revCd": "ck7",
    "revUnitsBilled": 1,
    "revServUnitCnt": 9,
    "servDtCymd": "2021-03-30",
    "servDtCymdText": "2021-03-30",
    "hcpcCd": "kmzjs",
    "hcpcInd": "t",
    "hcpcModifier": "hc",
    "hcpcModifier2": "z",
    "hcpcModifier3": "q",
    "hcpcModifier4": "k",
    "hcpcModifier5": "9",
    "acoRedRarc": "rjfgw",
    "acoRedCarc": "2xb",
    "acoRedCagc": "1",
    "ndcQtyQualEnum": "NDC_QTY_QUAL_UN",
    "ndc": "dksxpmx1dt",
    "ndcQty": "09102"
  }],
  "drgCd": "0",
  "groupCode": "1",
  "clmTypIndEnum": "CLAIM_TYPE_TOB_X20_X28",
  "recdDtCymdText": "2021-01-15",
  "currTranDtCymdText": "2021-05-07",
  "rdaClaimKey": "79173353508803659401417553844541",
  "intermediaryNb": "82320",
  "admTypCdEnum": "ADM_TYPE_9"
}""",
        json);
  }

  /** Verifies that the overrides in the {@link RandomClaimGeneratorConfig} are enforced. */
  @Test
  public void testFieldOverrides() {
    final int maxClaimIds = 18;
    final int maxMbis = 14;
    final var normalConfig = RandomClaimGeneratorConfig.builder().seed(1).build();
    final var normalGenerator = new RandomFissClaimGenerator(normalConfig);
    final var normalClaims =
        IntStream.range(1, 100)
            .mapToObj(i -> normalGenerator.randomClaim())
            .collect(Collectors.toList());

    // We expect to normally get many more unique values than the overrides will use.
    assertThat(
        countDistinctFieldValues(normalClaims, FissClaim::getRdaClaimKey),
        Matchers.greaterThan((long) maxClaimIds));
    assertThat(
        countDistinctFieldValues(normalClaims, FissClaim::getMbi),
        Matchers.greaterThan((long) maxMbis));

    // We expect these ids to normally fall within their normal field length.
    assertThat(
        maxFieldLength(normalClaims, FissClaim::getDcn),
        Matchers.lessThan(RandomMcsClaimGenerator.ForcedErrorFieldLength));
    assertThat(
        maxFieldLength(normalClaims, FissClaim::getIntermediaryNb),
        Matchers.lessThan(RandomMcsClaimGenerator.ForcedErrorFieldLength));

    final var overrideConfig =
        normalConfig.toBuilder()
            .maxUniqueClaimIds(maxClaimIds)
            .maxUniqueMbis(maxMbis)
            .randomErrorRate(10)
            .build();
    final var overrideGenerator = new RandomFissClaimGenerator(overrideConfig);
    final var overrideClaims =
        IntStream.range(1, 100)
            .mapToObj(i -> overrideGenerator.randomClaim())
            .collect(Collectors.toList());

    // We expect to get exactly the specified number of unique ids.
    assertEquals(maxClaimIds, countDistinctFieldValues(overrideClaims, FissClaim::getRdaClaimKey));
    assertEquals(maxMbis, countDistinctFieldValues(overrideClaims, FissClaim::getMbi));

    // We expect these ids to sometimes have a value with the forced error length when we are
    // forcing errors.
    assertEquals(
        RandomMcsClaimGenerator.ForcedErrorFieldLength,
        maxFieldLength(overrideClaims, FissClaim::getDcn));
    assertEquals(
        RandomMcsClaimGenerator.ForcedErrorFieldLength,
        maxFieldLength(overrideClaims, FissClaim::getIntermediaryNb));
  }
}
