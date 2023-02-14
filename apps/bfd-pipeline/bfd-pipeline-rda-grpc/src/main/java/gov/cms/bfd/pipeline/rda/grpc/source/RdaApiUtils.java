package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.mpsm.rda.v1.ChangeType;

/** Utility class for the RDA API. */
public class RdaApiUtils {
  /**
   * Maps a {@link ChangeType} to a {@link RdaChange.Type}.
   *
   * @param apiType the api type to map
   * @return the mapped rda change type
   */
  public static RdaChange.Type mapApiChangeType(ChangeType apiType) {
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
