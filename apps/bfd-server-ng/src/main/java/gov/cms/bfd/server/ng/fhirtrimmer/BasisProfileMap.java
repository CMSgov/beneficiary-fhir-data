package gov.cms.bfd.server.ng.fhirtrimmer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/** Map of basis profiles to FhirPaths. */
@NoArgsConstructor
@Getter
public class BasisProfileMap {

  private final EnumMap<BasisProfile, List<String>> blackList = new EnumMap<>(BasisProfile.class);
  private final EnumMap<BasisProfile, List<String>> whiteList = new EnumMap<>(BasisProfile.class);

  /** A single entry from a dictionary support file YAML, exhaustive property list. */
  @Data
  public static class DictionaryEntry {
    private String inputPath;
    private List<String> appliesTo;
    private List<String> sources;
    private String sourceView;
    private String sourceColumn;
    private String nameOverride;
    private List<String> ccwMapping;
    private List<String> cclfMapping;
    private String bfdDerived;
    private String definitionOverride;
    private String referenceTable;
    private String fhirPath;
    private String suppressInDD;
    private List<String> profiles;
    private String notes;
  }

  /**
   * Updates the BasisProfileMap with a file input stream (dictionary yaml).
   * @param fileStream the YAML file
   */
  private void updateBasisProfileMapWithStream(InputStream fileStream) {

    var loaderOptions = new LoaderOptions();
    var constructor = new Constructor(DictionaryEntry[].class, loaderOptions);

    Yaml yaml = new Yaml(constructor);

    DictionaryEntry[] entries = yaml.load(fileStream);
    if (entries == null) {
      return;
    }

    for (var entry : entries) {
      if (entry.getFhirPath() == null) {
        continue;
      }

      // Default to all profiles if profile is not a property in the YAML
      // TODO: Discuss this with Alex
      Set<BasisProfile> applicableProfiles = EnumSet.allOf(BasisProfile.class);

      if (entry.getProfiles() != null) {
        applicableProfiles = entry.getProfiles().stream()
                .map(p -> BasisProfile.valueOf(p.toUpperCase()))
                .collect(Collectors.toSet());
      }

      for (var profile : BasisProfile.values()) {
        var targetMap = applicableProfiles.contains(profile) ? this.whiteList : this.blackList;
        targetMap.computeIfAbsent(profile, _ -> new ArrayList<>()).add(entry.getFhirPath());
      }
    }
  }

  /**
   * Generates the profiles and loads them into the class instance.
   */
  public void generateProfileBasisMap() {
    try {
      var resolver = new PathMatchingResourcePatternResolver();
      var dictionaryFiles = resolver.getResources("classpath:dictionary-support-files/*.yaml");

      for (var file : dictionaryFiles) {
        try (var inputStream = file.getInputStream()) {
          updateBasisProfileMapWithStream(inputStream);
        }
      }

    } catch (IOException e) {
      System.err.println("OH NO: " + e.getMessage());
    }
  }
}
