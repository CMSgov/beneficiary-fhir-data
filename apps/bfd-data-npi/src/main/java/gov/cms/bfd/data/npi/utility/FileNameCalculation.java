package gov.cms.bfd.data.npi.utility;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/** Extracts a file name. */
public class FileNameCalculation {
  /** Base url for the nppes download. */
  protected static final String BASE_URL =
      "https://download.cms.gov/nppes/NPPES_Data_Dissemination_";

  /**
   * Extracts a file name.
   *
   * @param getMonthBefore gets the month before
   * @return a file name string
   */
  public static String getFileName(boolean getMonthBefore) {
    Map<Integer, String> months =
        new HashMap<Integer, String>() {
          {
            put(0, "January");
            put(1, "February");
            put(2, "March");
            put(3, "April");
            put(4, "May");
            put(5, "June");
            put(6, "July");
            put(7, "August");
            put(8, "September");
            put(9, "October");
            put(10, "November");
            put(11, "December");
          }
        };

    Calendar cal = Calendar.getInstance();
    int month = cal.get(Calendar.MONTH);
    int currentYear = cal.get(Calendar.YEAR);
    String currentMonth = null;

    String fileName = null;

    if (getMonthBefore) {
      if (month == 0) {
        currentMonth = months.get(11);
        currentYear = currentYear - 1;
      } else {
        currentMonth = months.get(month - 1);
      }

    } else {
      currentMonth = months.get(month);
    }

    fileName = BASE_URL + currentMonth + "_" + currentYear + ".zip";

    return fileName;
  }
}
