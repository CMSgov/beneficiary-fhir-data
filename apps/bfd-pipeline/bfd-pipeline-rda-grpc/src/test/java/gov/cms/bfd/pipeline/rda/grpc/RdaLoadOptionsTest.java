package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class RdaLoadOptionsTest {
  @Test
  public void configIsSerializable() throws Exception {
    final RdaLoadOptions original =
        new RdaLoadOptions(
            AbstractRdaLoadJob.Config.builder()
                .runInterval(Duration.ofDays(12))
                .batchSize(9832)
                .build(),
            GrpcRdaSource.Config.builder()
                .serverType(GrpcRdaSource.Config.ServerType.Remote)
                .host("localhost")
                .port(5432)
                .maxIdle(Duration.ofMinutes(59))
                .authenticationToken("my-secret")
                .build(),
            new RdaServerJob.Config(),
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
