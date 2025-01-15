package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/** Represents a manifest list file (ManifestList.done). */
@Getter
public class FinalManifestList {
  /** Contained list of manifests. */
  private final Set<String> manifests;

  /** Timestamp text extracted from the S3 prefix. */
  private final String timestampText;

  /** Timestamp from the S3 prefix. */
  private final Instant timestamp;

  /**
   * Creates a new instance from a downloaded file.
   *
   * @param downloadedFileContent file contents
   * @param key S3 key
   */
  public FinalManifestList(byte[] downloadedFileContent, String key) {
    String fileString = new String(downloadedFileContent, StandardCharsets.UTF_8);
    // Get the prefix without the filename
    String prefix = key.substring(0, key.lastIndexOf('/'));

    String[] components = prefix.split("/");
    this.timestampText = components[components.length - 1];
    this.timestamp = Instant.parse(this.timestampText);
    this.manifests =
        Arrays.stream(fileString.split("\n"))
            .filter(l -> !l.isBlank())
            .map(l -> prefix + "/" + l)
            .collect(Collectors.toSet());
  }
}
