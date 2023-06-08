package gov.cms.bfd.data.npi.utility;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/** Extracts a file name. */
public class FileNameCalculation {
  /** Base url for the nppes download. */
  protected static final String BASE_URL =
      "https://download.cms.gov/nppes/NPPES_Data_Dissemination_";

  /** Map of months. */
  private static final Map<Integer, String> months =
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

  /**
   * Extracts a file name.
   *
   * @param getMonthBefore gets the month before
   * @return a file name string
   */
  public static String getFileName(boolean getMonthBefore) {
    Calendar cal = Calendar.getInstance();
    int currentMonth = cal.get(Calendar.MONTH);
    int currentYear = cal.get(Calendar.YEAR);

    return getMonthAndYearForFile(getMonthBefore, currentMonth, currentYear);
  }

  /**
   * Formats the file name with Month and Year.
   *
   * @param getMonthBefore whether to get the previous month or not
   * @param currentMonth is the integer for month
   * @param currentYear is the integer for year
   * @return a file name string
   */
  public static String getMonthAndYearForFile(
      boolean getMonthBefore, int currentMonth, int currentYear) {

    String month = null;
    int year = 0;
    String fileName = null;

    if (getMonthBefore) {
      if (currentMonth == 0) {
        month = months.get(11);
        year = currentYear - 1;
      } else {
        month = months.get(currentMonth - 1);
        year = currentYear;
      }

    } else {
      month = months.get(currentMonth);
      year = currentYear;
    }

    return BASE_URL + month + "_" + String.valueOf(year) + ".zip";
  }
}
