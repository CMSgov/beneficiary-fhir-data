package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/** test. */
@Getter
public class FinalManifestList {
  /** test. */
  private Set<String> manifests;

  /** test. */
  private String timestamp;

  /**
   * test.
   *
   * @param downloadedFile file
   * @param key test
   */
  public FinalManifestList(byte[] downloadedFile, String key) {
    String fileString = new String(downloadedFile, StandardCharsets.UTF_8);
    String prefix = key.substring(key.lastIndexOf('/'));
    String[] components = prefix.split("/");
    timestamp = components[components.length - 1];
    manifests =
        Arrays.stream(fileString.split("\n"))
            .filter(l -> !l.isBlank())
            .map(l -> prefix + l)
            .collect(Collectors.toSet());
  }
}