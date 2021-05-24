package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaim;
import gov.cms.mpsm.rda.v1.FissProcCodes;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class FissClaimTransformerTest {
  // using a fixed Clock ensures our timestamp is predictable
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(clock, IdHasher.createInstanceForTestingOnly());
  private FissClaim.Builder builder;
  private PreAdjFissClaim claim;

  @Before
  public void setUp() throws Exception {
    builder = FissClaim.newBuilder();
    claim = new PreAdjFissClaim();
  }

  @Test
  public void minimumValidClaim() {
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('S');
    claim.setCurrLoc1('A');
    claim.setCurrLoc2("two");
    claim.setLastUpdated(clock.instant());
    builder.setDcn("dcn").setHicNo("hicn").setCurrStatus("S").setCurrLoc1("A").setCurrLoc2("two");
    assertThat(transformer.transformClaim(builder.build()), samePropertyValuesAs(claim));
  }

  @Test
  public void allFields() {
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('S');
    claim.setCurrLoc1('A');
    claim.setCurrLoc2("two");
    claim.setMedaProvId("mpi");
    claim.setTotalChargeAmount(new BigDecimal("1002.54"));
    claim.setReceivedDate(LocalDate.of(2020, 1, 2));
    claim.setCurrTranDate(LocalDate.of(2021, 3, 4));
    claim.setAdmitDiagCode("1234567");
    claim.setPrincipleDiag("7654321");
    claim.setNpiNumber("npi-123456");
    claim.setMbi("1234567890123");
    claim.setMbiHash("d51b083f91c62eff93b6245bc8203bafa566f41b3553314d049059b8e55eea0d");
    claim.setFedTaxNumber("1234567890");
    claim.setLastUpdated(clock.instant());
    builder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatus("S")
        .setCurrLoc1("A")
        .setCurrLoc2("two")
        .setMedaProvId("mpi")
        .setTotalChargeAmount("1002.54")
        .setRecdDt("2020-01-02")
        .setCurrTranDate("2021-03-04")
        .setAdmDiagCode("1234567")
        .setPrincipleDiag("7654321")
        .setNpiNumber("npi-123456")
        .setMbi("1234567890123")
        .setFedTaxNb("1234567890");
    assertThat(transformer.transformClaim(builder.build()), samePropertyValuesAs(claim));
  }

  @Test
  public void procCodes() {
    builder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatus("A")
        .setCurrLoc1("1")
        .setCurrLoc2("2")
        .addFissProcCodes(
            FissProcCodes.newBuilder().setProcCd("code-1").setProcFlag("fl-1").build())
        .addFissProcCodes(
            FissProcCodes.newBuilder()
                .setProcCd("code-2")
                .setProcFlag("fl-2")
                .setProcDt("2021-07-06")
                .build());
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('A');
    claim.setCurrLoc1('1');
    claim.setCurrLoc2("2");
    claim.setLastUpdated(clock.instant());
    PreAdjFissProcCode code = new PreAdjFissProcCode();
    code.setDcn("dcn");
    code.setPriority((short) 0);
    code.setProcCode("code-1");
    code.setProcFlag("fl-1");
    code.setLastUpdated(claim.getLastUpdated());
    code = new PreAdjFissProcCode();
    code.setDcn("dcn");
    code.setPriority((short) 1);
    code.setProcCode("code-2");
    code.setProcFlag("fl-2");
    code.setProcDate(LocalDate.of(2021, 7, 6));
    code.setLastUpdated(claim.getLastUpdated());
    assertThat(transformer.transformClaim(builder.build()), samePropertyValuesAs(claim));
  }

  @Test
  public void requiredFieldsMissing() {
    try {
      transformer.transformClaim(builder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          Arrays.asList(
              new DataTransformer.ErrorMessage("dcn", "invalid length: expected=[1,23] actual=0"),
              new DataTransformer.ErrorMessage("hicNo", "invalid length: expected=[1,12] actual=0"),
              new DataTransformer.ErrorMessage(
                  "currStatus", "invalid length: expected=[1,1] actual=0"),
              new DataTransformer.ErrorMessage(
                  "currLoc1", "invalid length: expected=[1,1] actual=0"),
              new DataTransformer.ErrorMessage(
                  "currLoc2", "invalid length: expected=[1,5] actual=0")),
          ex.getErrors());
    }
  }

  @Test
  public void allBadFields() {
    try {
      builder
          .setDcn("123456789012345678901234")
          .setHicNo("1234567890123")
          .setCurrStatus("12")
          .setCurrLoc1("12")
          .setCurrLoc2("123456")
          .setMedaProvId("12345678901234")
          .setTotalChargeAmount("not-a-number")
          .setRecdDt("not-a-date")
          .setCurrTranDate("not-a-date")
          .setAdmDiagCode("12345678")
          .setPrincipleDiag("12345678")
          .setNpiNumber("12345678901")
          .setMbi("12345678901234")
          .setFedTaxNb("12345678901")
          .addFissProcCodes(
              FissProcCodes.newBuilder()
                  .setProcCd("12345678901")
                  .setProcFlag("12345")
                  .setProcDt("not-a-date")
                  .build());
      transformer.transformClaim(builder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          Arrays.asList(
              new DataTransformer.ErrorMessage("dcn", "invalid length: expected=[1,23] actual=24"),
              new DataTransformer.ErrorMessage(
                  "hicNo", "invalid length: expected=[1,12] actual=13"),
              new DataTransformer.ErrorMessage(
                  "currStatus", "invalid length: expected=[1,1] actual=2"),
              new DataTransformer.ErrorMessage(
                  "currLoc1", "invalid length: expected=[1,1] actual=2"),
              new DataTransformer.ErrorMessage(
                  "currLoc2", "invalid length: expected=[1,5] actual=6"),
              new DataTransformer.ErrorMessage(
                  "medaProvId", "invalid length: expected=[1,13] actual=14"),
              new DataTransformer.ErrorMessage("totalChargeAmount", "invalid amount"),
              new DataTransformer.ErrorMessage("receivedDate", "invalid date"),
              new DataTransformer.ErrorMessage("currTranDate", "invalid date"),
              new DataTransformer.ErrorMessage(
                  "admitDiagCode", "invalid length: expected=[1,7] actual=8"),
              new DataTransformer.ErrorMessage(
                  "principleDiag", "invalid length: expected=[1,7] actual=8"),
              new DataTransformer.ErrorMessage(
                  "npiNumber", "invalid length: expected=[1,10] actual=11"),
              new DataTransformer.ErrorMessage("mbi", "invalid length: expected=[1,13] actual=14"),
              new DataTransformer.ErrorMessage(
                  "fedTaxNumber", "invalid length: expected=[1,10] actual=11"),
              new DataTransformer.ErrorMessage(
                  "procCode-1-procCode", "invalid length: expected=[1,10] actual=11"),
              new DataTransformer.ErrorMessage(
                  "procCode-1-procFlag", "invalid length: expected=[1,4] actual=5"),
              new DataTransformer.ErrorMessage("procCode-1-procDate", "invalid date")),
          ex.getErrors());
    }
  }
}
