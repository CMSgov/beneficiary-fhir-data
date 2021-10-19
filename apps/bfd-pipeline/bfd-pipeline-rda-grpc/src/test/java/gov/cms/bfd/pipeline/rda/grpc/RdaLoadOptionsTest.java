package gov.cms.bfd.pipeline.rda.grpc;

import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class RdaLoadOptionsTest {
  @Test
  public void configIsSerializable() throws Exception {
    final RdaLoadOptions original =
        new RdaLoadOptions(
            new AbstractRdaLoadJob.Config(
                Duration.ofDays(12), 9832, Optional.empty(), Optional.empty()),
            new GrpcRdaSource.Config(
                GrpcRdaSource.Config.ServerType.Remote,
                "localhost",
                5432,
                "mock-server",
                Duration.ofMinutes(59)),
            new IdHasher.Config(1000, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    RdaLoadOptions loaded;
    try (ObjectInputStream inp =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      loaded = (RdaLoadOptions) inp.readObject();
    }
    Assert.assertEquals(original, loaded);
  }
}
