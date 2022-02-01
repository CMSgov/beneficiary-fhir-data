package gov.cms.bfd.pipeline.bridge;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/** Helper class for defining application specific configurations. */
@Data
@FieldNameConstants
public class AppConfig {
  private String inputDirPath;
  private String outputDirPath;
  private String fissOutputFile;
  private String mcsOutputFile;
  private String mbiSource;
  private String fissSeqStart;
  private String mcsSeqStart;
  private String buildAttributionSet;
  private String attributionSetSize;
  private String attributionTemplateFile;
  private String attributionScriptFile;
  private Set<String> fissSources = new HashSet<>();
  private Set<String> mcsSources = new HashSet<>();
}
