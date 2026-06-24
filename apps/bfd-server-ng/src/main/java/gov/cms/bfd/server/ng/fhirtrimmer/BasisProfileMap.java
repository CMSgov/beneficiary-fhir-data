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
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/** Map of resource type -&gt; basis profile -&gt; FhirPaths, both blacklisted and whitelisted. */
@NoArgsConstructor
@Getter
public class BasisProfileMap {

  private final EnumMap<ResourceType, EnumMap<BasisProfile, List<String>>> blackList =
          new EnumMap<>(ResourceType.class);
  private final EnumMap<ResourceType, EnumMap<BasisProfile, List<String>>> whiteList =
          new EnumMap<>(ResourceType.class);

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
   *
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
      String fhirPath = entry.getFhirPath();
      if (fhirPath == null || fhirPath.isBlank()) {
        continue;
      }

      ResourceType resourceType = resolveResourceType(fhirPath);
      if (resourceType == null) {
        System.err.println("OH NO: could not resolve resource type from FhirPath: " + fhirPath);
        continue;
      }

      // profiles = where this field APPLIES (should be kept, i.e. whitelisted).
      // Every other profile blacklists it.
      Set<BasisProfile> applicableProfiles = EnumSet.allOf(BasisProfile.class);
      if (entry.getProfiles() != null) {
        applicableProfiles =
                entry.getProfiles().stream()
                        .map(p -> BasisProfile.valueOf(p.toUpperCase()))
                        .collect(Collectors.toSet());
      }

      var blackProfileMap =
              blackList.computeIfAbsent(resourceType, _ -> new EnumMap<>(BasisProfile.class));
      var whiteProfileMap =
              whiteList.computeIfAbsent(resourceType, _ -> new EnumMap<>(BasisProfile.class));

      for (var profile : BasisProfile.values()) {
        var targetMap = applicableProfiles.contains(profile) ? whiteProfileMap : blackProfileMap;
        targetMap.computeIfAbsent(profile, _ -> new ArrayList<>()).add(fhirPath);
      }
    }
  }

  /**
   * Get ResourceType from FhirPath.
   *
   * @param fhirPath the FhirPath expression
   * @return the resolved resource type, or null if the leading segment isn't a known type
   */
  private ResourceType resolveResourceType(String fhirPath) {
    for (ResourceType type : ResourceType.values()) {
      var name = type.toString();
      int idx = fhirPath.indexOf(name);
      if (idx >= 0
              && (idx == 0 || !Character.isLetterOrDigit(fhirPath.charAt(idx - 1)))
              && idx + name.length() < fhirPath.length()
              && fhirPath.charAt(idx + name.length()) == '.') {
        return type;
      }
    }
    return null;
  }

  /** Generates the profiles and loads them into the class instance. */
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