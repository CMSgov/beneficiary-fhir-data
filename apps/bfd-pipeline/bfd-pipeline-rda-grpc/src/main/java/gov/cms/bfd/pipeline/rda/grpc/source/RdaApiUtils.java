package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.mpsm.rda.v1.ClaimChange;

public class RdaApiUtils {
  public static RdaChange.Type mapApiChangeType(ClaimChange.ChangeType apiType) {
    switch (apiType) {
      case CHANGE_TYPE_INSERT:
        return RdaChange.Type.INSERT;
      case CHANGE_TYPE_UPDATE:
        return RdaChange.Type.UPDATE;
      case CHANGE_TYPE_DELETE:
        return RdaChange.Type.DELETE;
      default:
        throw new IllegalArgumentException("no mapping for unrecognized type " + apiType.name());
    }
  }
}
