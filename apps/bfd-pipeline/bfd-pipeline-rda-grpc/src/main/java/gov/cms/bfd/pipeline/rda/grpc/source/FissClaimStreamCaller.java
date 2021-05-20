package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.mpsm.rda.v1.EmptyRequest;
import gov.cms.mpsm.rda.v1.FissClaim;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;
import java.sql.Date;
import java.util.Iterator;

/**
 * GrpcStreamCaller implementation that calls the RDA FissClaim service. At this stage in RDA API
 * development there is no way to resume a stream from a given point in time so every time the
 * service is called it sends all of its values.
 */
public class FissClaimStreamCaller implements GrpcStreamCaller<FissClaim> {
  @Override
  public GrpcResponseStream<FissClaim> callService(ManagedChannel channel) throws Exception {
    Preconditions.checkNotNull(channel);
    final EmptyRequest request = EmptyRequest.newBuilder().build();
    ClientCall<EmptyRequest, FissClaim> call =
        channel.newCall(RDAServiceGrpc.getGetFissClaimsMethod(), CallOptions.DEFAULT);
    final Iterator<FissClaim> results = ClientCalls.blockingServerStreamingCall(call, request);
    return new GrpcResponseStream<>(call, results);
  }

  private PreAdjFissClaim transformClaimObject(FissClaim apiClaim) {
    PreAdjFissClaim dbClaim = new PreAdjFissClaim();
    dbClaim.setDcn(apiClaim.getDcn());
    dbClaim.setHicNo(apiClaim.getHicNo());
    dbClaim.setCurrStatus(parseCharString(apiClaim.getCurrStatus()));
    dbClaim.setCurrLoc1(parseCharString(apiClaim.getCurrLoc1()));
    dbClaim.setCurrLoc2(apiClaim.getCurrLoc2());
    dbClaim.setMedaProvId(apiClaim.getMedaProvId());
    dbClaim.setAdmitDiagCode(apiClaim.getAdmDiagCode());
    dbClaim.setCurrTranDate(parseDateString(apiClaim.getCurrTranDate()));
    return dbClaim;
  }

  private Date parseDateString(String date) {
    return Date.valueOf(date);
  }

  private char parseCharString(String value) {
    return value.charAt(0);
  }
}
