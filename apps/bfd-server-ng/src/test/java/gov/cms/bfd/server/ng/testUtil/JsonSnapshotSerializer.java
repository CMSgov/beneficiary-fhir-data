package gov.cms.bfd.server.ng.testUtil;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.SnapshotSerializerContext;
import au.com.origin.snapshots.serializers.v1.ToStringSnapshotSerializer;
import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class JsonSnapshotSerializer extends ToStringSnapshotSerializer {

  private static final Pattern LOCALHOST_REGEX = Pattern.compile("http://localhost:\\d+");
  private static final Pattern UUID_REGEX =
      Pattern.compile(
          "\"id\"\\s*:\\s*\"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\"");

  @Override
  public String getOutputFormat() {
    return "fhir+json";
  }

  @SneakyThrows
  @Override
  public Snapshot apply(Object object, SnapshotSerializerContext snapshotSerializerContext) {

    // Clean up the patch file if it exists. If the test fails again, it will generate a new one.
    var patchFile =
        SnapshotHelper.getPatchfile(this.getClass(), snapshotSerializerContext.getName());

    if (patchFile.exists()) {
      // if the deletion failed for some reason, we can just ignore it
      //noinspection ResultOfMethodCallIgnored
      patchFile.delete();
    }

    FhirContext ctx = FhirContext.forR4();
    // First, we use the default FHIR JSON serializer. This is important to ensure we're starting
    // with the specific output that our endpoints use.
    // You can also set specific properties to be ignored or replaced here.
    var resource = (IBaseResource) object;
    var json = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);

    // Replace nondeterministic values
    // Ports are randomized
    json = LOCALHOST_REGEX.matcher(json).replaceAll("http://localhost");
    // Generated UUIDs are random
    json = UUID_REGEX.matcher(json).replaceAll("\"id\" : \"{uuid}\"");

    // Take the HAPI FHIR output and serialize it using a serialization format that will sort keys
    // in order.
    // This will prevent re-ordered keys from showing up in the diff, since that isn't important.
    var keyOrderingObjectMapper =
        JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();
    var orderedJsonNode =
        keyOrderingObjectMapper
            .reader()
            .with(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .readTree(json);
    var orderedJsonString =
        keyOrderingObjectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(orderedJsonNode);
    return super.apply(orderedJsonString, snapshotSerializerContext);
  }
}
