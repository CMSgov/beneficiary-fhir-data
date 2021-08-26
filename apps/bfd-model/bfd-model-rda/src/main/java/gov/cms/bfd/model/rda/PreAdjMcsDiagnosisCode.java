package gov.cms.bfd.model.rda;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class PreAdjMcsDiagnosisCode {
  private String idrDiagIcdType;
  private String idrDiagCode;
  private Instant lastUpdated;
}
