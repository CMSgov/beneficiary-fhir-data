package gov.cms.bfd.server.ng;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.SnapshotSerializerContext;
import au.com.origin.snapshots.serializers.v1.ToStringSnapshotSerializer;
import ca.uhn.fhir.context.FhirContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class JsonSnapshotSerializer extends ToStringSnapshotSerializer {

  @Override
  public String getOutputFormat() {
    return "fhir+json";
  }

  @Override
  public Snapshot apply(Object object, SnapshotSerializerContext snapshotSerializerContext) {
    Properties prop = new Properties();
    try {
      prop.load(this.getClass().getClassLoader().getResourceAsStream("snapshot.properties"));
      var snapshotDir = prop.getProperty("snapshot-dir");

      var patch =
          new File(
              Paths.get(
                      "src/test/java",
                      getClass().getPackageName().replace(".", "/"),
                      snapshotDir,
                      snapshotSerializerContext.getName() + ".patch")
                  .toString());
      if (patch.exists()) {
        //noinspection ResultOfMethodCallIgnored
        patch.delete();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    FhirContext ctx = FhirContext.forR4();
    var json =
        ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString((IBaseResource) object);
    return super.apply(json, snapshotSerializerContext);
  }
}
