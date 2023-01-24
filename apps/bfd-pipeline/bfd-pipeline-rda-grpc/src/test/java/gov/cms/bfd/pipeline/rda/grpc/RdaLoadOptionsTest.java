package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Tests related to the {@link RdaLoadOptions} class. */
public class RdaLoadOptionsTest {

  /** Tests that the {@link RdaLoadOptions} can be serialized without issue */
  @Test
  public void configIsSerializable() throws Exception {
    final RdaLoadOptions original =
        new RdaLoadOptions(
            AbstractRdaLoadJob.Config.builder()
                .runInterval(Duration.ofDays(12))
                .batchSize(9832)
                .build(),
            RdaSourceConfig.builder()
                .serverType(RdaSourceConfig.ServerType.Remote)
                .host("localhost")
                .port(5432)
                .maxIdle(Duration.ofMinutes(59))
                .authenticationToken("my-secret")
                .build(),
            new RdaServerJob.Config(),
            0,
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
    assertEquals(original, loaded);
  }
}
