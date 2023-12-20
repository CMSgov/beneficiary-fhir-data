package gov.cms.bfd.pipeline.ccw.rif.extract;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;

public class ParquetFileParser implements AutoCloseable {
  private final List<Type> fieldTypes;
  private final List<String> fieldNames;
  private final ParquetFileReader fileReader;
  private long pagesRemaining;
  private PageReadStore pages;
  private long pageRowsRemaining;
  private RecordReader recordReader;
  private long recordNumber;
  private RifParquetObject currentRecord;

  public ParquetFileParser(File parquetFile) throws IOException {
    fileReader = ParquetFileReader.open(new MyInputFile(parquetFile));
    MessageType schema = fileReader.getFooter().getFileMetaData().getSchema();
    fieldTypes = schema.getFields();
    fieldNames = fieldTypes.stream().map(Type::getName).toList();
    pagesRemaining = fileReader.getRowGroups().size();
  }

  public boolean readNext() throws IOException {
    while (pageRowsRemaining == 0) {
      if (pagesRemaining == 0) {
        currentRecord = null;
        return false;
      }
      pagesRemaining -= 1;
      pages = fileReader.readNextRowGroup();
      pageRowsRemaining = pages.getRowCount();
    }
    pageRowsRemaining -= 1;
    recordNumber += 1;
    SimpleGroup simpleGroup = (SimpleGroup) recordReader.read();
    currentRecord = new RifParquetObject(recordNumber, simpleGroup, fieldNames);
    return true;
  }

  @Nullable
  public RifParquetObject getCurrentObject() {
    return currentRecord;
  }

  @Override
  public void close() throws Exception {
    fileReader.close();
  }
}
