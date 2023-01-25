package gov.cms.bfd.pipeline.bridge.io;

import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import java.nio.file.Path;
import lombok.Data;

/** Data class for SinkArguments. */
@Data
public class SinkArguments {
  /** outputPath returns {@link Path}. */
  private final Path outputPath;
  /** sequenceCounter returns {@link WrappedCounter}. */
  private final WrappedCounter sequenceCounter;
}
