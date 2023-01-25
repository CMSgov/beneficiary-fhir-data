package gov.cms.bfd.pipeline.bridge;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/** Helper class for defining application specific configurations. */
@Data
@FieldNameConstants
public class AppConfig {
  /** Input Directory Path returns {@link String}. */
  private String inputDirPath;
  /** Output Directory Path returns {@link String}. */
  private String outputDirPath;
  /** Fiss Output File returns {@link String}. */
  private String fissOutputFile;
  /** Mcs Output File returns {@link String}. */
  private String mcsOutputFile;
  /** MBI Source returns {@link String}. */
  private String mbiSource;
  /** Fiss Sequence Start returns {@link String}. */
  private String fissSeqStart;
  /** Mcs Sequence Start returns {@link String}. */
  private String mcsSeqStart;
  /** Build Attribution Set returns {@link String}. */
  private String buildAttributionSet;
  /** Attribution Set Size returns {@link String}. */
  private String attributionSetSize;
  /** Attribution Template File returns {@link String}. */
  private String attributionTemplateFile;
  /** Attribution Script File returns {@link String}. */
  private String attributionScriptFile;
  /** Attribution Fiss Ratio returns {@link String}. */
  private String attributionFissRatio;
  /** Fiss Sources returns {@link Set}. */
  private Set<String> fissSources = new HashSet<>();
  /** Mcs Sources returns {@link Set}. */
  private Set<String> mcsSources = new HashSet<>();
}
