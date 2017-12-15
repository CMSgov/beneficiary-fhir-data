package gov.hhs.cms.bluebutton.fhirstress.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

public class RifParser 
{
  private CsvToBean<RifEntry> csvToBean = null;
  private Iterator<RifEntry> csvToBeanIt = null;

  public RifParser(String rifFile, String delimiter) {
    try {
      //System.out.println("RifFile = " + rifFile + ", delimiter = " + delimiter);
      csvToBean = new CsvToBeanBuilder(new FileReader(rifFile)).withSeparator(delimiter.charAt(0)).withType(RifEntry.class).build();
      csvToBeanIt = csvToBean.iterator();
    }
	  catch (IOException e) {
      e.printStackTrace();
    }
  } 

  public RifEntry next() {
    return csvToBeanIt.next();
  }

  public boolean hasNext() {
    return csvToBeanIt.hasNext();
  }
}
