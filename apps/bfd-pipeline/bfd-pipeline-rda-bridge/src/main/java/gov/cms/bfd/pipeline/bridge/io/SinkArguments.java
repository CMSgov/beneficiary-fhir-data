package gov.cms.bfd.pipeline.bridge.io;

import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import java.nio.file.Path;
import lombok.Data;

@Data
public class SinkArguments {
  private final Path outputPath;
  private final WrappedCounter sequenceCounter;
}
