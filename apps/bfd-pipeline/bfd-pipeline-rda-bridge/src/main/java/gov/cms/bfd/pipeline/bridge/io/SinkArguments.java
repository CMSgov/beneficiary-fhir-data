package gov.cms.bfd.pipeline.bridge.io;

import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import java.nio.file.Path;
import lombok.Data;

/** Data class used for Fiss and MCs file path and its associated seequence in that file. */
@Data
public class SinkArguments {
  /** Output Path is the path of the associated Fiss or MCS file. */
  private final Path outputPath;

  /** This field is what keeps track of the processed sequence numbers of the consumed files. */
  private final WrappedCounter sequenceCounter;
}
