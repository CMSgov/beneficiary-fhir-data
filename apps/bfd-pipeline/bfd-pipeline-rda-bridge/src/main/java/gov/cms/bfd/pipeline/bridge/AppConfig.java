package gov.cms.bfd.pipeline.bridge;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/** Helper class for defining application specific configurations. */
@Data
@FieldNameConstants
public class AppConfig {
  /** Input Directory Path. */
  private String inputDirPath;

  /** Output Directory Path. */
  private String outputDirPath;

  /** Fiss Output File. */
  private String fissOutputFile;

  /** Mcs Output File. */
  private String mcsOutputFile;

  /** MBI Source. */
  private String mbiSource;

  /** Fiss Sequence Start. */
  private String fissSeqStart;

  /** Mcs Sequence Start. */
  private String mcsSeqStart;

  /** Build Attribution Set. */
  private String buildAttributionSet;

  /** Attribution Set Size. */
  private String attributionSetSize;

  /** Attribution Template File. */
  private String attributionTemplateFile;

  /** Attribution Script File. */
  private String attributionScriptFile;

  /** Attribution Fiss Ratio. */
  private String attributionFissRatio;

  /** Fiss Sources. */
  private Set<String> fissSources = new HashSet<>();

  /** Mcs Sources. */
  private Set<String> mcsSources = new HashSet<>();
}
