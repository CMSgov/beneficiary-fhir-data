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

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
  private static final Pattern LOCALHOST_REGEX = Pattern.compile("http://localhost:\\d+");
  private static final Pattern ID_UUID_REGEX =
      Pattern.compile(String.format("\"id\"\\s*:\\s*\"%s\"", UUID_REGEX));
  private static final Pattern FULLURL_UUID_REGEX =
      Pattern.compile(
          String.format("\"fullUrl\"\\s*:\\s*\"urn:uuid:%s\"", UUID_REGEX),
          Pattern.MULTILINE | Pattern.DOTALL);
  private static final Pattern LAST_UPDATED_REGEX = Pattern.compile("\"lastUpdated\":\\s*\".*\"");
  private static final Pattern REFERENCE_UUID_REGEX =
      Pattern.compile(
          String.format("\"reference\"\\s*:\\s*\"([A-Za-z]+)/%s\"", UUID_REGEX),
          Pattern.MULTILINE | Pattern.DOTALL);
  private static final Pattern CLAIM_IDR_LOAD_DATE_REGEX =
      Pattern.compile(
          "(\"code\"\\s*:\\s*\"CLM_IDR_LD_DT\".*?\"timingDate\"\\s*:\\s*\")\\d{4}-\\d{2}-\\d{2}(\")?",
          Pattern.MULTILINE | Pattern.DOTALL);
  private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";

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
    json = ID_UUID_REGEX.matcher(json).replaceAll(String.format("\"id\" : \"%s\"", DEFAULT_UUID));
    json =
        FULLURL_UUID_REGEX
            .matcher(json)
            .replaceAll(String.format("\"fullUrl\" : \"urn:uuid:%s\"", DEFAULT_UUID));
    json =
        REFERENCE_UUID_REGEX
            .matcher(json)
            .replaceAll(String.format("\"reference\": \"$1/%s\"", DEFAULT_UUID));
    // lastUpdated gets reset whenever we recreate the test data
    json =
        LAST_UPDATED_REGEX
            .matcher(json)
            .replaceAll("\"lastUpdated\": \"9999-12-31T00:00:00.000+00:00\"");

    json = CLAIM_IDR_LOAD_DATE_REGEX.matcher(json).replaceAll("$19999-12-31$2");

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
