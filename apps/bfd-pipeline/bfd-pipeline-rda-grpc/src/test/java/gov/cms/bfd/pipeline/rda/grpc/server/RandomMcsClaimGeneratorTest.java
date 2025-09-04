package gov.cms.bfd.pipeline.rda.grpc.server;

import static gov.cms.bfd.pipeline.rda.grpc.server.AbstractRandomClaimGeneratorTest.countDistinctFieldValues;
import static gov.cms.bfd.pipeline.rda.grpc.server.AbstractRandomClaimGeneratorTest.maxFieldLength;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/** Tests the {@link RandomMcsClaimGenerator}. */
public class RandomMcsClaimGeneratorTest {
  /**
   * Verifies when a single MCS claim is generated using a seed, it's fields match the expected
   * values.
   *
   * @throws InvalidProtocolBufferException indicates test failure (failure to parse to json for
   *     comparison)
   */
  @Test
  public void randomClaim() throws InvalidProtocolBufferException {
    final Clock july1 = Clock.fixed(Instant.ofEpochMilli(1625172944844L), ZoneOffset.UTC);
    final RandomMcsClaimGenerator generator =
        new RandomMcsClaimGenerator(
            RandomClaimGeneratorConfig.builder()
                .seed(1)
                .optionalOverride(true)
                .clock(july1)
                .build());
    final McsClaim claim = generator.randomClaim();
    final String json = JsonFormat.printer().print(claim);
    assertEquals(1, generator.getPreviousSequenceNumber());
    assertEquals(claim.getIdrDtlCnt(), claim.getMcsDetailsList().size());
    for (McsDiagnosisCode diagnosisCode : claim.getMcsDiagnosisCodesList()) {
      assertEquals(claim.getIdrClmHdIcn(), diagnosisCode.getIdrClmHdIcn());
    }
    assertEquals(
"""
{
  "idrClmHdIcn": "543185449367619",
  "idrContrId": "0546",
  "idrHic": "935301121",
  "idrDtlCnt": 3,
  "idrBeneLast16": "bg",
  "idrBeneFirstInit": "c",
  "idrBeneMidInit": "k",
  "idrBeneSexEnum": "BENEFICIARY_SEX_MALE",
  "idrStatusCodeEnum": "STATUS_CODE_ADJUSTED",
  "idrStatusDate": "2021-05-24",
  "idrBillProvNpi": "db",
  "idrBillProvNum": "9743",
  "idrBillProvEin": "zrcfgs7",
  "idrBillProvType": "h",
  "idrBillProvSpec": "cm",
  "idrBillProvGroupIndEnum": "BILLING_PROVIDER_INDICATOR_GROUP",
  "idrBillProvPriceSpec": "1",
  "idrBillProvCounty": "1r",
  "idrBillProvLoc": "84",
  "idrTotAllowed": "50055.81",
  "idrCoinsurance": "95.89",
  "idrDeductible": "9894.24",
  "idrTotBilledAmt": "866.34",
  "idrClaimReceiptDate": "2021-06-18",
  "idrClaimMbi": "k71641b7gm3",
  "mcsDiagnosisCodes": [{
    "idrClmHdIcn": "543185449367619",
    "idrDiagCode": "p",
    "idrDiagIcdTypeUnrecognized": "s",
    "rdaPosition": 1
  }, {
    "idrClmHdIcn": "543185449367619",
    "idrDiagIcdTypeEnum": "DIAGNOSIS_ICD_TYPE_ICD10",
    "idrDiagCode": "c3pz04",
    "rdaPosition": 2
  }, {
    "idrClmHdIcn": "543185449367619",
    "idrDiagIcdTypeEnum": "DIAGNOSIS_ICD_TYPE_ICD9",
    "idrDiagCode": "3hm99",
    "rdaPosition": 3
  }, {
    "idrClmHdIcn": "543185449367619",
    "idrDiagCode": "c",
    "idrDiagIcdTypeUnrecognized": "s",
    "rdaPosition": 4
  }, {
    "idrClmHdIcn": "543185449367619",
    "idrDiagCode": "g88",
    "idrDiagIcdTypeUnrecognized": "d",
    "rdaPosition": 5
  }, {
    "idrClmHdIcn": "543185449367619",
    "idrDiagCode": "vwmb",
    "idrDiagIcdTypeUnrecognized": "x",
    "rdaPosition": 6
  }],
  "mcsDetails": [{
    "idrDtlStatusEnum": "DETAIL_STATUS_REJECTED",
    "idrDtlFromDate": "2021-03-08",
    "idrDtlToDate": "2021-03-08",
    "idrProcCode": "n",
    "idrModOne": "5",
    "idrModTwo": "k",
    "idrModThree": "q",
    "idrModFour": "v3",
    "idrDtlPrimaryDiagCode": "k",
    "idrKPosLnameOrg": "pggtbgjwkmztsfjvjzgtcpbkhfhmzxhpdnqsrzqjvbpgthdsxpsrk",
    "idrKPosFname": "pcrstwdwfthjxwbbmzgzjthbrfjr",
    "idrKPosMname": "gqkbtrk",
    "idrKPosAddr1": "4btxhsqcz68vc",
    "idrKPosAddr21st": "fgp2f0q408xvhcwv1fs7hz6r9pw",
    "idrKPosAddr22nd": "tsn",
    "idrKPosCity": "qbcmjpdsfzzhqwbrvrxdnjqcdzhp",
    "idrKPosState": "fr",
    "idrKPosZip": "1061715129",
    "idrDtlDiagIcdTypeUnrecognized": "p",
    "idrTosEnum": "TYPE_OF_SERVICE_ASSISTANCE_AT_SURGERY",
    "idrTwoDigitPosEnum": "TWO_DIGIT_PLAN_OF_SERVICE_COMPREHENSIVE_OUTPATIENT_REHABILITATION_FACILITY",
    "idrDtlRendType": "5n",
    "idrDtlRendSpec": "81",
    "idrDtlRendNpi": "2cjn",
    "idrDtlRendProv": "cr1bnkb3",
    "idrKDtlFacProvNpi": "x4wz142gr",
    "idrDtlAmbPickupAddres1": "0qvnn4rpj93q728c1d7n9x3m7",
    "idrDtlAmbPickupAddres2": "6z4dxbxvkv3",
    "idrDtlAmbPickupCity": "qs05mkp3bkm2s5c3ktm",
    "idrDtlAmbPickupState": "2",
    "idrDtlAmbPickupZipcode": "t01",
    "idrDtlAmbDropoffName": "djfj8",
    "idrDtlAmbDropoffAddrL1": "gwd0hb70d0gpb3px",
    "idrDtlAmbDropoffAddrL2": "b6cttknhfwg",
    "idrDtlAmbDropoffCity": "ds",
    "idrDtlAmbDropoffState": "01",
    "idrDtlAmbDropoffZipcode": "88jcc14",
    "idrDtlNdc": "hbqxc3p79vg4bfjkqsr5rgjv7pcvgjqr1zp9pqgcr",
    "idrDtlNdcUnitCount": "rp34mptht4wr",
    "idrDtlNumber": 1
  }, {
    "idrDtlFromDate": "2021-04-12",
    "idrDtlToDate": "2021-04-12",
    "idrProcCode": "k",
    "idrModOne": "n",
    "idrModTwo": "9",
    "idrModThree": "cb",
    "idrModFour": "0v",
    "idrDtlDiagIcdTypeEnum": "DIAGNOSIS_ICD_TYPE_ICD9",
    "idrDtlPrimaryDiagCode": "w",
    "idrKPosLnameOrg": "wxsdbmvqwhdpgvqkhnzmpqtzkdvczxhwxjmfbwwqshndhbpckrvcmngc",
    "idrKPosFname": "nmctkmdjkxgnsddnqthbwqbgcbzbjb",
    "idrKPosMname": "mpmsjjqxdhdrknb",
    "idrKPosAddr1": "0z4p95cqnq4tg6wg1wd1sdkxschm4nvzwbnwt05dg5tsw8q1xcwfw5",
    "idrKPosAddr21st": "0",
    "idrKPosAddr22nd": "6wmnd13g7gj61qs8w4rdxcht",
    "idrKPosCity": "wcgk",
    "idrKPosState": "kc",
    "idrKPosZip": "395749",
    "idrDtlStatusUnrecognized": "b",
    "idrTosEnum": "TYPE_OF_SERVICE_DME_PRESCRIPTION",
    "idrTwoDigitPosUnrecognized": "s",
    "idrDtlRendType": "x",
    "idrDtlRendSpec": "m",
    "idrDtlRendNpi": "dvb",
    "idrDtlRendProv": "p7bpvdkc",
    "idrKDtlFacProvNpi": "p061",
    "idrDtlAmbPickupAddres1": "6zzkc06vrf8qq70961rz2",
    "idrDtlAmbPickupAddres2": "v6tv",
    "idrDtlAmbPickupCity": "1d7gvd3gd198pdtk",
    "idrDtlAmbPickupState": "zz",
    "idrDtlAmbPickupZipcode": "bw9d",
    "idrDtlAmbDropoffName": "wfh4p5jgpdg5jszdrcb6j2ns",
    "idrDtlAmbDropoffAddrL1": "bs954dwjq3m85ndjdgdjwjcq",
    "idrDtlAmbDropoffAddrL2": "29dd4d3",
    "idrDtlAmbDropoffCity": "9hrhpnjnw8tq3c0m7jt4",
    "idrDtlAmbDropoffState": "hj",
    "idrDtlAmbDropoffZipcode": "r",
    "idrDtlNdc": "6dpzf1d5bmmgrdkzvph0r0kxqzxr",
    "idrDtlNdcUnitCount": "nnm1p1",
    "idrDtlNumber": 2
  }, {
    "idrDtlFromDate": "2021-03-29",
    "idrDtlToDate": "2021-03-29",
    "idrProcCode": "kzg4b",
    "idrModOne": "3j",
    "idrModTwo": "pj",
    "idrModThree": "k",
    "idrModFour": "n",
    "idrDtlPrimaryDiagCode": "07d",
    "idrKPosLnameOrg": "gqnrrphtwszfhdqvnbcsvgxsrpfzcdbfcxgstzhcrfbvznspht",
    "idrKPosFname": "hvr",
    "idrKPosMname": "tsdwjfsr",
    "idrKPosAddr1": "vd0c6mvws5hzmvmvtmbjcx7p9m1f1dvg0n03whcnb078f6m2wkjq",
    "idrKPosAddr21st": "xsg6qhr8f4n34c77gqzz7",
    "idrKPosAddr22nd": "sv3fzr7tzn3hw",
    "idrKPosCity": "cjmbrcs",
    "idrKPosState": "r",
    "idrKPosZip": "97239824074",
    "idrDtlDiagIcdTypeUnrecognized": "h",
    "idrDtlStatusUnrecognized": "n",
    "idrTosUnrecognized": "v",
    "idrTwoDigitPosEnum": "TWO_DIGIT_PLAN_OF_SERVICE_INDEPENDENT_CLINIC",
    "idrDtlRendType": "f",
    "idrDtlRendSpec": "z",
    "idrDtlRendNpi": "918q",
    "idrDtlRendProv": "kr23h9k4f6",
    "idrKDtlFacProvNpi": "tf9ncj",
    "idrDtlAmbPickupAddres1": "z",
    "idrDtlAmbPickupAddres2": "tzvvnxcst5zcc8pvb",
    "idrDtlAmbPickupCity": "zj2p94m3hjct",
    "idrDtlAmbPickupState": "d",
    "idrDtlAmbPickupZipcode": "2hx",
    "idrDtlAmbDropoffName": "rn93",
    "idrDtlAmbDropoffAddrL1": "g379h9z14qpwj",
    "idrDtlAmbDropoffAddrL2": "b36rjh56",
    "idrDtlAmbDropoffCity": "k435qcfd53whjf5txf",
    "idrDtlAmbDropoffState": "gs",
    "idrDtlAmbDropoffZipcode": "qqwq",
    "idrDtlNdc": "dmt6kw5bj7qsnzshvq1c91vv33fh7km81",
    "idrDtlNdcUnitCount": "7",
    "idrDtlNumber": 3
  }],
  "idrClaimTypeUnrecognized": "h",
  "idrBillProvStatusCdUnrecognized": "s",
  "idrHdrFromDos": "2021-03-08",
  "idrHdrToDos": "2021-04-12",
  "idrAssignmentEnum": "CLAIM_ASSIGNMENT_NON_ASSIGNED_LAB_SERVICES",
  "idrClmLevelIndUnrecognized": "9",
  "idrHdrAudit": 28109,
  "idrHdrAuditIndEnum": "AUDIT_INDICATOR_AUDIT_NUMBER",
  "idrUSplitReasonEnum": "SPLIT_REASON_CODE_KIDNEY_DONOR",
  "idrJReferringProvNpi": "h5xbd3",
  "idrJFacProvNpi": "brpb24",
  "idrUDemoProvNpi": "qbrsqrw9",
  "idrUSuperNpi": "8crf7m43j9",
  "idrUFcadjBilNpi": "g4gj4sf",
  "idrAmbPickupAddresLine1": "4jvn1w5z5706s3mw",
  "idrAmbPickupAddresLine2": "pst59d0",
  "idrAmbPickupCity": "xbn02z37f",
  "idrAmbPickupState": "37",
  "idrAmbPickupZipcode": "h1",
  "idrAmbDropoffName": "rjps29fczvd5f6g0fjppk",
  "idrAmbDropoffAddrLine1": "84m013kgpfxcbbpszzh977hzc",
  "idrAmbDropoffAddrLine2": "4ck32",
  "idrAmbDropoffCity": "f0qw",
  "idrAmbDropoffState": "x",
  "idrAmbDropoffZipcode": "ggtmg3wp",
  "mcsAudits": [{
    "idrJAuditNum": 32488,
    "idrJAuditIndEnum": "CUTBACK_AUDIT_INDICATOR_HEADER_EDIT_NUMBER",
    "idrJAuditDispUnrecognized": "x",
    "rdaPosition": 1
  }, {
    "idrJAuditNum": 6771,
    "idrJAuditIndEnum": "CUTBACK_AUDIT_INDICATOR__AUDIT_NUM_IS_ZEROES",
    "idrJAuditDispUnrecognized": "t",
    "rdaPosition": 2
  }, {
    "idrJAuditNum": 15930,
    "idrJAuditIndUnrecognized": "k",
    "idrJAuditDispEnum": "CUTBACK_AUDIT_DISPOSITION_MODIFIER_51",
    "rdaPosition": 3
  }, {
    "idrJAuditNum": 25878,
    "idrJAuditIndUnrecognized": "b",
    "idrJAuditDispEnum": "CUTBACK_AUDIT_DISPOSITION_REDUCE_BY_SUM_OF_PRIOR_PAYMENTS",
    "rdaPosition": 4
  }, {
    "idrJAuditNum": 1044,
    "idrJAuditIndUnrecognized": "t",
    "idrJAuditDispEnum": "CUTBACK_AUDIT_DISPOSITION_REDUCE_BY_SUM_OF_MATCHING_DATES",
    "rdaPosition": 5
  }, {
    "idrJAuditNum": 17441,
    "idrJAuditIndUnrecognized": "3",
    "idrJAuditDispEnum": "CUTBACK_AUDIT_DISPOSITION_EOMB_MESSAGE_ONLY",
    "rdaPosition": 6
  }, {
    "idrJAuditNum": 16211,
    "idrJAuditIndEnum": "CUTBACK_AUDIT_INDICATOR_HEADER_EDIT_NUMBER",
    "idrJAuditDispEnum": "CUTBACK_AUDIT_DISPOSITION_TRANSFER",
    "rdaPosition": 7
  }],
  "mcsLocations": [{
    "idrLocClerk": "t",
    "idrLocCode": "d",
    "idrLocDate": "2021-04-17",
    "idrLocActvCodeUnrecognized": "x",
    "rdaPosition": 1
  }, {
    "idrLocClerk": "9j",
    "idrLocCode": "6kc",
    "idrLocDate": "2021-03-04",
    "idrLocActvCodeUnrecognized": "f",
    "rdaPosition": 2
  }, {
    "idrLocClerk": "579",
    "idrLocCode": "3jb",
    "idrLocDate": "2021-05-21",
    "idrLocActvCodeUnrecognized": "3",
    "rdaPosition": 3
  }, {
    "idrLocClerk": "gzx",
    "idrLocCode": "z",
    "idrLocDate": "2021-05-22",
    "idrLocActvCodeUnrecognized": "w",
    "rdaPosition": 4
  }, {
    "idrLocClerk": "p5",
    "idrLocCode": "kd2",
    "idrLocDate": "2021-03-11",
    "idrLocActvCodeEnum": "LOCATION_ACTIVITY_CODE_MAINTENANCE_APPLIED_FROM_WORKSHEET_F",
    "rdaPosition": 5
  }, {
    "idrLocClerk": "5g7",
    "idrLocCode": "cg",
    "idrLocDate": "2021-05-11",
    "idrLocActvCodeEnum": "LOCATION_ACTIVITY_CODE_CORRECTION_DOCUMENT_REPRINT",
    "rdaPosition": 6
  }, {
    "idrLocClerk": "q6g3",
    "idrLocCode": "ttf",
    "idrLocDate": "2021-02-01",
    "idrLocActvCodeEnum": "LOCATION_ACTIVITY_CODE_CLAIM_IS_MISSING",
    "rdaPosition": 7
  }],
  "mcsAdjustments": [{
    "idrAdjDate": "2021-04-02",
    "idrXrefIcn": "j1ct",
    "idrAdjClerk": "gz",
    "idrInitCcn": "11hc3kgkv",
    "idrAdjChkWrtDt": "2021-01-03",
    "idrAdjBEombAmt": "380.38",
    "idrAdjPEombAmt": "685.12",
    "rdaPosition": 1
  }, {
    "idrAdjDate": "2021-02-13",
    "idrXrefIcn": "jvqb45f5xfvn",
    "idrAdjClerk": "jnz",
    "idrInitCcn": "qfrm7fs9ws7",
    "idrAdjChkWrtDt": "2021-06-26",
    "idrAdjBEombAmt": "3983.09",
    "idrAdjPEombAmt": "47308.33",
    "rdaPosition": 2
  }, {
    "idrAdjDate": "2021-02-26",
    "idrXrefIcn": "j14b9",
    "idrAdjClerk": "8",
    "idrInitCcn": "m81p0",
    "idrAdjChkWrtDt": "2021-06-20",
    "idrAdjBEombAmt": "8800.96",
    "idrAdjPEombAmt": "501.81",
    "rdaPosition": 3
  }, {
    "idrAdjDate": "2021-04-26",
    "idrXrefIcn": "s4kb8c49b34j18",
    "idrAdjClerk": "vn",
    "idrInitCcn": "hzf12cg",
    "idrAdjChkWrtDt": "2021-01-31",
    "idrAdjBEombAmt": "43.34",
    "idrAdjPEombAmt": "3.13",
    "rdaPosition": 4
  }, {
    "idrAdjDate": "2021-04-04",
    "idrXrefIcn": "rc9w3kvc67",
    "idrAdjClerk": "0dw7",
    "idrInitCcn": "mpvbt2p5fk7fpzf",
    "idrAdjChkWrtDt": "2021-05-09",
    "idrAdjBEombAmt": "166.38",
    "idrAdjPEombAmt": "714.80",
    "rdaPosition": 5
  }]
}""",
        json);
  }

  /** Verifies that the overrides in the {@link RandomClaimGeneratorConfig} are enforced. */
  @Test
  public void testFieldOverrides() {
    final int maxClaimIds = 18;
    final int maxMbis = 14;
    final var normalConfig = RandomClaimGeneratorConfig.builder().seed(1).build();
    final var normalGenerator = new RandomMcsClaimGenerator(normalConfig);
    final var normalClaims =
        IntStream.range(1, 100)
            .mapToObj(i -> normalGenerator.randomClaim())
            .collect(Collectors.toList());

    // We expect to normally get many more unique values than the overrides will use.
    assertThat(
        countDistinctFieldValues(normalClaims, McsClaim::getIdrClaimMbi),
        Matchers.greaterThan((long) maxClaimIds));
    assertThat(
        countDistinctFieldValues(normalClaims, McsClaim::getIdrClmHdIcn),
        Matchers.greaterThan((long) maxMbis));

    // We expect these ids to normally fall within their normal field length.
    assertThat(
        maxFieldLength(normalClaims, McsClaim::getIdrContrId),
        Matchers.lessThan(RandomMcsClaimGenerator.ForcedErrorFieldLength));
    assertThat(
        maxFieldLength(normalClaims, McsClaim::getIdrHic),
        Matchers.lessThan(RandomMcsClaimGenerator.ForcedErrorFieldLength));

    final var overrideConfig =
        normalConfig.toBuilder()
            .maxUniqueClaimIds(maxClaimIds)
            .maxUniqueMbis(maxMbis)
            .randomErrorRate(10)
            .build();
    final var overrideGenerator = new RandomMcsClaimGenerator(overrideConfig);
    final var overrideClaims =
        IntStream.range(1, 100)
            .mapToObj(i -> overrideGenerator.randomClaim())
            .collect(Collectors.toList());

    // We expect to get exactly the specified number of unique ids.
    assertEquals(maxClaimIds, countDistinctFieldValues(overrideClaims, McsClaim::getIdrClmHdIcn));
    assertEquals(maxMbis, countDistinctFieldValues(overrideClaims, McsClaim::getIdrClaimMbi));

    // We expect these ids to sometimes have a value with the forced error length when we are
    // forcing errors.
    assertEquals(
        RandomMcsClaimGenerator.ForcedErrorFieldLength,
        maxFieldLength(overrideClaims, McsClaim::getIdrContrId));
    assertEquals(
        RandomMcsClaimGenerator.ForcedErrorFieldLength,
        maxFieldLength(overrideClaims, McsClaim::getIdrHic));
  }
}
