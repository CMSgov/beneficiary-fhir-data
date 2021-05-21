package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.mpsm.rda.v1.FissClaim;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FissClaimTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(FissClaimTransformer.class);
  private final Clock clock;

  public FissClaimTransformer(Clock clock) {
    this.clock = clock;
  }

  public PreAdjFissClaim transformClaim(FissClaim from) {
    final DataTransformer transformer = new DataTransformer();
    final PreAdjFissClaim to = new PreAdjFissClaim();
    transformer
        .copyString("dcn", from.getDcn(), false, 1, 23, to::setDcn)
        .copyString("hicNo", from.getHicNo(), false, 1, 12, to::setHicNo)
        .copyCharacter("currStatus", from.getCurrStatus(), to::setCurrStatus)
        .copyCharacter("currLoc1", from.getCurrLoc1(), to::setCurrLoc1)
        .copyString("currLoc2", from.getCurrLoc2(), false, 1, 5, to::setCurrLoc2);
    if (from.hasMedaProvId()) {
      transformer.copyString("medaProvId", from.getMedaProvId(), true, 1, 13, to::setMedaProvId);
    }
    if (from.hasTotalChargeAmount()) {
      transformer.copyAmount(
          "totalChargeAmount", from.getTotalChargeAmount(), true, to::setTotalChargeAmount);
    }
    if (from.hasRecdDt()) {
      transformer.copyDate("receivedDate", from.getRecdDt(), true, to::setReceivedDate);
    }
    if (from.hasCurrTranDate()) {
      transformer.copyDate("currTranDate", from.getCurrTranDate(), true, to::setCurrTranDate);
    }
    if (from.hasAdmDiagCode()) {
      transformer.copyString(
          "adminDiagCode", from.getAdmDiagCode(), true, 1, 7, to::setAdmitDiagCode);
    }
    if (from.hasPrincipleDiag()) {
      transformer.copyString(
          "principleDiag", from.getPrincipleDiag(), true, 1, 7, to::setPrincipleDiag);
    }
    if (from.hasNpiNumber()) {
      transformer.copyString("npiNumber", from.getNpiNumber(), true, 1, 10, to::setNpiNumber);
    }
    if (from.hasMbi()) {
      final String mbi = from.getMbi();
      transformer
          .copyString("mbi", mbi, true, 1, 13, to::setMbi)
          .copyHashedString("mbiHash", mbi, true, 1, 23, to::setMbiHash);
    }
    if (from.hasFedTaxNb()) {
      transformer.copyString("fedTaxNumber", from.getFedTaxNb(), true, 1, 10, to::setFedTaxNumber);
    }
    to.setLastUpdated(clock.instant());
    if (transformer.isSuccessful()) {
      return to;
    }
    LOGGER.error("received invalid FissClaim from RDA: errors={}", transformer.getErrors());
    throw new FissClaimException(
        String.format("received invalid FissClaim from RDA: errors=%s", transformer.getErrors()));
  }

  public static class FissClaimException extends RuntimeException {
    public FissClaimException(String message) {
      super(message);
    }
  }
}
