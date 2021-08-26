package gov.cms.bfd.model.rda;

import java.time.Instant;
import java.time.LocalDate;
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
public class PreAdjFissProcCode {
  private String procCode;
  private String procFlag;
  private LocalDate procDate;
  private Instant lastUpdated;
}
