package gov.cms.bfd.server.ng.fhirtrimmer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Map of basis profiles to FhirPaths. */
@NoArgsConstructor
@Getter
public class BasisProfileMap {

  private static Map<BasisProfile, List<String>> readBasisProfileMap(InputStream inputStream) {
    try {
      EnumMap<BasisProfile, List<String>> basisProfileMap = new EnumMap<>(BasisProfile.class);
      var yamlData = new YAMLMapper().readValue(inputStream, new TypeReference<List<String>>() {});

      for (var entry : yamlData) {
        var x = entry;
      }

      return basisProfileMap;

    } catch (Exception e) {
      throw new RuntimeException("Failed to read YAML files", e);
    }
  }

  /**
   * Retrieve the basis profile, just eob right now.
   *
   * @return the profile!
   */
  public static Map<BasisProfile, List<String>> getBasisProfileMap() {
    var inputStream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("ExplanationOfBenefit.yaml");

    var contextClass = Thread.currentThread().getContextClassLoader();

    if (inputStream == null) {
      throw new IllegalArgumentException("File not found in classpath: security_labels.yml");
    }

    return readBasisProfileMap(inputStream);
  }
}
