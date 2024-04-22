package gov.cms.bfd.sharedutils.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Immutable object containing settings for the {@link LayeredConfiguration} class. */
@Immutable
@Getter
@ToString
@EqualsAndHashCode
public class LayeredConfigurationSettings {
  /** Optional path to a java properties file. Will be empty string if no path was provided. */
  private final String propertiesFile;

  /**
   * List of SSM parameter paths. Each path is a folder containing parameter values to be loaded
   * and/or sub-folders that themselves can contain parameter values to be loaded. Will be empty
   * list if no paths were provided.
   */
  private final List<String> ssmHierarchies;

  /**
   * Initializes an instance with the given values. Jackson might pass nulls for any of these
   * properties so the constructor handles those using appropriate empty values.
   *
   * @param propertiesFile value for {@link #propertiesFile}
   * @param ssmHierarchies value for {@link #ssmHierarchies}
   */
  @Builder
  public LayeredConfigurationSettings(
      @JsonProperty("propertiesFile") @Nullable String propertiesFile,
      @JsonProperty("ssmHierarchies") @Nullable List<String> ssmHierarchies) {
    this.propertiesFile = Strings.nullToEmpty(propertiesFile);
    this.ssmHierarchies = ssmHierarchies == null ? List.of() : List.copyOf(ssmHierarchies);
  }

  /**
   * Determines if a java properties file path has been provided.
   *
   * @return true if a properties file path is available
   */
  public boolean hasPropertiesFile() {
    return !propertiesFile.isEmpty();
  }

  /**
   * Determines if any hierarchies have been provided.
   *
   * @return true if any SSM hierarchy paths have been provided
   */
  public boolean hasSsmHierarchies() {
    return !ssmHierarchies.isEmpty();
  }
}
