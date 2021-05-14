package gov.cms.bfd.model.rda;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/* PK class for the PreAdjFissProcCodes table */
public class PreAdjFissProcCodePK implements Serializable {

  protected String dcn;
  protected short priority;
}
