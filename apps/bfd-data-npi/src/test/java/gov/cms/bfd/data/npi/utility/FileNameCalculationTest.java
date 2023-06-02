package gov.cms.bfd.data.npi.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** File Name Calculation Test. */
public class FileNameCalculationTest {
  /** The calendar to use. */
  public Calendar calendar;

  /** The current month to use. */
  public int currentMonth;

  /** The current year to use. */
  public int currentYear;

  /** The expected file name to use. */
  public String expectedFileName;

  /** The File Name calcultion class to use. */
  public FileNameCalculation fileNameCalculation;

  /** Map of all the months. */
  private Map<Integer, String> months =
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

  /** Setup method. */
  @BeforeEach
  public void setup() {
    calendar = Calendar.getInstance();
    currentMonth = calendar.get(Calendar.MONTH);
    currentYear = calendar.get(Calendar.YEAR);
    fileNameCalculation = new FileNameCalculation();
    expectedFileName = "";
  }

  /** Happy path for file Name calculation. */
  @Test
  public void returnTheCorrectFileNameForCurrentMonth() {
    String month = months.get(currentMonth);
    expectedFileName = FileNameCalculation.BASE_URL + month + "_" + currentYear + ".zip";
    String fileName = fileNameCalculation.getFileName(false);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the month before, if its January, it will return December and the year before. */
  @Test
  public void returnsTheCorrectMonthBefore() {
    String month = "";
    if (currentMonth == 0) {
      month = months.get(11);
      currentYear = currentYear - 1;
    } else {
      month = months.get(currentMonth - 1);
    }
    expectedFileName = FileNameCalculation.BASE_URL + month + "_" + currentYear + ".zip";
    String fileName = fileNameCalculation.getFileName(true);

    assertEquals(expectedFileName, fileName);
  }
}
