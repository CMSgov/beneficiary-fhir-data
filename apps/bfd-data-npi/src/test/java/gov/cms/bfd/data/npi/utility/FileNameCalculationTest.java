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
  public int month;

  /** The current year to use. */
  public int year;

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
    fileNameCalculation = new FileNameCalculation();
    expectedFileName = "";
    month = 0;
    year = 2014;
  }

  /** Returns the correct file name for January. */
  @Test
  public void returnTheCorrectFileNameForJanuary() {
    month = 0;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for February. */
  @Test
  public void returnTheCorrectFileNameForFebruary() {
    month = 1;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for March. */
  @Test
  public void returnTheCorrectFileNameForMarch() {
    month = 2;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for April. */
  @Test
  public void returnTheCorrectFileNameForApril() {
    month = 3;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for May. */
  @Test
  public void returnTheCorrectFileNameForMay() {
    month = 4;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for June. */
  @Test
  public void returnTheCorrectFileNameForJune() {
    month = 5;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for July. */
  @Test
  public void returnTheCorrectFileNameForJuly() {
    month = 6;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for August. */
  @Test
  public void returnTheCorrectFileNameForAugust() {
    month = 7;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for September. */
  @Test
  public void returnTheCorrectFileNameForSeptember() {
    month = 8;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for October. */
  @Test
  public void returnTheCorrectFileNameForOctober() {
    month = 9;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for November. */
  @Test
  public void returnTheCorrectFileNameForNovember() {
    month = 10;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /** Returns the correct file name for December. */
  @Test
  public void returnTheCorrectFileNameForDecember() {
    month = 11;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(month) + "_" + year + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(false, month, year);

    assertEquals(expectedFileName, fileName);
  }

  /**
   * Returns the correct file name for previous year and month when get previous month set to true.
   */
  @Test
  public void returnTheCorrectFileNameForThePreviousMonthAndYearWhenGetPreviousMonthSetToTrue() {
    month = 0;
    expectedFileName = FileNameCalculation.BASE_URL + months.get(11) + "_" + (year - 1) + ".zip";
    String fileName = fileNameCalculation.getMonthAndYearForFile(true, month, year);

    assertEquals(expectedFileName, fileName);
  }
}
