package gov.cms.bfd.pipeline.bridge.etl;

import com.google.protobuf.MessageOrBuilder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class AbstractTransformer {

  private static final SimpleDateFormat rifDateFormat = new SimpleDateFormat("dd-MMM-yyy");
  private static final SimpleDateFormat rdaDateFormat = new SimpleDateFormat("yyyy-MM-dd");

  public abstract MessageOrBuilder transform(Parser.Data<String> data);

  protected String convertRifDate(String rifDate) {
    if (!rifDate.isEmpty()) {
      try {
        Date targetDate = rifDateFormat.parse(rifDate);
        return rdaDateFormat.format(targetDate);
      } catch (ParseException e) {
        throw new IllegalArgumentException("Invalid date format", e);
      }
    }

    return "";
  }
}
